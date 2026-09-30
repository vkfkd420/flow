package com.flow.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 DB에 schema.sql, seed.sql이 적용되어 있어야 한다. 테스트마다 롤백된다. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExtensionPolicyApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void 정책_조회_고정_확장자는_커스텀_목록에_없다() throws Exception {
		mvc.perform(get("/api/extensions"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fixed.length()").value(7))
			.andExpect(jsonPath("$.fixed[*].extension").value(hasItem("exe")))
			.andExpect(jsonPath("$.custom[*].extension").value(not(hasItem("exe"))))
			.andExpect(jsonPath("$.customLimit").value(200));
	}

	@Test
	void 고정_확장자_체크_변경이_저장된다() throws Exception {
		mvc.perform(patch("/api/extensions/fixed/exe").contentType(MediaType.APPLICATION_JSON).content("{\"blocked\":true}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.blocked").value(true));

		mvc.perform(get("/api/extensions"))
			.andExpect(jsonPath("$.fixed[?(@.extension == 'exe')].blocked").value(hasItem(true)));
	}

	@Test
	void 고정_확장자가_아니면_404() throws Exception {
		mvc.perform(patch("/api/extensions/fixed/sh").contentType(MediaType.APPLICATION_JSON).content("{\"blocked\":true}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FIXED_EXTENSION_NOT_FOUND"));
	}

	@Test
	void 고정_확장자_체크값이_없으면_400() throws Exception {
		mvc.perform(patch("/api/extensions/fixed/exe").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 커스텀_추가는_정규화해서_저장하고_삭제할_수_있다() throws Exception {
		MvcResult result = mvc.perform(addCustom(" .SH "))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.extension").value("sh"))
			.andReturn();
		String id = result.getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");

		mvc.perform(get("/api/extensions"))
			.andExpect(jsonPath("$.custom[*].extension").value(hasItem("sh")));

		mvc.perform(delete("/api/extensions/custom/" + id)).andExpect(status().isNoContent());
		mvc.perform(delete("/api/extensions/custom/" + id))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CUSTOM_EXTENSION_NOT_FOUND"));
	}

	@Test
	void 커스텀_중복_추가는_409() throws Exception {
		mvc.perform(addCustom("sh")).andExpect(status().isCreated());
		mvc.perform(addCustom("SH"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_EXTENSION"));
	}

	@Test
	void 커스텀에_고정_확장자를_입력하면_409와_안내() throws Exception {
		mvc.perform(addCustom("EXE"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("FIXED_EXTENSION_CONFLICT"));
	}

	@Test
	void 커스텀_형식_오류는_400() throws Exception {
		mvc.perform(addCustom("tar.gz"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_EXTENSION"));
		mvc.perform(post("/api/extensions/custom").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 커스텀_삭제는_고정_확장자를_지우지_않는다() throws Exception {
		mvc.perform(delete("/api/extensions/custom/1")) // seed의 ID 1은 고정 확장자 bat
			.andExpect(status().isNotFound());
		mvc.perform(get("/api/extensions"))
			.andExpect(jsonPath("$.fixed[*].extension").value(hasItem("bat")));
	}

	@Test
	void 잘못된_경로나_ID는_500이_아니다() throws Exception {
		mvc.perform(delete("/api/extensions/custom/abc")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/nothing")).andExpect(status().isNotFound());
		mvc.perform(post("/api/extensions")).andExpect(status().isMethodNotAllowed());
	}

	private static org.springframework.test.web.servlet.RequestBuilder addCustom(String extension) {
		return post("/api/extensions/custom").contentType(MediaType.APPLICATION_JSON)
			.content("{\"extension\":\"" + extension + "\"}");
	}
}
