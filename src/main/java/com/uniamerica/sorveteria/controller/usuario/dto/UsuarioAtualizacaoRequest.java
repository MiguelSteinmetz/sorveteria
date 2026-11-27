package com.uniamerica.sorveteria.controller.usuario.dto;

import com.uniamerica.sorveteria.entity.Cargo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioAtualizacaoRequest(

  @NotBlank(message = "O nome é obrigatório")
  @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
  String nome,

  @NotNull(message = "O cargo é obrigatório")
  Cargo cargo

) {
}
