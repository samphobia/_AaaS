package com.authservice.interfaces.dto.request;

import com.authservice.domain.model.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAttributeDefinitionRequest {

    @NotBlank
    private String name;

    @NotNull
    private AttributeType type;

    private boolean required;
}
