package com.uniamerica.sorveteria.service;

import com.uniamerica.sorveteria.client.viacep.ViaCepClient;
import com.uniamerica.sorveteria.client.viacep.ViaCepResponse;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioAtualizacaoRequest;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioRequest;
import com.uniamerica.sorveteria.entity.Usuario;
import com.uniamerica.sorveteria.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.uniamerica.sorveteria.entity.Cargo;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ViaCepClient viaCepClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;


  private UsuarioRequest criarRequest() {
    return new UsuarioRequest(
      "Miguel",
      "miguel",
      "123456",
      Cargo.FUNCIONARIO,
      "85851000"
    );
  }
    private Usuario criarUsuarioComCep(String cep) {
        Usuario usuario = new Usuario();
        usuario.setNome("Miguel");
        usuario.setLogin("miguel");
        usuario.setSenha("123456");
        usuario.setCep(cep);
        return usuario;
    }

    // cadastra

    @Test
    @DisplayName("Deve cadastrar usuário quando o login ainda não existe")
    void deveCadastrarUsuarioComSucesso() {
        UsuarioRequest request = criarRequest();

        when(usuarioRepository.existsByLogin(request.login())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(passwordEncoder.encode("123456")).thenReturn("hash-bcrypt");

        Usuario resultado = usuarioService.cadastrar(request);

        assertNotNull(resultado);
        assertEquals("miguel", resultado.getLogin());
        assertEquals(Cargo.FUNCIONARIO, resultado.getCargo());
        assertEquals(true, resultado.getAtivo());
        assertEquals("85851000", resultado.getCep());
        assertEquals("hash-bcrypt", resultado.getSenha());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar CONFLICT quando o login já existe")
    void deveLancarConflitoQuandoLoginJaExiste() {
        UsuarioRequest request = criarRequest();

        when(usuarioRepository.existsByLogin(request.login())).thenReturn(true);

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.cadastrar(request));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName(" usuário tem CEP")
    void BuscarEnderecoComSucesso() {
        Usuario usuario = criarUsuarioComCep("85851000");
        ViaCepResponse enderecoEsperado = mock(ViaCepResponse.class);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(viaCepClient.buscarCep("85851000")).thenReturn(enderecoEsperado);

        ViaCepResponse resultado = usuarioService.buscarEndereco(1L);

        assertSame(enderecoEsperado, resultado);
        verify(viaCepClient).buscarCep("85851000");
    }

    @Test
    @DisplayName(" usuário não existe")
    void UsuarioNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.buscarEndereco(99L));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
        verify(viaCepClient, never()).buscarCep(anyString());
    }

    @Test
    @DisplayName("CEP e nulo")
    void CepNulo() {
        Usuario usuario = criarUsuarioComCep(null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.buscarEndereco(1L));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(viaCepClient, never()).buscarCep(anyString());
    }

    @Test
    @DisplayName("CEP está em branco")
    void CepEmBranco() {
        Usuario usuario = criarUsuarioComCep("   ");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.buscarEndereco(1L));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(viaCepClient, never()).buscarCep(anyString());
    }

    private Usuario criarUsuario(Long id, String login, Cargo cargo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Nome " + login);
        usuario.setLogin(login);
        usuario.setCargo(cargo);
        return usuario;
    }

    @Test
    @DisplayName("Deve atualizar nome e cargo do funcionário")
    void deveAtualizarUsuario() {
        Usuario usuario = criarUsuario(2L, "joao", Cargo.FUNCIONARIO);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario resultado = usuarioService.atualizar(2L, new UsuarioAtualizacaoRequest("João Souza", Cargo.GERENTE), "erik");

        assertEquals("João Souza", resultado.getNome());
        assertEquals(Cargo.GERENTE, resultado.getCargo());
    }

    @Test
    @DisplayName("Não deve deixar o usuário mudar o próprio cargo")
    void naoDeveMudarProprioCargo() {
        Usuario usuario = criarUsuario(1L, "erik", Cargo.GERENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> usuarioService.atualizar(1L, new UsuarioAtualizacaoRequest("Erik", Cargo.FUNCIONARIO), "erik"));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Não deve rebaixar o último gerente")
    void naoDeveRebaixarUltimoGerente() {
        Usuario usuario = criarUsuario(1L, "ana", Cargo.GERENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.countByCargo(Cargo.GERENTE)).thenReturn(1L);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> usuarioService.atualizar(1L, new UsuarioAtualizacaoRequest("Ana", Cargo.FUNCIONARIO), "erik"));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND ao atualizar usuário inexistente")
    void atualizarUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> usuarioService.atualizar(99L, new UsuarioAtualizacaoRequest("X", Cargo.FUNCIONARIO), "erik"));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    @DisplayName("Deve excluir funcionário")
    void deveExcluirUsuario() {
        Usuario usuario = criarUsuario(2L, "joao", Cargo.FUNCIONARIO);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        usuarioService.deletar(2L, "erik");

        verify(usuarioRepository).delete(usuario);
    }

    @Test
    @DisplayName("Não deve excluir o próprio usuário")
    void naoDeveExcluirProprioUsuario() {
        Usuario usuario = criarUsuario(1L, "erik", Cargo.GERENTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> usuarioService.deletar(1L, "erik"));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    @DisplayName("Não deve excluir o último gerente")
    void naoDeveExcluirUltimoGerente() {
        Usuario usuario = criarUsuario(3L, "ana", Cargo.GERENTE);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.countByCargo(Cargo.GERENTE)).thenReturn(1L);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> usuarioService.deletar(3L, "erik"));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }
}
