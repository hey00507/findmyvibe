package com.findmyvibe.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime answeredAt;

    private Answer(Session session, Question question, String content) {
        this.session = session;
        this.question = question;
        this.content = content;
        this.answeredAt = LocalDateTime.now();
    }

    public static Answer create(Session session, Question question, String content) {
        return new Answer(session, question, content);
    }
}
