package com.uniamerica.sorveteria.controller.auth;

import com.uniamerica.sorveteria.controller.auth.dto.CadastroRequest;
import com.uniamerica.sorveteria.controller.auth.dto.LoginRequest;
import com.uniamerica.sorveteria.controller.auth.dto.LoginResponse;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioRequest;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioResponse;
import com.uniamerica.sorveteria.entity.Cargo;
import com.uniamerica.sorveteria.service.AuthService;
import com.uniamerica.sorveteria.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final UsuarioService usuarioService;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/cadastro")
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest request) {
    UsuarioRequest usuario = new UsuarioRequest(
      request.nome(), request.login(), request.senha(), Cargo.FUNCIONARIO, request.cep());
    return UsuarioResponse.fromEntity(usuarioService.cadastrar(usuario));
  }
}
