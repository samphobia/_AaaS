package com.authservice.interfaces.rest;

import com.authservice.application.service.CustomAttributeApplicationService;
import com.authservice.interfaces.dto.request.CreateAttributeDefinitionRequest;
import com.authservice.interfaces.dto.response.AttributeDefinitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/tenants/attributes")
@RequiredArgsConstructor
@Tag(name = "Tenant Attributes")
public class TenantAttributeController {

    private final CustomAttributeApplicationService customAttributeApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "apiKeyAuth")
    @Operation(summary = "Create attribute definition", description = "Creates a tenant-scoped custom attribute definition")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Definition created"),
            @ApiResponse(responseCode = "400", description = "Invalid definition payload"),
            @ApiResponse(responseCode = "409", description = "Definition name already exists in tenant")
    })
    public AttributeDefinitionResponse createDefinition(@Valid @RequestBody CreateAttributeDefinitionRequest request) {
        var definition = customAttributeApplicationService.createDefinition(
                request.getName(),
                request.getType(),
                request.isRequired()
        );
        return AttributeDefinitionResponse.builder()
                .id(definition.getId())
                .name(definition.getName())
                .type(definition.getType())
                .required(definition.isRequired())
                .build();
    }
}
