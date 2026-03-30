package com.authservice.interfaces.dto.request;

import com.authservice.domain.model.AttributeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateTenantRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String apiKey;

    @Valid
    @NotNull
    private List<AttributeDefinition> attributes = List.of();

    @Data
    public static class AttributeDefinition {

        @NotBlank
        private String name;

        @NotNull
        private AttributeType type;

        private boolean required;
    }
}
