package com.wareable.userservice.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wareable.userservice.logging.LogUploaderService;
import com.wareable.userservice.model.AppUser;
import com.wareable.userservice.model.ERole;
import com.wareable.userservice.model.Role;
import com.wareable.userservice.payload.request.LoginRequest;
import com.wareable.userservice.payload.request.SignupRequest;
import com.wareable.userservice.payload.response.JwtResponse;
import com.wareable.userservice.payload.response.MessageResponse;
import com.wareable.userservice.repository.RoleRepository;
import com.wareable.userservice.repository.UserRepository;
import com.wareable.userservice.security.jwt.JwtUtils;
import com.wareable.userservice.services.impl.UserDetailsImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "User Controller", description = "CRUD operations for application users")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	LogUploaderService logUploaderService;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
    @Operation(summary = "User Sign In", 
			description = "Authenticates a user with username and password, returns a JWT token on success.", security = @SecurityRequirement(name = ""))
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		logUploaderService.appendLogToS3("API REQUEST: /signin by " + loginRequest.getUsername());

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
					.collect(Collectors.toList());

			logUploaderService.appendLogToS3("DB TRANSACTION: Authenticated user " + loginRequest.getUsername());

			return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(),
					userDetails.getEmail(), roles));
		} catch (Exception e) {
			logUploaderService.appendLogToS3("ERROR during /signin: " + e.getMessage());
			throw e;
		}
	}

	@PostMapping("/signup")
	@Operation(
		    summary = "User Sign Up",
		    description = "Registers a new user by taking username, email, password, and roles.", security = @SecurityRequirement(name = "")
		)public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		logUploaderService.appendLogToS3("API REQUEST: /signup by " + signUpRequest.getUsername());

		try {
			if (userRepository.existsByUsername(signUpRequest.getUsername())) {
				logUploaderService.appendLogToS3("DB CHECK: Username already exists: " + signUpRequest.getUsername());
				return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
			}

			if (userRepository.existsByEmail(signUpRequest.getEmail())) {
				logUploaderService.appendLogToS3("DB CHECK: Email already in use: " + signUpRequest.getEmail());
				return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
			}

			AppUser user = new AppUser(signUpRequest.getUsername(), signUpRequest.getEmail(),
					encoder.encode(signUpRequest.getPassword()));

			Set<String> strRoles = signUpRequest.getRole();
			Set<Role> roles = new HashSet<>();

			if (strRoles == null) {
				Role userRole = roleRepository.findByName(ERole.ROLE_USER)
						.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
				roles.add(userRole);
			} else {
				strRoles.forEach(role -> {
					switch (role) {
					case "admin":
						Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(adminRole);
						break;
					case "mod":
						Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(modRole);
						break;
					default:
						Role userRole = roleRepository.findByName(ERole.ROLE_USER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
						roles.add(userRole);
					}
				});
			}

			user.setRoles(roles);
			userRepository.save(user);

			logUploaderService.appendLogToS3("DB TRANSACTION: New user registered: " + signUpRequest.getUsername());

			return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
		} catch (Exception e) {
			logUploaderService.appendLogToS3("ERROR during /signup: " + e.getMessage());
			throw e;
		}
	}

	// Update user by ID
	@PutMapping("/{id}")
	@Operation(summary = "Update User", description = "Update an existing user's information by ID.")
	public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody AppUser updatedUser) {
		Optional<AppUser> userOptional = userRepository.findById(id);

		if (userOptional.isPresent()) {
			AppUser existingUser = userOptional.get();
			existingUser.setUsername(updatedUser.getUsername());
			existingUser.setEmail(updatedUser.getEmail());
			// You can include other fields as needed

			userRepository.save(existingUser);
			return ResponseEntity.ok(existingUser);
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	// Delete user by ID
	@DeleteMapping("/{id}")
	@Operation(summary = "Delete User", description = "Delete a user by ID.")
	public ResponseEntity<String> deleteUser(@PathVariable String id) {
		if (userRepository.existsById(id)) {
			userRepository.deleteById(id);
			return ResponseEntity.ok("User deleted successfully");
		} else {
			return ResponseEntity.notFound().build();
		}
	}

}