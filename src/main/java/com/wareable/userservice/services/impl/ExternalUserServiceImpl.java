package com.wareable.userservice.services.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wareable.userservice.logging.LogUploaderService;
import com.wareable.userservice.model.AppUser;
import com.wareable.userservice.payload.request.ExternalUser;
import com.wareable.userservice.repository.UserRepository;
import com.wareable.userservice.service.ExternalUserService;

@Service
public class ExternalUserServiceImpl implements ExternalUserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LogUploaderService logUploaderService;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private ObjectMapper objectMapper; // add this as a Spring bean if not already

	@Override
	public List<AppUser> getUsers() {
		logUploaderService.appendLogToS3("Fetching all users from MongoDB...");
		List<AppUser> users = userRepository.findAll();
		logUploaderService.appendLogToS3("Total users fetched: " + users.size());
		return users;
	}

	@Override
	public void importUsersFromCustomUrl(String url) {
		RestTemplate restTemplate = new RestTemplate();
		logUploaderService.appendLogToS3("🌐 Fetching JSON from URL: " + url);

		try {
			ExternalUser[] externalUsers = restTemplate.getForObject(url, ExternalUser[].class);
			if (externalUsers == null || externalUsers.length == 0) {
				logUploaderService.appendLogToS3("⚠ No users found at the provided URL.");
				return;
			}

			List<AppUser> savedUsers = Arrays.stream(externalUsers).map(externalUser -> {
				logUploaderService.appendLogToS3("User Name: " + externalUser.getUsername());
				AppUser appUser = new AppUser();
				appUser.setUsername(externalUser.getUsername());
				appUser.setEmail(externalUser.getEmail());
				appUser.setPassword("external_dummy_password");

				AppUser saved = userRepository.save(appUser);
				logUploaderService.appendLogToS3("Saved user to MongoDB: " + saved.getId());
				return saved;
			}).toList();

			logUploaderService.appendLogToS3("Finished importing " + savedUsers.size() + " users from custom URL.");

		} catch (Exception e) {
			logUploaderService.appendLogToS3("Error while importing from URL: " + e.getMessage());
			throw new RuntimeException("Failed to import users from URL", e);
		}
	}

	@Override
	public void importRawJsonFromUrl(String url) {
		RestTemplate restTemplate = new RestTemplate();
		logUploaderService.appendLogToS3("Fetching raw JSON from: " + url);

		try {
			Object rawJson = restTemplate.getForObject(url, Object.class);
			logUploaderService.appendLogToS3("Raw JSON fetched: " + rawJson.getClass().getSimpleName());

			if (rawJson instanceof List<?> rawList) {
				for (Object item : rawList) {
					Document doc = new Document(objectMapper.convertValue(item, Map.class));
					mongoTemplate.insert(doc, "external_raw_data");
				}
				logUploaderService.appendLogToS3(
						"Saved " + rawList.size() + " documents to MongoDB collection: external_raw_data");

			} else if (rawJson instanceof Map<?, ?> rawMap) {
				Document doc = new Document(objectMapper.convertValue(rawMap, Map.class));
				mongoTemplate.insert(doc, "external_raw_data");
				logUploaderService.appendLogToS3("Saved 1 document to MongoDB collection: external_raw_data");

			} else {
				logUploaderService.appendLogToS3("Unsupported JSON structure. Skipped saving.");
			}

		} catch (Exception e) {
			logUploaderService.appendLogToS3("Error importing raw JSON: " + e.getMessage());
			throw new RuntimeException("Failed to import unstructured JSON", e);
		}
	}

	@Override
	public List<Map<String, Object>> getJsonData() {
	    List<Document> docs = mongoTemplate.findAll(Document.class, "external_raw_data");
	    return docs.stream()
	               .map(doc -> doc.entrySet().stream()
	                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
	               .toList();
	}

}
