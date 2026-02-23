package io.github.joaosimsic.infrastructure.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
    @NotBlank String serverUrl,
    @NotBlank String realm,
    @NotBlank String clientId,
    @Valid @NotNull Admin admin) {

  public record Admin(@NotBlank String username, @NotBlank String password) {}
}
