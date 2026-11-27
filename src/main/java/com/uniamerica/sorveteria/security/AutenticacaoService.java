package com.uniamerica.sorveteria.security;

import com.uniamerica.sorveteria.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AutenticacaoService implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;

  @Override
  public UserDetails loadUserByUsername(String login) {
    return usuarioRepository.findByLogin(login)
      .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
  }
}
