package com.wareable.userservice.controller;

import com.wareable.userservice.model.AppUser;
import com.wareable.userservice.service.ExternalUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/external")
@Tag(name = "External User Controller", description = "APIs to fetch and manage external users and unstructured JSON data.")
public class ExternalUserController {

	@Autowired
	private ExternalUserService externalUserService;

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/fetch-user-data")
	@Operation(summary = "Fetch & Save Users from External API", description = "Fetches user data from a predefined external API (https://jsonplaceholder.typicode.com/users) and saves it into the database.")
	public ResponseEntity<String> fetchAndSaveUser() {
		try {
			String apiUrl = "https://jsonplaceholder.typicode.com/users";
			externalUserService.importUsersFromCustomUrl(apiUrl);
			return ResponseEntity.ok("Users imported successfully from: " + apiUrl);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
		}
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/list-user-data")
	@Operation(summary = "List Saved Users", description = "Retrieves a list of all users previously fetched from the external API and stored in the database.")
	public List<AppUser> getUsers() {
		return externalUserService.getUsers();
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/save-json-data")
	@Operation(summary = "Import JSON Data from Custom URL", description = "Accepts a custom JSON URL in the request body and imports unstructured data into the system.")
	public ResponseEntity<String> importDataFromUrl(@RequestBody Map<String, String> requestBody) {
		String url = requestBody.get("url");
		if (url == null || url.isBlank()) {
			return ResponseEntity.badRequest().body("Missing 'url' in request body");
		}

		try {
			externalUserService.importRawJsonFromUrl(url);
			return ResponseEntity.ok("Users imported successfully from: " + url);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
		}
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/list-json-data")
	@Operation(summary = "List Unstructured JSON Data", description = "Retrieves the list of imported raw/unstructured JSON data from the database.")
	public ResponseEntity<List<Map<String, Object>>> getUnstructuredData() {
		List<Map<String, Object>> data = externalUserService.getJsonData();
		return ResponseEntity.ok(data);
	}
}
