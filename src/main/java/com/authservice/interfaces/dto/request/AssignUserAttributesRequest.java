package com.authservice.interfaces.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class AssignUserAttributesRequest {

    @NotEmpty
    private Map<String, Object> attributes;
}
