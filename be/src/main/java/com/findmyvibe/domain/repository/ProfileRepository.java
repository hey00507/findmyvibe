package com.findmyvibe.domain.repository;

import com.findmyvibe.domain.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findBySessionId(UUID sessionId);
}
