package com.uniamerica.sorveteria.service;

import com.uniamerica.sorveteria.client.viacep.ViaCepClient;
import com.uniamerica.sorveteria.client.viacep.ViaCepResponse;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioAtualizacaoRequest;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioRequest;
import com.uniamerica.sorveteria.entity.Cargo;
import com.uniamerica.sorveteria.entity.Usuario;
import com.uniamerica.sorveteria.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
   private final ViaCepClient viaCepClient;
    private final PasswordEncoder passwordEncoder;

    public Usuario cadastrar(UsuarioRequest request) {
      log.info("Inciando Cadastro de usuario");

        if (usuarioRepository.existsByLogin(request.login())) {
          log.warn("Erro ao cadastrar usuario.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuario com esse login");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setLogin(request.login());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setCargo(request.cargo());
        usuario.setCep(request.cep());
        usuario.setAtivo(true);

      log.info("Usuario {} Cadastrado com sucesso", usuario.getNome());

        return usuarioRepository.save(usuario);
    }

  public ViaCepResponse buscarEndereco(Long usuarioId) {

    log.info("Buscando endereço do usuário {}", usuarioId);

    Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() ->
      new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario não encontrado com id " + usuarioId));

    if (usuario.getCep() == null || usuario.getCep().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario não possui CEP cadastrado");
    }

    ViaCepResponse endereco = viaCepClient.buscarCep(usuario.getCep());

    log.info("Endereço encontrado para o usuário {}", usuarioId);

    return endereco;
  }

  public List<Usuario> listar() {
    return usuarioRepository.findAll(Sort.by("nome"));
  }

  @Transactional
  public Usuario atualizar(Long id, UsuarioAtualizacaoRequest request, String loginLogado) {
    Usuario usuario = buscarPorId(id);

    if (usuario.getCargo() != request.cargo()) {
      if (usuario.getLogin().equals(loginLogado)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode alterar o seu próprio cargo");
      }
      if (usuario.getCargo() == Cargo.GERENTE && usuarioRepository.countByCargo(Cargo.GERENTE) <= 1) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Precisa existir pelo menos um gerente");
      }
    }

    usuario.setNome(request.nome());
    usuario.setCargo(request.cargo());

    log.info("Usuario {} atualizado", id);

    return usuarioRepository.save(usuario);
  }

  @Transactional
  public void deletar(Long id, String loginLogado) {
    Usuario usuario = buscarPorId(id);

    if (usuario.getLogin().equals(loginLogado)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode excluir o seu próprio usuário");
    }
    if (usuario.getCargo() == Cargo.GERENTE && usuarioRepository.countByCargo(Cargo.GERENTE) <= 1) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Precisa existir pelo menos um gerente");
    }

    usuarioRepository.delete(usuario);

    log.info("Usuario {} excluído", id);
  }

  private Usuario buscarPorId(Long id) {
    return usuarioRepository.findById(id).orElseThrow(() ->
      new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario não encontrado com id " + id));
  }
}
