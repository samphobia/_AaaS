package com.authservice.interfaces.rest;

import com.authservice.application.service.CustomAttributeApplicationService;
import com.authservice.interfaces.dto.request.AssignUserAttributesRequest;
import com.authservice.interfaces.dto.response.UserAttributesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Attributes")
public class UserAttributeController {

    private final CustomAttributeApplicationService customAttributeApplicationService;

    @PostMapping("/{id}/attributes")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "apiKeyAuth")
    @Operation(summary = "Assign user attributes", description = "Assigns or updates tenant-defined attributes on a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attributes assigned"),
            @ApiResponse(responseCode = "400", description = "Invalid attribute values or missing required attributes"),
            @ApiResponse(responseCode = "404", description = "User or attribute definition not found")
    })
    public UserAttributesResponse assignAttributes(@PathVariable("id") UUID userId,
                                                   @Valid @RequestBody AssignUserAttributesRequest request) {
        var assigned = customAttributeApplicationService.assignUserAttributes(userId, request.getAttributes());
        return UserAttributesResponse.builder()
                .userId(userId)
                .attributes(assigned)
                .build();
    }
}
