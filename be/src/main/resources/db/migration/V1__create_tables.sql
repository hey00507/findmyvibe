-- ===========================================
-- V1: FindMyVibe Phase 1 테이블 생성
-- Entity 5개 + BaseEntity 공통 컬럼(Auditing)
-- ===========================================

-- Session: 분석 한 건의 단위
CREATE TABLE sessions (
    id           UUID PRIMARY KEY,
    status       VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    completed_at TIMESTAMP,

    -- BaseEntity (JPA Auditing)
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(255),
    modified_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    modified_by  VARCHAR(255)
);

-- Question: 기본 질문 7개 + Claude가 생성하는 꼬리질문
CREATE TABLE questions (
    id           BIGSERIAL PRIMARY KEY,
    session_id   UUID         NOT NULL REFERENCES sessions(id),
    type         VARCHAR(10)  NOT NULL,
    content      TEXT         NOT NULL,
    order_index  INT          NOT NULL,

    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(255),
    modified_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    modified_by  VARCHAR(255)
);

-- Answer: 사용자의 답변
CREATE TABLE answers (
    id           BIGSERIAL PRIMARY KEY,
    session_id   UUID         NOT NULL REFERENCES sessions(id),
    question_id  BIGINT       NOT NULL REFERENCES questions(id),
    content      TEXT         NOT NULL,

    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(255),
    modified_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    modified_by  VARCHAR(255)
);

-- Profile: Claude가 분석한 성향 프로필 (Session과 1:1)
CREATE TABLE profiles (
    id           BIGSERIAL PRIMARY KEY,
    session_id   UUID         NOT NULL UNIQUE REFERENCES sessions(id),
    type_label   VARCHAR(50)  NOT NULL,
    description  TEXT         NOT NULL,
    keywords     JSONB        NOT NULL DEFAULT '[]',
    traits       JSONB        NOT NULL,

    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(255),
    modified_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    modified_by  VARCHAR(255)
);

-- Recommendation: 추천 취미/클래스 (Session당 5개)
CREATE TABLE recommendations (
    id           BIGSERIAL PRIMARY KEY,
    session_id   UUID         NOT NULL REFERENCES sessions(id),
    hobby_name   VARCHAR(100) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    match_score  INT          NOT NULL CHECK (match_score BETWEEN 0 AND 100),
    reason       TEXT         NOT NULL,
    class_url    VARCHAR(500),
    order_index  INT          NOT NULL,

    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(255),
    modified_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    modified_by  VARCHAR(255)
);

-- 조회 성능을 위한 인덱스
CREATE INDEX idx_questions_session        ON questions(session_id);
CREATE INDEX idx_answers_session          ON answers(session_id);
CREATE INDEX idx_recommendations_session  ON recommendations(session_id);
