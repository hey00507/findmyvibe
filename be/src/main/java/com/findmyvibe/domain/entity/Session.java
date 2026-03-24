package com.findmyvibe.domain.entity;

import com.findmyvibe.domain.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    private LocalDateTime completedAt;

    private Session(UUID id, SessionStatus status) {
        this.id = id;
        this.status = status;
    }

    public static Session create() {
        return new Session(UUID.randomUUID(), SessionStatus.CREATED);
    }

    public void markBasicAnswered() {
        if (this.status != SessionStatus.CREATED) {
            throw new IllegalStateException(
                    "BASIC_ANSWERED로 전이하려면 CREATED 상태여야 합니다. 현재: " + this.status);
        }
        this.status = SessionStatus.BASIC_ANSWERED;
    }

    public void markCompleted() {
        if (this.status != SessionStatus.BASIC_ANSWERED) {
            throw new IllegalStateException(
                    "COMPLETED로 전이하려면 BASIC_ANSWERED 상태여야 합니다. 현재: " + this.status);
        }
        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
