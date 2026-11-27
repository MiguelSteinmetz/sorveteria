package com.uniamerica.sorveteria.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


final class RespostaErroSeguranca {

  private RespostaErroSeguranca() {
  }

  static void escrever(HttpServletResponse response, int status, String mensagem) throws IOException {
    response.setStatus(status);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write("{\"status\":" + status + ",\"mensagem\":\"" + mensagem + "\"}");
  }
}
