package com.uniamerica.sorveteria.controller.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
  @NotBlank(message = "O nome é obrigatório")
  String nome,

  @NotBlank(message = "O login é obrigatório")
  @Size(min = 3, max = 50, message = "O login deve ter entre 3 e 50 caracteres")
  String login,

  @NotBlank(message = "A senha é obrigatória")
  @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
  String senha,

  @NotBlank(message = "O CEP é obrigatório")
  @Pattern(regexp = "\\d{8}", message = "O CEP deve conter 8 números")
  String cep
) {
}
