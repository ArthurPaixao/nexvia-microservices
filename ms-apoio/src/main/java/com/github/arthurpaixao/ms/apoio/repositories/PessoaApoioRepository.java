package com.github.arthurpaixao.ms.apoio.repositories;

import com.github.arthurpaixao.ms.apoio.entities.PessoaApoio;
import com.github.arthurpaixao.ms.apoio.entities.StatusSeguranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PessoaApoioRepository extends JpaRepository<PessoaApoio, Long> {

    // lista por prioridade: quem mais precisa de ajuda aparece primeiro
    List<PessoaApoio> findAllByOrderByPrioridadeAscNomeAsc();

    // painel "quem ainda falta": filtra pelo status de seguranca
    List<PessoaApoio> findByStatusSegurancaOrderByPrioridadeAscNomeAsc(StatusSeguranca statusSeguranca);
}
