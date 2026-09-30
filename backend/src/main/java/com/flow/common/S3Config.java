package com.flow.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 자격 증명은 AWS SDK 기본 체인에서 찾는다.
 * 로컬: ~/.aws/credentials (aws configure), AWS 배포: IAM 역할 → 앱 설정에 액세스 키를 두지 않는다.
 */
@Configuration
public class S3Config {

	@Bean
	public S3Client s3Client(@Value("${aws.region}") String region) {
		return S3Client.builder().region(Region.of(region)).build();
	}
}
