package com.flow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.common.ExecutableSignatureTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** S3는 가짜(Mock)로 대신한다. 정책 변경은 테스트마다 롤백된다. */
@SpringBootTest(properties = "aws.s3.bucket=test-bucket")
@AutoConfigureMockMvc
@Transactional
class FileUploadApiTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	S3Client s3;

	@Test
	void 정상_파일은_UUID_키로_저장된다() throws Exception {
		upload("../../보고서.txt", text("hello"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.originalName").value("보고서.txt"))
			.andExpect(jsonPath("$.size").value(5));

		ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3).putObject(captor.capture(), any(RequestBody.class));
		PutObjectRequest request = captor.getValue();
		assertThat(request.bucket()).isEqualTo("test-bucket");
		assertThat(request.key()).matches("uploads/[0-9a-f-]{36}"); // 원본 파일명·확장자를 키에 쓰지 않음
		assertThat(request.contentType()).isEqualTo("application/octet-stream");
	}

	@Test
	void 고정_확장자는_체크했을_때만_차단된다() throws Exception {
		upload("setup.exe", ExecutableSignatureTest.pe()).andExpect(status().isCreated());

		checkFixed("exe", true);
		upload("setup.exe", ExecutableSignatureTest.pe())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BLOCKED_EXTENSION"));
	}

	@Test
	void 이중_확장자와_대소문자도_차단된다() throws Exception {
		checkFixed("exe", true);
		expectBlocked(upload("invoice.exe.txt", text("x")), "BLOCKED_EXTENSION");
		expectBlocked(upload("SETUP.EXE", text("x")), "BLOCKED_EXTENSION");
		expectBlocked(upload("setup.exe.", text("x")), "BLOCKED_EXTENSION");
	}

	@Test
	void 커스텀_확장자가_차단된다() throws Exception {
		mvc.perform(post("/api/extensions/custom").contentType(MediaType.APPLICATION_JSON).content("{\"extension\":\"sh\"}"))
			.andExpect(status().isCreated());
		expectBlocked(upload("deploy.sh", text("echo")), "BLOCKED_EXTENSION");
	}

	@Test
	void 실행_파일을_다른_확장자로_위장하면_차단된다() throws Exception {
		expectBlocked(upload("report.jpg", ExecutableSignatureTest.pe()), "DISGUISED_EXECUTABLE");
		expectBlocked(upload("run", ExecutableSignatureTest.elf()), "DISGUISED_EXECUTABLE");
		expectBlocked(upload("notes.txt", text("#!/bin/sh\nrm -rf /")), "DISGUISED_EXECUTABLE");
	}

	@Test
	void 잘못된_파일은_거부된다() throws Exception {
		expectBlocked(upload("empty.txt", new byte[0]), "EMPTY_FILE");
		expectBlocked(upload("", text("x")), "INVALID_FILE_NAME");
		expectBlocked(upload("a\u0000.exe.jpg", text("x")), "INVALID_FILE_NAME");
		expectBlocked(upload("a".repeat(256) + ".txt", text("x")), "FILE_NAME_TOO_LONG");
	}

	@Test
	void 파일_파트가_없으면_400() throws Exception {
		mvc.perform(multipart("/api/files")).andExpect(status().isBadRequest());
		verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
	}

	@Test
	void 저장소_오류는_503과_안내() throws Exception {
		when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
			.thenThrow(SdkClientException.create("connection refused"));
		upload("ok.txt", text("x"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
	}

	private ResultActions upload(String name, byte[] content) throws Exception {
		return mvc.perform(multipart("/api/files").file(new MockMultipartFile("file", name, "image/jpeg", content)));
	}

	private void expectBlocked(ResultActions result, String code) throws Exception {
		result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(code));
		verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
	}

	private void checkFixed(String extension, boolean blocked) throws Exception {
		mvc.perform(patch("/api/extensions/fixed/" + extension).contentType(MediaType.APPLICATION_JSON)
			.content("{\"blocked\":" + blocked + "}")).andExpect(status().isOk());
	}

	private static byte[] text(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}
}
