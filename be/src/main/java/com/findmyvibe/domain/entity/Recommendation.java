package com.findmyvibe.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false, length = 100)
    private String hobbyName;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private int matchScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(length = 500)
    private String classUrl;

    @Column(nullable = false)
    private int orderIndex;

    private Recommendation(Session session, String hobbyName, String category,
                           int matchScore, String reason, int orderIndex) {
        if (matchScore < 0 || matchScore > 100) {
            throw new IllegalArgumentException("matchScore는 0~100 사이여야 합니다: " + matchScore);
        }
        this.session = session;
        this.hobbyName = hobbyName;
        this.category = category;
        this.matchScore = matchScore;
        this.reason = reason;
        this.orderIndex = orderIndex;
    }

    public static Recommendation create(Session session, String hobbyName, String category,
                                         int matchScore, String reason, int orderIndex) {
        return new Recommendation(session, hobbyName, category, matchScore, reason, orderIndex);
    }
}
