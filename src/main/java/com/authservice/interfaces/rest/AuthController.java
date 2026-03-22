package com.authservice.interfaces.rest;

import com.authservice.application.service.AuthApplicationService;
import com.authservice.application.exception.UnauthorizedException;
import com.authservice.application.usecase.command.LoginCommand;
import com.authservice.application.usecase.command.RefreshTokenCommand;
import com.authservice.application.usecase.command.RegisterUserCommand;
import com.authservice.infrastructure.security.TenantResolver;
import com.authservice.interfaces.dto.request.LoginRequest;
import com.authservice.interfaces.dto.request.RefreshTokenRequest;
import com.authservice.interfaces.dto.request.RegisterRequest;
import com.authservice.interfaces.dto.request.ServiceTokenRequest;
import com.authservice.interfaces.dto.response.CurrentUserResponse;
import com.authservice.interfaces.dto.response.LoginResponse;
import com.authservice.interfaces.dto.response.RegisterResponse;
import com.authservice.interfaces.dto.response.ServicePrincipalResponse;
import com.authservice.interfaces.dto.response.ServiceTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthApplicationService authApplicationService;
        private final TenantResolver tenantResolver;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register user", description = "Creates a Keycloak user and stores tenant-scoped external mapping")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(value = "{\"email\":\"john.doe@acme.com\",\"password\":\"StrongPass123!\",\"externalUserId\":\"crm-10001\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered", content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid X-API-KEY"),
            @ApiResponse(responseCode = "409", description = "externalUserId already exists in tenant")
    })
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request,
                                     @Parameter(description = "Tenant API key", example = "dev-default-api-key")
                                     @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        String resolvedApiKey = tenantResolver.resolve(apiKey).getApiKey();
        var authUser = authApplicationService.register(RegisterUserCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .externalUserId(request.getExternalUserId())
                .apiKey(resolvedApiKey)
                .build());

        return RegisterResponse.builder()
                .userId(authUser.getId())
                .keycloakUserId(authUser.getKeycloakUserId())
                .externalUserId(authUser.getExternalUserId())
                .tenantId(authUser.getTenantId())
                .status("REGISTERED")
                .build();
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Returns access and refresh token from Keycloak")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(value = "{\"username\":\"john.doe@acme.com\",\"password\":\"StrongPass123!\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var token = authApplicationService.login(LoginCommand.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build());

        return LoginResponse.builder()
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .expiresIn(token.getExpiresIn())
                .build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refreshes access token using refresh token")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(value = "{\"refreshToken\":\"eyJhbGciOi...\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var token = authApplicationService.refresh(RefreshTokenCommand.builder()
                .refreshToken(request.getRefreshToken())
                .build());

        return LoginResponse.builder()
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .expiresIn(token.getExpiresIn())
                .build();
    }

    @PostMapping("/service-token")
    @Operation(summary = "Issue service token", description = "Issues machine token using OAuth2 client credentials flow")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(value = "{\"clientId\":\"orders-service\",\"clientSecret\":\"orders-secret\",\"scope\":\"service.read\"}"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued", content = @Content(schema = @Schema(implementation = ServiceTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid service client credentials")
    })
    public ServiceTokenResponse serviceToken(@Valid @RequestBody ServiceTokenRequest request) {
        var token = authApplicationService.issueServiceToken(request.getClientId(), request.getClientSecret(), request.getScope());
        return ServiceTokenResponse.builder()
                .accessToken(token.getAccessToken())
                .expiresIn(token.getExpiresIn())
                .tokenType("Bearer")
                .build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "apiKeyAuth")
    @Operation(summary = "Current user", description = "Returns tenant-scoped user mapping for authenticated token subject")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = CurrentUserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid JWT or missing API key"),
            @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
        public CurrentUserResponse me(Authentication authentication) {
                Authentication auth = authentication;
                if (auth == null) {
                        auth = SecurityContextHolder.getContext().getAuthentication();
                }
                if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
                        throw new UnauthorizedException("Missing authentication principal");
                }

                var user = authApplicationService.getCurrentUser(auth.getName());
        return CurrentUserResponse.builder()
                .externalUserId(user.getExternalUserId())
                .tenantId(user.getTenantId())
                .roles(user.getRoles())
                .build();
    }

    @GetMapping("/service/me")
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "apiKeyAuth")
    @Operation(summary = "Current service principal", description = "Returns authenticated machine principal and granted scopes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Machine token valid", content = @Content(schema = @Schema(implementation = ServicePrincipalResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid bearer token or missing API key"),
            @ApiResponse(responseCode = "403", description = "Missing required scope")
    })
    public ServicePrincipalResponse serviceMe(Authentication authentication) {
        Authentication auth = authentication;
        if (auth == null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }

        if (auth == null) {
            throw new UnauthorizedException("Missing authentication");
        }

        var scopes = auth.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .filter(authority -> authority.startsWith("SCOPE_"))
                .map(authority -> authority.substring("SCOPE_".length()))
                .collect(java.util.stream.Collectors.toSet());

        return ServicePrincipalResponse.builder()
                .principal(auth.getName())
                .scopes(scopes)
                .build();
    }
}
