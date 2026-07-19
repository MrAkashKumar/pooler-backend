package com.akash.pooler_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

@Configuration
public class AwsS3Config {

    @Bean(destroyMethod = "close")
    public S3Client profileMediaS3Client(ProfileMediaProperties properties) {
        return S3Client.builder()
                .region(Region.of(nonBlankOrDefault(properties.getS3Region(), "ap-southeast-1")))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(10))
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .build())
                .build();
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
