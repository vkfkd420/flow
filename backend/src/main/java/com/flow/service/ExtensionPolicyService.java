package com.flow.service;

import com.flow.common.ApiException;
import com.flow.common.ExtensionRule;
import com.flow.dao.ExtensionPolicyDao;
import com.flow.dto.CustomExtension;
import com.flow.dto.ExtensionPolicy;
import com.flow.dto.ExtensionPolicyResponse;
import com.flow.dto.FixedExtension;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtensionPolicyService {

	public static final int CUSTOM_LIMIT = 200;

	private final ExtensionPolicyDao dao;

	public ExtensionPolicyService(ExtensionPolicyDao dao) {
		this.dao = dao;
	}

	@Transactional(readOnly = true)
	public ExtensionPolicyResponse getPolicy() {
		List<ExtensionPolicy> all = dao.findAll();
		List<FixedExtension> fixed = all.stream()
			.filter(p -> ExtensionPolicy.FIXED.equals(p.type()))
			.map(FixedExtension::from)
			.toList();
		List<CustomExtension> custom = all.stream()
			.filter(p -> ExtensionPolicy.CUSTOM.equals(p.type()))
			.map(CustomExtension::from)
			.toList();
		return new ExtensionPolicyResponse(fixed, custom, custom.size(), CUSTOM_LIMIT);
	}

	@Transactional
	public FixedExtension updateFixed(String extension, boolean blocked) {
		if (dao.updateFixedBlocked(extension, blocked) == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "FIXED_EXTENSION_NOT_FOUND",
				"'" + extension + "'는 고정 확장자가 아닙니다.");
		}
		return new FixedExtension(extension, blocked);
	}

	@Transactional
	public CustomExtension addCustom(String input) {
		String extension = ExtensionRule.normalize(input);
		if (extension == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXTENSION",
				"확장자는 영문과 숫자로 1~" + ExtensionRule.MAX_LENGTH + "자까지 입력할 수 있습니다.");
		}

		// 개수 확인과 INSERT 사이에 다른 요청이 끼어들지 않도록 먼저 잠근다 (200개 초과 방지)
		int count = dao.countCustomForUpdate();

		ExtensionPolicy existing = dao.findByExtension(extension);
		if (existing != null) {
			throw duplicate(extension, existing);
		}
		if (count >= CUSTOM_LIMIT) {
			throw new ApiException(HttpStatus.CONFLICT, "CUSTOM_LIMIT_EXCEEDED",
				"커스텀 확장자는 최대 " + CUSTOM_LIMIT + "개까지 추가할 수 있습니다.");
		}

		try {
			dao.insertCustom(extension);
		} catch (DuplicateKeyException e) {
			// 잠금으로 막히지 않는 경로(고정 확장자와 동시 변경 등)의 최종 방어선은 UNIQUE 제약
			throw duplicate(extension, dao.findByExtension(extension));
		}
		return CustomExtension.from(dao.findByExtension(extension));
	}

	@Transactional
	public void deleteCustom(Long id) {
		if (dao.deleteCustom(id) == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "CUSTOM_EXTENSION_NOT_FOUND",
				"삭제할 커스텀 확장자가 없습니다. 이미 삭제되었을 수 있습니다.");
		}
	}

	private ApiException duplicate(String extension, ExtensionPolicy existing) {
		if (existing != null && ExtensionPolicy.FIXED.equals(existing.type())) {
			return new ApiException(HttpStatus.CONFLICT, "FIXED_EXTENSION_CONFLICT",
				"'" + extension + "'는 고정 확장자입니다. 고정 확장자 영역에서 체크해 주세요.");
		}
		return new ApiException(HttpStatus.CONFLICT, "DUPLICATE_EXTENSION",
			"'" + extension + "'는 이미 추가된 확장자입니다.");
	}
}
