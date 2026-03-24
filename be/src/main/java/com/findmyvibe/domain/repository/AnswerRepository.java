package com.findmyvibe.domain.repository;

import com.findmyvibe.domain.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySessionId(UUID sessionId);
}
