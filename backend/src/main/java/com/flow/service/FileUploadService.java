package com.flow.service;

import com.flow.common.ApiException;
import com.flow.common.ExecutableSignature;
import com.flow.common.UploadFileRule;
import com.flow.dao.ExtensionPolicyDao;
import com.flow.dto.FileUploadResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class FileUploadService {

	private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

	private static final String KEY_PREFIX = "uploads/";

	private final ExtensionPolicyDao dao;
	private final S3Client s3;
	private final String bucket;

	public FileUploadService(ExtensionPolicyDao dao, S3Client s3, @Value("${aws.s3.bucket}") String bucket) {
		this.dao = dao;
		this.s3 = s3;
		this.bucket = bucket;
	}

	public FileUploadResponse upload(MultipartFile file) {
		String name = validateName(file.getOriginalFilename());
		if (file.isEmpty()) {
			throw reject("EMPTY_FILE", "빈 파일은 업로드할 수 없습니다.", name);
		}

		// 1. 확장자 정책: 파일명의 모든 점 구간 검사 ("a.exe.txt"의 exe도 차단)
		List<String> extensions = UploadFileRule.extensions(name);
		Set<String> blocked = new HashSet<>(dao.findBlockedExtensions());
		for (String extension : extensions) {
			if (blocked.contains(extension)) {
				throw reject("BLOCKED_EXTENSION", "'" + extension + "' 확장자는 업로드가 차단되어 있습니다.", name);
			}
		}

		// 2. 내용 검사: 실행 파일인데 확장자가 그걸 숨기면 차단 (report.jpg인데 실제로는 exe)
		String lastExtension = extensions.isEmpty() ? null : extensions.get(extensions.size() - 1);
		ExecutableSignature signature = ExecutableSignature.detect(readHead(file));
		if (signature != null && !signature.matchesExtension(lastExtension)) {
			String disguise = lastExtension == null ? "확장자가 없어" : "확장자(" + lastExtension + ")가";
			throw reject("DISGUISED_EXECUTABLE",
				"파일 내용은 " + signature.label() + "인데 " + disguise + " 이를 숨기고 있어 업로드할 수 없습니다.", name);
		}

		// 3. 저장: 원본 파일명을 키에 쓰지 않는다 (경로 조작·덮어쓰기 방지), 형식은 클라이언트 값을 믿지 않는다
		String id = UUID.randomUUID().toString();
		store(KEY_PREFIX + id, file, name);
		log.info("업로드 성공 id={} size={} name={}", id, file.getSize(), name);
		return new FileUploadResponse(id, name, file.getSize());
	}

	private String validateName(String originalFilename) {
		if (originalFilename == null) {
			throw reject("INVALID_FILE_NAME", "파일명이 없습니다.", null);
		}
		String name = UploadFileRule.baseName(originalFilename).strip();
		if (name.isEmpty() || UploadFileRule.hasForbiddenChar(name)) {
			// 제어 문자가 로그에 그대로 찍히지 않도록 파일명은 남기지 않는다
			throw reject("INVALID_FILE_NAME", "파일명이 없거나 사용할 수 없는 문자가 포함되어 있습니다.", null);
		}
		if (name.length() > UploadFileRule.MAX_FILE_NAME_LENGTH) {
			throw reject("FILE_NAME_TOO_LONG",
				"파일명은 최대 " + UploadFileRule.MAX_FILE_NAME_LENGTH + "자까지 가능합니다.", null);
		}
		return name;
	}

	private byte[] readHead(MultipartFile file) {
		try (InputStream in = file.getInputStream()) {
			return in.readNBytes(ExecutableSignature.HEAD_SIZE);
		} catch (IOException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "UNREADABLE_FILE", "파일을 읽을 수 없습니다. 다시 시도해 주세요.");
		}
	}

	private void store(String key, MultipartFile file, String name) {
		if (bucket == null || bucket.isBlank()) {
			log.error("S3 버킷 설정(AWS_S3_BUCKET)이 없습니다.");
			throw storageUnavailable();
		}
		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType("application/octet-stream")
			.contentLength(file.getSize())
			// S3 메타데이터는 ASCII만 안전하므로 인코딩해서 원본 파일명을 남긴다
			.metadata(Map.of("original-name", URLEncoder.encode(name, StandardCharsets.UTF_8)))
			.build();
		try (InputStream in = file.getInputStream()) {
			s3.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
		} catch (SdkException | IOException e) {
			log.error("S3 저장 실패 key={}", key, e);
			throw storageUnavailable();
		}
	}

	private static ApiException storageUnavailable() {
		return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE",
			"파일 저장소에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
	}

	private static ApiException reject(String code, String message, String name) {
		log.warn("업로드 차단 code={} name={}", code, name);
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}
}
