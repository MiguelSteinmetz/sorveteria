package com.uniamerica.sorveteria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

  @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "login", nullable = false, unique = true)
    private String login;


    @Column(name = "senha", nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo", nullable = false)
    private Cargo cargo;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

  @Column(name = "cep", length = 8)
  private String cep;


  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return cargo == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + cargo.name()));
  }

  @Override
  public String getPassword() {
    return senha;
  }

  @Override
  public String getUsername() {
    return login;
  }

  @Override
  public boolean isEnabled() {
    return Boolean.TRUE.equals(ativo);
  }
}
