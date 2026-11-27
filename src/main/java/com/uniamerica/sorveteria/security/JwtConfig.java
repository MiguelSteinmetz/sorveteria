package com.uniamerica.sorveteria.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

  private final SecretKey chave;
  private final JwtProperties properties;

  public JwtConfig(JwtProperties properties) {
    this.properties = properties;
    this.chave = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  public JwtEncoder jwtEncoder() {
    return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave)
      .macAlgorithm(MacAlgorithm.HS256)
      .build();

    // Valida assinatura, expiracao (exp), emissor (iss) e exige a claim exp.
    OAuth2TokenValidator<Jwt> validador = new DelegatingOAuth2TokenValidator<>(
      JwtValidators.createDefaultWithIssuer(properties.issuer()),
      new JwtClaimValidator<Object>(JwtClaimNames.EXP, Objects::nonNull)
    );
    decoder.setJwtValidator(validador);
    return decoder;
  }
}
