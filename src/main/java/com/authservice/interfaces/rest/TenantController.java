package com.authservice.interfaces.rest;

import com.authservice.application.service.TenantOnboardingApplicationService;
import com.authservice.interfaces.dto.request.CreateTenantRequest;
import com.authservice.interfaces.dto.response.CreateTenantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants")
public class TenantController {

    private final TenantOnboardingApplicationService tenantOnboardingApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create tenant", description = "Creates tenant with initial user attribute schema so signup is immediately tenant-specific")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(value = "{\"name\":\"Tenant A\",\"apiKey\":\"tenant-a-key\",\"attributes\":[{\"name\":\"name\",\"type\":\"STRING\",\"required\":true},{\"name\":\"idcard\",\"type\":\"STRING\",\"required\":true}]}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tenant created and schema provisioned"),
            @ApiResponse(responseCode = "400", description = "Invalid tenant or schema payload"),
            @ApiResponse(responseCode = "409", description = "Tenant apiKey already exists")
    })
    public CreateTenantResponse createTenant(@Valid @RequestBody CreateTenantRequest request) {
        var result = tenantOnboardingApplicationService.createTenant(
                request.getName(),
                request.getApiKey(),
                request.getAttributes().stream()
                        .map(attribute -> TenantOnboardingApplicationService.AttributeDefinitionInput.builder()
                                .name(attribute.getName())
                                .type(attribute.getType())
                                .required(attribute.isRequired())
                                .build())
                        .toList()
        );

        return CreateTenantResponse.builder()
                .tenantId(result.getTenant().getId())
                .name(result.getTenant().getName())
                .apiKey(result.getTenant().getApiKey())
                .attributes(result.getAttributeDefinitions().stream()
                        .map(definition -> CreateTenantResponse.AttributeDefinition.builder()
                                .id(definition.getId())
                                .name(definition.getName())
                                .type(definition.getType())
                                .required(definition.isRequired())
                                .build())
                        .toList())
                .build();
    }
}
