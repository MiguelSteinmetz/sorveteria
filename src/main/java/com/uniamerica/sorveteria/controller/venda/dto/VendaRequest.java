package com.uniamerica.sorveteria.controller.venda.dto;

import com.uniamerica.sorveteria.entity.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VendaRequest(

        @NotEmpty(message = "A venda deve possuir pelo menos um item")
        @Valid
        List<ItemVendaRequest> itens,

        @NotNull(message = "A forma de pagamento é obrigatória")
        FormaPagamento formaPagamento

) {
}
