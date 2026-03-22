package com.authservice.application.usecase;

import com.authservice.application.usecase.command.RegisterUserCommand;
import com.authservice.domain.model.AuthUser;

public interface RegisterUserUseCase {

    AuthUser register(RegisterUserCommand command);
}
