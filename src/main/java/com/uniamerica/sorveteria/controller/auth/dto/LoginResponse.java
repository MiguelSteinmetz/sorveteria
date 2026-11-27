package com.uniamerica.sorveteria.controller.auth.dto;

import com.uniamerica.sorveteria.entity.Cargo;

import java.time.Instant;

public record LoginResponse(
  String token,
  String tipo,
  Instant expiraEm,
  String nome,
  Cargo cargo
) {
}
