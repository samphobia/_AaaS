package com.authservice.interfaces.rest;

import com.authservice.application.service.TenantOnboardingApplicationService;
import com.authservice.application.service.model.TenantProvisioningResult;
import com.authservice.domain.model.AttributeType;
import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.domain.model.Tenant;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TenantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantOnboardingApplicationService tenantOnboardingApplicationService;

        @MockBean
        private IdentityProviderClient identityProviderClient;

        @MockBean
        private TenantResolver tenantResolver;

    @Test
    void createTenant_ShouldReturnCreatedTenantWithSchema() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID attrId = UUID.randomUUID();

        when(tenantOnboardingApplicationService.createTenant(eq("Tenant A"), eq("tenant-a-key"), anyList()))
                .thenReturn(TenantProvisioningResult.builder()
                        .tenant(Tenant.builder()
                                .id(tenantId)
                                .name("Tenant A")
                                .apiKey("tenant-a-key")
                                .build())
                        .attributeDefinitions(List.of(
                                CustomAttributeDefinition.builder()
                                        .id(attrId)
                                        .tenantId(tenantId)
                                        .name("idcard")
                                        .type(AttributeType.STRING)
                                        .required(true)
                                        .build()
                        ))
                        .build());

        String payload = """
                {
                  "name": "Tenant A",
                  "apiKey": "tenant-a-key",
                  "attributes": [
                    {"name": "idcard", "type": "STRING", "required": true}
                  ]
                }
                """;

        mockMvc.perform(post("/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.name").value("Tenant A"))
                .andExpect(jsonPath("$.apiKey").value("tenant-a-key"))
                .andExpect(jsonPath("$.attributes[0].name").value("idcard"))
                .andExpect(jsonPath("$.attributes[0].type").value("STRING"));
    }
}
