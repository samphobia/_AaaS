package com.authservice.application.usecase;

import com.authservice.application.service.model.TokenPair;
import com.authservice.application.usecase.command.RefreshTokenCommand;

public interface RefreshTokenUseCase {

    TokenPair refresh(RefreshTokenCommand command);
}
