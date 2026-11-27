package com.uniamerica.sorveteria.service;

import com.uniamerica.sorveteria.controller.auth.dto.LoginRequest;
import com.uniamerica.sorveteria.controller.auth.dto.LoginResponse;
import com.uniamerica.sorveteria.entity.Usuario;
import com.uniamerica.sorveteria.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final TokenService tokenService;

  public LoginResponse login(LoginRequest request) {
    Authentication autenticacao;
    try {
      autenticacao = authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(request.login(), request.senha()));
    } catch (AuthenticationException e) {
      log.warn("Falha de login para o usuario {}", request.login());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login ou senha inválidos");
    }

    Usuario usuario = (Usuario) autenticacao.getPrincipal();
    TokenService.TokenGerado token = tokenService.gerarToken(usuario);
    log.info("Usuario {} autenticado", usuario.getLogin());

    return new LoginResponse(token.token(), "Bearer", token.expiraEm(), usuario.getNome(), usuario.getCargo());
  }
}
