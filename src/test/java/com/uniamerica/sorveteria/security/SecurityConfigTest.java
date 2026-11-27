package com.uniamerica.sorveteria.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.uniamerica.sorveteria.controller.auth.AuthController;
import com.uniamerica.sorveteria.controller.produto.ProdutoController;
import com.uniamerica.sorveteria.controller.relatorio.RelatorioController;
import com.uniamerica.sorveteria.controller.relatorio.dto.ResumoVendasResponse;
import com.uniamerica.sorveteria.entity.Cargo;
import com.uniamerica.sorveteria.entity.Usuario;
import com.uniamerica.sorveteria.repository.UsuarioRepository;
import com.uniamerica.sorveteria.service.AuthService;
import com.uniamerica.sorveteria.service.ProdutoService;
import com.uniamerica.sorveteria.service.RelatorioService;
import com.uniamerica.sorveteria.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, ProdutoController.class, RelatorioController.class})
@Import({SecurityConfig.class, JwtConfig.class, TokenService.class, AuthService.class,
  AutenticacaoService.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = {
  "api.security.jwt.expiration=30m",
  "api.security.jwt.issuer=api-sorveteria",
  "api.security.cors.allowed-origins=http://localhost:4200"
})
class SecurityConfigTest {

  @DynamicPropertySource
  static void chaveJwtAleatoria(DynamicPropertyRegistry registry) {
    registry.add("api.security.jwt.secret", SecurityConfigTest::chaveAleatoria);
  }

  private static String chaveAleatoria() {
    byte[] bytes = new byte[48];
    new SecureRandom().nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TokenService tokenService;

  @Autowired
  private JwtEncoder jwtEncoder;

  @Autowired
  private JwtDecoder jwtDecoder;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private UsuarioRepository usuarioRepository;

  @MockitoBean
  private ProdutoService produtoService;

  @MockitoBean
  private RelatorioService relatorioService;

  @MockitoBean
  private UsuarioService usuarioService;

  private Usuario usuario(String login, Cargo cargo, String senhaPura, boolean ativo) {
    Usuario usuario = new Usuario();
    usuario.setId(1L);
    usuario.setNome("Usuario " + login);
    usuario.setLogin(login);
    usuario.setSenha(passwordEncoder.encode(senhaPura));
    usuario.setCargo(cargo);
    usuario.setAtivo(ativo);
    return usuario;
  }

  private String bearer(Cargo cargo) {
    return "Bearer " + tokenService.gerarToken(usuario("teste", cargo, "x", true)).token();
  }

  @Test
  void loginValidoDeveRetornarTokenComExpiracao() throws Exception {
    when(usuarioRepository.findByLogin("maria"))
      .thenReturn(Optional.of(usuario("maria", Cargo.GERENTE, "senha123", true)));

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"login\":\"maria\",\"senha\":\"senha123\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.token").isNotEmpty())
      .andExpect(jsonPath("$.tipo").value("Bearer"))
      .andExpect(jsonPath("$.expiraEm").isNotEmpty())
      .andExpect(jsonPath("$.cargo").value("GERENTE"));
  }

  @Test
  void loginComSenhaErradaDeveRetornar401() throws Exception {
    when(usuarioRepository.findByLogin("maria"))
      .thenReturn(Optional.of(usuario("maria", Cargo.GERENTE, "senha123", true)));

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"login\":\"maria\",\"senha\":\"errada\"}"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.mensagem").value("Login ou senha inválidos"));
  }

  @Test
  void loginDeUsuarioInexistenteDeveRetornar401() throws Exception {
    when(usuarioRepository.findByLogin("ninguem")).thenReturn(Optional.empty());

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"login\":\"ninguem\",\"senha\":\"x\"}"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void loginDeUsuarioInativoDeveRetornar401() throws Exception {
    when(usuarioRepository.findByLogin("joao"))
      .thenReturn(Optional.of(usuario("joao", Cargo.FUNCIONARIO, "senha123", false)));

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"login\":\"joao\",\"senha\":\"senha123\"}"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void cadastroPublicoDeveCriarFuncionarioSemToken() throws Exception {
    Usuario criado = usuario("novo", Cargo.FUNCIONARIO, "senha123", true);
    criado.setCep("85851000");
    when(usuarioService.cadastrar(any())).thenReturn(criado);

    mockMvc.perform(post("/api/auth/cadastro")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nome\":\"Novo\",\"login\":\"novo\",\"senha\":\"senha123\",\"cep\":\"85851000\"}"))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.login").value("novo"))
      .andExpect(jsonPath("$.cargo").value("FUNCIONARIO"));
  }

  @Test
  void cadastroComDadosInvalidosDeveRetornar400() throws Exception {
    mockMvc.perform(post("/api/auth/cadastro")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nome\":\"\",\"login\":\"a\",\"senha\":\"123\",\"cep\":\"abc\"}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void loginSemCamposDeveRetornar400() throws Exception {
    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void tokenGeradoDeveTerExpiracaoConfiguradaECargo() {
    TokenService.TokenGerado gerado = tokenService.gerarToken(usuario("maria", Cargo.GERENTE, "x", true));
    Jwt jwt = jwtDecoder.decode(gerado.token());

    assertThat(jwt.getSubject()).isEqualTo("maria");
    assertThat(jwt.getClaimAsString(TokenService.CLAIM_CARGO)).isEqualTo("GERENTE");
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void requisicaoSemTokenDeveRetornar401() throws Exception {
    mockMvc.perform(get("/api/produtos"))
      .andExpect(status().isUnauthorized())
      .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
      .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void tokenMalFormadoDeveRetornar401() throws Exception {
    mockMvc.perform(get("/api/produtos").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenAssinadoComOutraChaveDeveRetornar401() throws Exception {
    JwtEncoder outroEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
      chaveAleatoria().getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
    String token = criarToken(outroEncoder, Instant.now().plusSeconds(600));

    mockMvc.perform(get("/api/produtos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenExpiradoDeveRetornar401() throws Exception {
    String token = criarToken(jwtEncoder, Instant.now().minus(Duration.ofMinutes(10)));

    mockMvc.perform(get("/api/produtos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenValidoDeveAcessarRotaProtegida() throws Exception {
    when(produtoService.listar()).thenReturn(List.of());

    mockMvc.perform(get("/api/produtos").header(HttpHeaders.AUTHORIZATION, bearer(Cargo.FUNCIONARIO)))
      .andExpect(status().isOk());
  }

  @Test
  void funcionarioNaoPodeVerRelatoriosDeveRetornar403() throws Exception {
    mockMvc.perform(get("/api/relatorios/vendas/resumo")
        .header(HttpHeaders.AUTHORIZATION, bearer(Cargo.FUNCIONARIO)))
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void funcionarioNaoPodeExcluirProdutoDeveRetornar403() throws Exception {
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/produtos/1")
        .header(HttpHeaders.AUTHORIZATION, bearer(Cargo.FUNCIONARIO)))
      .andExpect(status().isForbidden());
  }

  @Test
  void gerentePodeVerRelatorios() throws Exception {
    when(relatorioService.gerarResumoMensal(anyInt(), anyInt()))
      .thenReturn(new ResumoVendasResponse(0.0, 0L, 0.0, 0L));

    mockMvc.perform(get("/api/relatorios/vendas/resumo")
        .header(HttpHeaders.AUTHORIZATION, bearer(Cargo.GERENTE)))
      .andExpect(status().isOk());
  }

  @Test
  void preflightCorsDoAngularDeveSerPermitido() throws Exception {
    mockMvc.perform(options("/api/produtos")
        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
      .andExpect(status().isOk())
      .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
  }

  private String criarToken(JwtEncoder encoder, Instant expiraEm) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer("api-sorveteria")
      .issuedAt(expiraEm.minus(Duration.ofMinutes(30)))
      .expiresAt(expiraEm)
      .subject("teste")
      .claim(TokenService.CLAIM_CARGO, "GERENTE")
      .build();
    return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
  }
}
