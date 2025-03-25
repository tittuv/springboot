package com.wareable.userservice.security;

import org.springframework.stereotype.Service;

@Service
public class RolePermissionChecker {

	private final RolePermissionsConfig rolePermissionsConfig;

	public RolePermissionChecker(RolePermissionsConfig config) {
		this.rolePermissionsConfig = config;
	}

	public boolean hasPermission(String role, String permission) {
		return rolePermissionsConfig.getRoles().getOrDefault(role, new RolePermissionsConfig.Role()).getPermissions()
				.contains(permission);
	}
}