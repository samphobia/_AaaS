package com.authservice.interfaces.rest;

import com.authservice.application.service.AuthApplicationService;
import com.authservice.application.service.CustomAttributeApplicationService;
import com.authservice.application.service.IdentityProviderClient;
import com.authservice.application.service.model.TokenPair;
import com.authservice.domain.model.AuthUser;
import com.authservice.domain.model.Role;
import com.authservice.domain.model.Tenant;
import com.authservice.infrastructure.security.TenantResolver;
import com.authservice.interfaces.dto.request.LoginRequest;
import com.authservice.interfaces.dto.request.RefreshTokenRequest;
import com.authservice.interfaces.dto.request.RegisterRequest;
import com.authservice.interfaces.dto.request.ServiceTokenRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthApplicationService authApplicationService;

        @MockBean
        private IdentityProviderClient identityProviderClient;

        @MockBean
        private TenantResolver tenantResolver;

        @MockBean
        private CustomAttributeApplicationService customAttributeApplicationService;

    @Test
    void register_ShouldReturnCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(tenantResolver.resolve(eq("dev-default-api-key"))).thenReturn(Tenant.builder()
                .id(tenantId)
                .name("Default Tenant")
                .apiKey("dev-default-api-key")
                .build());
        when(authApplicationService.register(any())).thenReturn(AuthUser.builder()
                .id(userId)
                .keycloakUserId("kc-1")
                .externalUserId("ext-1")
                .tenantId(tenantId)
                .roles(Set.of(Role.USER))
                .active(true)
                .build());

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@mail.com");
        request.setPassword("StrongPass123");
        request.setExternalUserId("ext-1");
        request.setAttributes(Map.of("name", "Alice", "idcard", "ID-123"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "dev-default-api-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keycloakUserId").value("kc-1"));
    }

    @Test
    void register_ShouldReturnCreated_WhenApiKeyHeaderMissing() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantResolver.resolve(eq(null))).thenReturn(Tenant.builder()
                .id(tenantId)
                .name("Default Tenant")
                .apiKey("dev-default-api-key")
                .build());
        when(authApplicationService.register(any())).thenReturn(AuthUser.builder()
                .id(UUID.randomUUID())
                .keycloakUserId("kc-1")
                .externalUserId("ext-standalone")
                .tenantId(tenantId)
                .roles(Set.of(Role.USER))
                .active(true)
                .build());

        RegisterRequest request = new RegisterRequest();
        request.setEmail("standalone@mail.com");
        request.setPassword("StrongPass123");
        request.setExternalUserId("ext-standalone");
        request.setAttributes(Map.of("name", "Standalone User"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keycloakUserId").value("kc-1"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenPayloadIsMalformedJson() throws Exception {
        String malformedPayload = "{" +
                "\"email\":\"test@test.com\"," +
                "\"password\":\"Test234$\"," +
                "\"externalUserId\":\"crm-10001\"," +
                "\"attributes\":{\"name\":\"JohnDoe\",}" +
                "}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "dev-default-api-key")
                        .content(malformedPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void login_ShouldReturnTokens() throws Exception {
        when(authApplicationService.login(any())).thenReturn(TokenPair.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .expiresIn(300)
                .build());

        LoginRequest request = new LoginRequest();
        request.setUsername("test@mail.com");
        request.setPassword("StrongPass123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "dev-default-api-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void refresh_ShouldReturnTokens() throws Exception {
        when(authApplicationService.refresh(any())).thenReturn(TokenPair.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .expiresIn(300)
                .build());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "dev-default-api-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void me_ShouldReturnCurrentUser() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(authApplicationService.getCurrentUser(eq("kc-1"))).thenReturn(AuthUser.builder()
                .id(UUID.randomUUID())
                .keycloakUserId("kc-1")
                .externalUserId("ext-1")
                .tenantId(tenantId)
                .roles(Set.of(Role.USER))
                .active(true)
                .build());
        when(customAttributeApplicationService.getUserAttributes(any())).thenReturn(Map.of("department", "sales"));

        mockMvc.perform(get("/auth/me")
                        .principal(() -> "kc-1")
                        .header("X-API-KEY", "dev-default-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalUserId").value("ext-1"))
                .andExpect(jsonPath("$.attributes.department").value("sales"));
    }

    @Test
    void serviceToken_ShouldReturnAccessToken() throws Exception {
        when(authApplicationService.issueServiceToken(eq("auth-client"), eq("auth-client-secret"), eq("service.read")))
                .thenReturn(TokenPair.builder()
                        .accessToken("service-access")
                        .expiresIn(300)
                        .build());

        ServiceTokenRequest request = new ServiceTokenRequest();
        request.setClientId("auth-client");
        request.setClientSecret("auth-client-secret");
        request.setScope("service.read");

        mockMvc.perform(post("/auth/service-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "dev-default-api-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("service-access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

        @Test
        void forgotPassword_ShouldReturnAccepted() throws Exception {
                mockMvc.perform(post("/auth/forgot-password")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("X-API-KEY", "dev-default-api-key")
                                                .content("{" +
                                                                "\"email\":\"user@mail.com\"" +
                                                                "}"))
                                .andExpect(status().isAccepted());
        }

        @Test
        void changePassword_ShouldReturnNoContent() throws Exception {
                mockMvc.perform(post("/auth/change-password")
                                                .principal(() -> "kc-1")
                                                .header("Authorization", "Bearer token-value")
                                                .header("X-API-KEY", "dev-default-api-key")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{" +
                                                                "\"currentPassword\":\"OldPass123!\"," +
                                                                "\"newPassword\":\"NewPass123!\"" +
                                                                "}"))
                                .andExpect(status().isNoContent());
        }
}
