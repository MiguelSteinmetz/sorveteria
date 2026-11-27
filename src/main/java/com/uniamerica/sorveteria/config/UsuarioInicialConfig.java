package com.uniamerica.sorveteria.config;

import com.uniamerica.sorveteria.entity.*;
import com.uniamerica.sorveteria.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;


@Slf4j
@Configuration
public class UsuarioInicialConfig {

  @Bean
  public ApplicationRunner criarUsuarioInicial(UsuarioRepository usuarioRepository,
                                               PasswordEncoder passwordEncoder,
                                               @Value("${api.admin.login:}") String login,
                                               @Value("${api.admin.senha:}") String senha) {
    return args -> {
      if (usuarioRepository.count() > 0) {
        return;
      }
      if (login.isBlank() || senha.isBlank()) {
        log.warn("Nenhum usuario cadastrado. Defina ADMIN_LOGIN e ADMIN_PASSWORD para criar o primeiro GERENTE.");
        return;
      }

      Usuario admin = new Usuario();
      admin.setNome("Administrador");
      admin.setLogin(login);
      admin.setSenha(passwordEncoder.encode(senha));
      admin.setCargo(Cargo.GERENTE);
      admin.setAtivo(true);
      usuarioRepository.save(admin);

      log.info("Usuario GERENTE inicial '{}' criado", login);
    };
  }
}
