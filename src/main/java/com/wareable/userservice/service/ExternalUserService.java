package com.wareable.userservice.service;

import java.util.List;
import java.util.Map;

import com.wareable.userservice.model.AppUser;

public interface ExternalUserService {

	List<AppUser> getUsers();

	void importUsersFromCustomUrl(String url);

	void importRawJsonFromUrl(String url);

	List<Map<String, Object>> getJsonData();
}
