package com.findmyvibe.domain.repository;

import com.findmyvibe.domain.entity.Question;
import com.findmyvibe.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySessionOrderByOrderIndex(Session session);
}
