package com.github.arthurpaixao.ms.alertas.repositories;

import com.github.arthurpaixao.ms.alertas.entities.Acionamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcionamentoRepository extends JpaRepository<Acionamento, Long> {
}
