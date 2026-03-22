package com.authservice.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServiceTokenRequest {

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    private String scope;
}
