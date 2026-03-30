package com.authservice.application.usecase.command;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class RegisterUserCommand {
    String email;
    String password;
    String externalUserId;
    String apiKey;
    Map<String, Object> attributes;
}
