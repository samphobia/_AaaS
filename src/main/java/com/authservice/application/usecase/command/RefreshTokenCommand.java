package com.authservice.application.usecase.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefreshTokenCommand {
    String refreshToken;
}
