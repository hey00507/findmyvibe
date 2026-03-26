package com.findmyvibe.common.config;

import com.findmyvibe.domain.service.AiAnalysisPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiAnalysisPort aiAnalysisPort;

    @Test
    @DisplayName("CORS preflight 요청이 허용된다 (localhost:3000)")
    void corsPreflight_allowsLocalhost3000() throws Exception {
        mockMvc.perform(options("/api/v1/sessions")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    @DisplayName("허용되지 않은 Origin은 CORS 헤더가 없다")
    void corsPreflight_rejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/sessions")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
