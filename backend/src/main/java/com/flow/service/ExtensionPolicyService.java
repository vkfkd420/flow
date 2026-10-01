package com.flow.service;

import com.flow.common.ApiException;
import com.flow.common.ErrorCode;
import com.flow.common.ExtensionRule;
import com.flow.dao.ExtensionPolicyDao;
import com.flow.dto.CustomExtension;
import com.flow.dto.ExtensionPolicy;
import com.flow.dto.ExtensionPolicyResponse;
import com.flow.dto.FixedExtension;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
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
			throw new ApiException(ErrorCode.FIXED_EXTENSION_NOT_FOUND, extension);
		}
		return new FixedExtension(extension, blocked);
	}

	@Transactional
	public CustomExtension addCustom(String input) {
		String extension = ExtensionRule.normalize(input);
		if (extension == null) {
			throw new ApiException(ErrorCode.INVALID_EXTENSION, ExtensionRule.MAX_LENGTH);
		}

		// 개수 확인과 INSERT 사이에 다른 요청이 끼어들지 않도록 먼저 잠근다 (200개 초과 방지)
		int count = dao.countCustomForUpdate();

		ExtensionPolicy existing = dao.findByExtension(extension);
		if (existing != null) {
			throw duplicate(extension, existing);
		}
		if (count >= CUSTOM_LIMIT) {
			throw new ApiException(ErrorCode.CUSTOM_LIMIT_EXCEEDED, CUSTOM_LIMIT);
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
			throw new ApiException(ErrorCode.CUSTOM_EXTENSION_NOT_FOUND);
		}
	}

	private ApiException duplicate(String extension, ExtensionPolicy existing) {
		boolean fixed = existing != null && ExtensionPolicy.FIXED.equals(existing.type());
		return new ApiException(fixed ? ErrorCode.FIXED_EXTENSION_CONFLICT : ErrorCode.DUPLICATE_EXTENSION, extension);
	}
}
