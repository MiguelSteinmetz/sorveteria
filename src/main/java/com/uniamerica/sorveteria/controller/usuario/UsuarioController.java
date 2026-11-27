package com.uniamerica.sorveteria.controller.usuario;

import com.uniamerica.sorveteria.client.viacep.ViaCepResponse;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioAtualizacaoRequest;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioRequest;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioResponse;
import com.uniamerica.sorveteria.entity.Usuario;
import com.uniamerica.sorveteria.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@Valid @RequestBody UsuarioRequest request) {
        Usuario usuario = usuarioService.cadastrar(request);
        log.info("Usuario {} cadastrado com sucesso", usuario.getNome());
        return UsuarioResponse.fromEntity(usuario);
    }

  @GetMapping("/{id}/endereco")
  public ViaCepResponse buscarEndereco(@PathVariable Long id) {

    log.info("Recebida requisição para consultar endereço do usuário {}", id);

    return usuarioService.buscarEndereco(id);
  }

  @GetMapping
  public List<UsuarioResponse> listar() {
    return usuarioService.listar().stream().map(UsuarioResponse::fromEntity).toList();
  }

  @PutMapping("/{id}")
  public UsuarioResponse atualizar(@PathVariable Long id,
                                   @Valid @RequestBody UsuarioAtualizacaoRequest request,
                                   Principal principal) {
    return UsuarioResponse.fromEntity(usuarioService.atualizar(id, request, principal.getName()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletar(@PathVariable Long id, Principal principal) {
    usuarioService.deletar(id, principal.getName());
  }
}
