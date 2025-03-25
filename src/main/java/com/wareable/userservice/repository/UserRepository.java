package com.wareable.userservice.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.wareable.userservice.model.AppUser;

@Repository
public interface UserRepository extends MongoRepository<AppUser, String> {
  Optional<AppUser> findByUsername(String username);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);
}
