package com.uniamerica.sorveteria.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;


@ConfigurationProperties(prefix = "api.security.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {

  private static final int TAMANHO_MINIMO_SECRET = 32;

  public JwtProperties {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
        "JWT_SECRET nao configurado. Defina a variavel de ambiente JWT_SECRET (veja .env.example).");
    }
    if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SECRET) {
      throw new IllegalStateException(
        "JWT_SECRET deve ter pelo menos " + TAMANHO_MINIMO_SECRET + " caracteres.");
    }
    if (expiration == null || expiration.isNegative() || expiration.isZero()) {
      expiration = Duration.ofHours(2);
    }
    if (issuer == null || issuer.isBlank()) {
      issuer = "api-sorveteria";
    }
  }
}
