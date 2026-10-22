package com.uniamerica.sorveteria.service;

import com.uniamerica.sorveteria.controller.produto.venda.dto.VendaRequest;
import com.uniamerica.sorveteria.entity.Produto;
import com.uniamerica.sorveteria.entity.StatusVenda;
import com.uniamerica.sorveteria.entity.Venda;
import com.uniamerica.sorveteria.repository.ProdutoRepository;
import com.uniamerica.sorveteria.repository.VendaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private VendaService vendaService;

    @Test
    void whenRegistrarVendaValidaDeveSalvarVendaEDescontarEstoque() {
        Produto produto = criarProduto(1L, "Sorvete", 10, 12.5, true);

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> {
            Venda vendaSalva = invocation.getArgument(0);
            vendaSalva.setId(99L);
            return vendaSalva;
        });

        Venda venda = vendaService.registrar(new VendaRequest(1L, 3));

        assertNotNull(venda);
        assertEquals(99L, venda.getId());
        assertEquals(7, produto.getEstoque());
        assertEquals(12.5, venda.getValorUnitario());
        assertEquals(37.5, venda.getValorTotal());
        assertEquals(StatusVenda.CONCLUIDA, venda.getStatus());
        assertNotNull(venda.getDataHora());
    }

    @Test
    void whenRegistrarVendaComProdutoIndisponivelDeveLancarExcecao() {
        Produto produto = criarProduto(2L, "Milkshake", 5, 18.0, false);

        when(produtoRepository.findById(2L)).thenReturn(Optional.of(produto));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vendaService.registrar(new VendaRequest(2L, 1)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("não está disponível"));
    }

    @Test
    void whenRegistrarVendaComQuantidadeInvalidaDeveLancarExcecao() {
        Produto produto = criarProduto(3L, "Casquinha", 5, 10.0, true);

        when(produtoRepository.findById(3L)).thenReturn(Optional.of(produto));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vendaService.registrar(new VendaRequest(3L, 0)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("maior que zero"));
    }

    @Test
    void whenRegistrarVendaComEstoqueInsuficienteDeveLancarExcecao() {
        Produto produto = criarProduto(4L, "Açaí", 2, 15.0, true);

        when(produtoRepository.findById(4L)).thenReturn(Optional.of(produto));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vendaService.registrar(new VendaRequest(4L, 3)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Estoque insuficiente"));
    }

    @Test
    void whenBuscarPorIdComVendaExistenteDeveRetornarVenda() {
        Venda venda = new Venda();
        venda.setId(15L);
        venda.setProduto(criarProduto(5L, "Sorvete", 10, 20.0, true));
        venda.setQuantidade(2);
        venda.setValorUnitario(20.0);
        venda.setValorTotal(40.0);
        venda.setDataHora(LocalDateTime.now());
        venda.setStatus(StatusVenda.CONCLUIDA);

        when(vendaRepository.findById(15L)).thenReturn(Optional.of(venda));

        Venda resultado = vendaService.buscarPorId(15L);

        assertNotNull(resultado);
        assertEquals(15L, resultado.getId());
        assertEquals(StatusVenda.CONCLUIDA, resultado.getStatus());
    }

    @Test
    void whenCancelarVendaValidaDeveAtualizarStatusEEstoque() {
        Produto produto = criarProduto(6L, "Sorvete", 5, 9.0, true);
        Venda venda = new Venda();
        venda.setId(22L);
        venda.setProduto(produto);
        venda.setQuantidade(2);
        venda.setValorTotal(18.0);
        venda.setStatus(StatusVenda.CONCLUIDA);

        when(vendaRepository.findById(22L)).thenReturn(Optional.of(venda));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Venda resultado = vendaService.cancelar(22L);

        assertEquals(StatusVenda.CANCELADA, resultado.getStatus());
        assertEquals(7, produto.getEstoque());
        verify(produtoRepository).save(produto);
        verify(vendaRepository).save(venda);
    }

    @Test
    void whenCancelarVendaJaCanceladaDeveLancarExcecao() {
        Produto produto = criarProduto(7L, "Açaí", 3, 11.0, true);
        Venda venda = new Venda();
        venda.setId(50L);
        venda.setProduto(produto);
        venda.setQuantidade(1);
        venda.setStatus(StatusVenda.CANCELADA);

        when(vendaRepository.findById(50L)).thenReturn(Optional.of(venda));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vendaService.cancelar(50L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("já está cancelada"));
    }

    private Produto criarProduto(Long id, String nome, Integer estoque, Double preco, Boolean disponivel) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setCategoria(com.uniamerica.sorveteria.entity.Categoria.SORVETE);
        produto.setEstoque(estoque);
        produto.setPreco(preco);
        produto.setDisponivel(disponivel);
        return produto;
    }
}


        