package com.demobank.api.controller;

import com.demobank.api.config.CorsConfig;
import com.demobank.api.config.SecurityConfig;
import com.demobank.api.dto.TransactionResponse;
import com.demobank.api.security.JwtAuthenticationFilter;
import com.demobank.api.security.JwtService;
import com.demobank.api.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
class TransactionControllerSecurityTest {

    private static final String TRANSFER_BODY = """
            {"sourceAccountNumber":"FR76SOURCE0001","targetAccountNumber":"FR76TARGET0001","amount":10.00,"description":"Loyer"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void historyIsRejectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/transactions/history/FR76SOURCE0001"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(transactionService);
    }

    @Test
    void transferIsRejectedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRANSFER_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(transactionService);
    }

    @Test
    @WithMockUser(username = "alice@demobank.test")
    void historyIsServedToAuthenticatedUser() throws Exception {
        when(transactionService.getHistory("FR76SOURCE0001")).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/history/FR76SOURCE0001"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alice@demobank.test")
    void transferIsServedToAuthenticatedUser() throws Exception {
        when(transactionService.transfer(any())).thenReturn(TransactionResponse.builder()
                .id(1L)
                .amount(new BigDecimal("10.00"))
                .sourceAccount("FR76SOURCE0001")
                .targetAccount("FR76TARGET0001")
                .build());

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRANSFER_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceAccount").value("FR76SOURCE0001"));
    }
}
