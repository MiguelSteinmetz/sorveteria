
package com.uniamerica.sorveteria.controller;

import com.uniamerica.sorveteria.client.viacep.ViaCepResponse;
import com.uniamerica.sorveteria.controller.usuario.UsuarioController;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioAtualizacaoRequest;
import com.uniamerica.sorveteria.controller.usuario.dto.UsuarioRequest;
import com.uniamerica.sorveteria.entity.Cargo;
import com.uniamerica.sorveteria.entity.Usuario;
import com.uniamerica.sorveteria.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Testa so o controller; as regras de seguranca sao testadas em SecurityConfigTest.
@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UsuarioService usuarioService;

  @Test
  void deveCadastrarUsuario() throws Exception {
    Usuario usuario = new Usuario();
    usuario.setId(1L);
    usuario.setNome("João da Silva");
    usuario.setLogin("joao");
    usuario.setCargo(Cargo.FUNCIONARIO);
    usuario.setAtivo(true);
    usuario.setCep("85851000");

    when(usuarioService.cadastrar(any(UsuarioRequest.class)))
      .thenReturn(usuario);

    UsuarioRequest request = new UsuarioRequest(
      "João da Silva",
      "joao",
      "Senha123",
      Cargo.FUNCIONARIO,
      "85851000"
    );

    mockMvc.perform(post("/api/usuarios")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value(1))
      .andExpect(jsonPath("$.nome").value("João da Silva"))
      .andExpect(jsonPath("$.login").value("joao"))
      .andExpect(jsonPath("$.ativo").value(true));
  }

  @Test
  void deveRetornarErroAoCadastrarUsuarioComDadosInvalidos() throws Exception {
    UsuarioRequest request = new UsuarioRequest(
      "",
      "",
      "",
      null,
      "123"
    );

    mockMvc.perform(post("/api/usuarios")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void deveBuscarEnderecoDoUsuario() throws Exception {
    ViaCepResponse endereco = new ViaCepResponse(
      "85851000",
      "Rua das Flores",
      "Centro",
      "Foz do Iguaçu",
      "PR",
      "45"
    );

    when(usuarioService.buscarEndereco(1L)).thenReturn(endereco);

    mockMvc.perform(get("/api/usuarios/1/endereco"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.cep").value("85851000"))
      .andExpect(jsonPath("$.logradouro").value("Rua das Flores"))
      .andExpect(jsonPath("$.uf").value("PR"));
  }

  @Test
  void deveListarUsuarios() throws Exception {
    Usuario usuario = new Usuario();
    usuario.setId(1L);
    usuario.setNome("João da Silva");
    usuario.setLogin("joao");
    usuario.setCargo(Cargo.FUNCIONARIO);

    when(usuarioService.listar()).thenReturn(List.of(usuario));

    mockMvc.perform(get("/api/usuarios"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].login").value("joao"));
  }

  @Test
  void deveAtualizarUsuario() throws Exception {
    Usuario usuario = new Usuario();
    usuario.setId(2L);
    usuario.setNome("João Souza");
    usuario.setLogin("joao");
    usuario.setCargo(Cargo.GERENTE);

    when(usuarioService.atualizar(eq(2L), any(UsuarioAtualizacaoRequest.class), eq("erik"))).thenReturn(usuario);

    mockMvc.perform(put("/api/usuarios/2")
        .principal(() -> "erik")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new UsuarioAtualizacaoRequest("João Souza", Cargo.GERENTE))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.nome").value("João Souza"))
      .andExpect(jsonPath("$.cargo").value("GERENTE"));
  }

  @Test
  void deveRetornar400AoAtualizarSemNome() throws Exception {
    mockMvc.perform(put("/api/usuarios/2")
        .principal(() -> "erik")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new UsuarioAtualizacaoRequest("", null))))
      .andExpect(status().isBadRequest());
  }

  @Test
  void deveExcluirUsuario() throws Exception {
    mockMvc.perform(delete("/api/usuarios/2").principal(() -> "erik"))
      .andExpect(status().isNoContent());

    verify(usuarioService).deletar(2L, "erik");
  }
}
