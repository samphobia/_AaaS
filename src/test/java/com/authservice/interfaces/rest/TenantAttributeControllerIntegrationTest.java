package com.authservice.interfaces.rest;

import com.authservice.application.service.CustomAttributeApplicationService;
import com.authservice.application.service.IdentityProviderClient;
import com.authservice.domain.model.AttributeType;
import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.infrastructure.security.TenantResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantAttributeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TenantAttributeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomAttributeApplicationService customAttributeApplicationService;

    @MockBean
    private IdentityProviderClient identityProviderClient;

    @MockBean
    private TenantResolver tenantResolver;

    @Test
    void createDefinition_ShouldReturnCreated() throws Exception {
        UUID definitionId = UUID.randomUUID();
        when(customAttributeApplicationService.createDefinition(eq("department"), eq(AttributeType.STRING), eq(true)))
                .thenReturn(CustomAttributeDefinition.builder()
                        .id(definitionId)
                        .tenantId(UUID.randomUUID())
                        .name("department")
                        .type(AttributeType.STRING)
                        .required(true)
                        .build());

        String payload = "{\"name\":\"department\",\"type\":\"STRING\",\"required\":true}";

        mockMvc.perform(post("/tenants/attributes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(definitionId.toString()))
                .andExpect(jsonPath("$.name").value("department"))
                .andExpect(jsonPath("$.type").value("STRING"))
                .andExpect(jsonPath("$.required").value(true));
    }
}
