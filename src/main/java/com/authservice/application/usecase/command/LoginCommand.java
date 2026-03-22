package com.authservice.application.usecase.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginCommand {
    String username;
    String password;
}
