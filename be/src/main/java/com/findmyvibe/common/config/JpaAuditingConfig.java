package com.findmyvibe.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        // Phase 1: 인증 없이 "system" 반환
        // Phase 2+: SecurityContextHolder에서 사용자 정보 추출
        return () -> Optional.of("system");
    }
}
