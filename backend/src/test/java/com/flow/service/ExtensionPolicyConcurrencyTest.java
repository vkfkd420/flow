package com.flow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.flow.common.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 200개 제한이 동시 요청에서도 지켜지는지 확인한다.
 * 실제로 커밋해야 동시성을 볼 수 있으므로 롤백하지 않고, 이 테스트가 넣은 행(PREFIX)만 지운다.
 */
@SpringBootTest
class ExtensionPolicyConcurrencyTest {

	private static final String PREFIX = "zzconc";

	@Autowired
	ExtensionPolicyService service;

	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM FILE_EXTENSION_POLICY WHERE TYPE = 'CUSTOM' AND EXTENSION LIKE ?", PREFIX + "%");
	}

	@Test
	void 남은_자리가_1개일_때_동시에_10건을_추가해도_1건만_성공한다() throws Exception {
		int existing = countCustom();
		assumeTrue(existing < ExtensionPolicyService.CUSTOM_LIMIT, "이미 커스텀 확장자가 200개 이상이면 건너뜀");
		for (int i = existing; i < ExtensionPolicyService.CUSTOM_LIMIT - 1; i++) {
			jdbc.update("INSERT INTO FILE_EXTENSION_POLICY (EXTENSION, TYPE) VALUES (?, 'CUSTOM')", PREFIX + "f" + i);
		}

		int threads = 10;
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		List<Future<String>> results = new ArrayList<>();
		for (int t = 0; t < threads; t++) {
			String extension = PREFIX + "t" + t;
			results.add(pool.submit(() -> {
				start.await();
				try {
					service.addCustom(extension);
					return "OK";
				} catch (ApiException e) {
					return e.getCode();
				}
			}));
		}
		start.countDown();

		List<String> codes = new ArrayList<>();
		for (Future<String> result : results) {
			codes.add(result.get());
		}
		pool.shutdown();

		assertThat(codes).filteredOn("OK"::equals).hasSize(1);
		assertThat(codes).filteredOn("CUSTOM_LIMIT_EXCEEDED"::equals).hasSize(threads - 1);
		assertThat(countCustom()).isEqualTo(ExtensionPolicyService.CUSTOM_LIMIT);
	}

	private int countCustom() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM FILE_EXTENSION_POLICY WHERE TYPE = 'CUSTOM'", Integer.class);
	}
}
