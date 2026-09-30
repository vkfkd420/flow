package com.flow.controller;

import com.flow.dto.CustomAddRequest;
import com.flow.dto.CustomExtension;
import com.flow.dto.ExtensionPolicyResponse;
import com.flow.dto.FixedExtension;
import com.flow.dto.FixedUpdateRequest;
import com.flow.service.ExtensionPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/extensions")
public class ExtensionPolicyController {

	private final ExtensionPolicyService service;

	public ExtensionPolicyController(ExtensionPolicyService service) {
		this.service = service;
	}

	@GetMapping
	public ExtensionPolicyResponse getPolicy() {
		return service.getPolicy();
	}

	@PatchMapping("/fixed/{extension}")
	public FixedExtension updateFixed(@PathVariable String extension, @Valid @RequestBody FixedUpdateRequest request) {
		return service.updateFixed(extension, request.blocked());
	}

	@PostMapping("/custom")
	@ResponseStatus(HttpStatus.CREATED)
	public CustomExtension addCustom(@Valid @RequestBody CustomAddRequest request) {
		return service.addCustom(request.extension());
	}

	@DeleteMapping("/custom/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCustom(@PathVariable Long id) {
		service.deleteCustom(id);
	}
}
