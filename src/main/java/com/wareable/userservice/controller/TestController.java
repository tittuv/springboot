package com.wareable.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wareable.userservice.logging.LogUploaderService;

@RestController
@RequestMapping("/api/log")
public class TestController {

	private final LogUploaderService logUploaderService;

	public TestController(LogUploaderService logUploaderService) {
		this.logUploaderService = logUploaderService;
	}

	@GetMapping("/simulate")
    @PreAuthorize("isAuthenticated()")
	public ResponseEntity<String> simulateLogs() {
		StringBuilder logBuilder = new StringBuilder();

		// Mock API Request
		logBuilder.append("INFO: API Request received at /api/test/simulate\n");

		// Mock DB Transaction
		logBuilder.append("INFO: DB Transaction - User fetched from DB\n");

		// Mock Error
		try {
			int result = 10 / 0;
		} catch (Exception e) {
			logBuilder.append("ERROR: Division by zero - ").append(e.getMessage()).append("\n");
		}

		// Upload to S3
		logUploaderService.appendLogToS3(logBuilder.toString());

		return ResponseEntity.ok("Log simulated and uploaded to S3.");
	}
}