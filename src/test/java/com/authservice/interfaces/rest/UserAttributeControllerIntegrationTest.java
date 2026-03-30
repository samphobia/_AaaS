package com.authservice.interfaces.rest;

import com.authservice.application.service.CustomAttributeApplicationService;
import com.authservice.application.service.IdentityProviderClient;
import com.authservice.infrastructure.security.TenantResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserAttributeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserAttributeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomAttributeApplicationService customAttributeApplicationService;

    @MockBean
    private IdentityProviderClient identityProviderClient;

    @MockBean
    private TenantResolver tenantResolver;

    @Test
    void assignAttributes_ShouldReturnAssignedValues() throws Exception {
        UUID userId = UUID.randomUUID();
        when(customAttributeApplicationService.assignUserAttributes(eq(userId), eq(Map.of("department", "sales", "active", true))))
                .thenReturn(Map.of("department", "sales", "active", true));

        String payload = "{\"attributes\":{\"department\":\"sales\",\"active\":true}}";

        mockMvc.perform(post("/users/{id}/attributes", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.attributes.department").value("sales"))
                .andExpect(jsonPath("$.attributes.active").value(true));
    }
}
