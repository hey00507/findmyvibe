# CLAUDE.md — FindMyVibe

## Package Structure
```
com.findmyvibe/
├── api/                 # Controller, DTO
├── domain/              # Entity, Repository, Service
├── infrastructure/      # Claude Client, Redis, Crawler
└── common/              # Config, Exception
```

## Dependency Rules
- domain must not depend on api or infrastructure
- controller must not access repository directly
- infrastructure must not depend on api

## Development Process
1. Define PRD first
2. Check existing code for reusable parts
3. Write failing tests
4. Implement minimal code to pass
5. Refactor

## Principles
- 100% API test coverage
- Extract method after 3 repetitions
- Remove unused resources immediately
