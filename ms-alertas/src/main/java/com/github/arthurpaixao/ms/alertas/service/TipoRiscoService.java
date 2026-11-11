package com.github.arthurpaixao.ms.alertas.service;

import com.github.arthurpaixao.ms.alertas.dto.TipoRiscoResponseDTO;
import com.github.arthurpaixao.ms.alertas.repositories.TipoRiscoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TipoRiscoService {

    @Autowired
    private TipoRiscoRepository tipoRiscoRepository;

    @Transactional(readOnly = true)
    public List<TipoRiscoResponseDTO> findAllTiposRisco() {
        return tipoRiscoRepository.findAll().stream()
                .map(TipoRiscoResponseDTO::new)
                .toList();
    }
}
