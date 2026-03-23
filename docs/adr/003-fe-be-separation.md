# ADR-003: FE/BE 분리 배포 (Vercel + AWS)

## 상태
Accepted (2026-03-22)

## 맥락
웹앱을 배포할 때 세 가지 선택지가 있었다:

- **A. 모노리스** — Spring Boot에서 정적 리소스 서빙 (SSR 또는 static/)
- **B. AWS 올인** — FE: CloudFront + S3, BE: ECS Fargate
- **C. FE/BE 분리** — FE: Vercel, BE: AWS ECS Fargate

## 결정
**C. FE: Vercel, BE: AWS** 분리 배포를 선택한다.

## 근거

### 역할 분리
- 1인 개발이지만 FE/BE를 독립 프로젝트로 관리
- 독립 배포: FE 수정 시 BE 재배포 불필요, 반대도 마찬가지
- 독립 롤백: 한쪽 문제 시 다른 쪽 영향 없음

### Vercel 선택 이유
- GitHub 연동 한 줄로 자동 배포
- PR마다 프리뷰 URL 자동 생성
- Hobby 플랜 무료 (개인 프로젝트 충분)
- BE에서 이미 AWS 인프라 역량을 보여주므로 FE까지 AWS에 올릴 필요 없음

### AWS 올인 대비 장점
- CloudFront + S3 + Route53 + ACM 설정 생략 → 셋업 시간 절약
- 인프라 설정 대신 API 완성도에 집중

## 결과
- BE: `api.findmyvibe.com` (AWS ALB → ECS Fargate)
- FE: `findmyvibe.com` (Vercel)
- CORS 허용: `findmyvibe.com`, `*.vercel.app`, `localhost:5173`
- API prefix: `/api/v1/**`
- 에러 응답: RFC 7807 Problem Details
