package com.findmyvibe.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingConfigUnitTest {

    @Test
    @DisplayName("AuditorAware는 'system'을 반환한다")
    void auditorAwareReturnsSystem() {
        JpaAuditingConfig config = new JpaAuditingConfig();

        AuditorAware<String> auditorAware = config.auditorAware();

        assertThat(auditorAware.getCurrentAuditor())
                .isPresent()
                .hasValue("system");
    }
}
