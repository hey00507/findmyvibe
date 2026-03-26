package com.findmyvibe.domain.service;

import com.findmyvibe.domain.constant.BasicQuestions;
import com.findmyvibe.domain.entity.Question;
import com.findmyvibe.domain.entity.Session;
import com.findmyvibe.domain.repository.QuestionRepository;
import com.findmyvibe.domain.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public SessionCreateResult createSession() {
        Session session = Session.create();
        sessionRepository.save(session);

        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < BasicQuestions.COUNT; i++) {
            questions.add(Question.createBasic(session, BasicQuestions.CONTENTS.get(i), i + 1));
        }
        List<Question> savedQuestions = questionRepository.saveAll(questions);

        return new SessionCreateResult(session, savedQuestions);
    }

    public record SessionCreateResult(Session session, List<Question> questions) {
    }
}
