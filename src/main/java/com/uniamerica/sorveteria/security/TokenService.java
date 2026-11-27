package com.uniamerica.sorveteria.security;

import com.uniamerica.sorveteria.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

  public static final String CLAIM_CARGO = "cargo";
  public static final String CLAIM_NOME = "nome";

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;

  public TokenGerado gerarToken(Usuario usuario) {
    Instant agora = Instant.now();
    Instant expiraEm = agora.plus(properties.expiration());

    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer(properties.issuer())
      .issuedAt(agora)
      .expiresAt(expiraEm)
      .subject(usuario.getLogin())
      .claim(CLAIM_CARGO, usuario.getCargo().name())
      .claim(CLAIM_NOME, usuario.getNome())
      .build();

    JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();

    return new TokenGerado(token, expiraEm);
  }

  public record TokenGerado(String token, Instant expiraEm) {
  }
}
