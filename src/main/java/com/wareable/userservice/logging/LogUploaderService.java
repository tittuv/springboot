package com.wareable.userservice.logging;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class LogUploaderService {

	private final S3Client s3Client;

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	public LogUploaderService(@Value("${aws.accessKey}") String accessKey, @Value("${aws.secretKey}") String secretKey,
			@Value("${aws.region}") String region) {

		this.s3Client = S3Client.builder().region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
				.endpointOverride(URI.create("http://localhost:4566")).forcePathStyle(true).build();
	}

	@PostConstruct
	public void init() {
		try {
			CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();
			s3Client.createBucket(createBucketRequest);
		} catch (S3Exception e) {
			System.out.println("Bucket might already exist: " + e.awsErrorDetails().errorMessage());
		}
	}

	public void appendLogToS3(String logContent) {
		String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE); // e.g., 2025-03-12
		String logFileKey = "logs/app-log-" + today + ".log";

		StringBuilder updatedLog = new StringBuilder();

		try {
			// Try to read existing file
			GetObjectRequest getRequest = GetObjectRequest.builder().bucket(bucketName).key(logFileKey).build();

			String existingLog = s3Client.getObjectAsBytes(getRequest).asUtf8String();
			updatedLog.append(existingLog);

		} catch (NoSuchKeyException e) {
			// File not found in S3 – first time writing
			System.out.println("ℹ️ Log file does not exist yet. Creating new one.");
		} catch (SdkClientException e) {
			System.out.println("Could not connect to S3: " + e.getMessage());
			return;
		}

		// Append new log
		updatedLog.append(logContent).append(System.lineSeparator());

		// Upload updated file
		PutObjectRequest putRequest = PutObjectRequest.builder().bucket(bucketName).key(logFileKey).build();

		s3Client.putObject(putRequest, RequestBody.fromBytes(updatedLog.toString().getBytes(StandardCharsets.UTF_8)));
	}
}
