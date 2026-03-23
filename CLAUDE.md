# CLAUDE.md — FindMyVibe

## 프로젝트 개요
AI 성향 분석 + 취미/원데이클래스 추천 웹앱 (1인 개발)

## 모노레포 구조
- `be/` — Spring Boot 백엔드 (Java 25)
- `fe/` — React 프론트엔드 (Phase 3 구현 예정)
- `docs/` — 공통 문서 (ADR, 설계)

## 개발 원칙
- 100% API 테스트 커버리지
- 3회 반복 시 메서드 추출
- 미사용 자원 즉시 제거
- PRD 정의 → 기존 코드 확인 → 테스트 작성 → 구현 → 리팩토링

## 커밋 컨벤션
- `{Type}: {한글 설명}` 형식
- Type: `Feat`, `Fix`, `Refactor`, `Test`, `Docs`, `Chore`
- 커밋 전 테스트 통과 확인
