package com.authservice.application.usecase;

import com.authservice.application.service.model.TokenPair;
import com.authservice.application.usecase.command.LoginCommand;

public interface LoginUseCase {

    TokenPair login(LoginCommand command);
}
