package com.findmyvibe.domain.entity;

import com.findmyvibe.domain.enums.QuestionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int orderIndex;

    private Question(Session session, QuestionType type, String content, int orderIndex) {
        this.session = session;
        this.type = type;
        this.content = content;
        this.orderIndex = orderIndex;
    }

    public static Question createBasic(Session session, String content, int orderIndex) {
        return new Question(session, QuestionType.BASIC, content, orderIndex);
    }

    public static Question createFollowUp(Session session, String content, int orderIndex) {
        return new Question(session, QuestionType.FOLLOW_UP, content, orderIndex);
    }
}
