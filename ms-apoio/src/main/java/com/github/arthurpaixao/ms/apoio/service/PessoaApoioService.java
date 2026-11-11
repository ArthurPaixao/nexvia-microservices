package com.github.arthurpaixao.ms.apoio.service;

import com.github.arthurpaixao.ms.apoio.dto.PessoaApoioRequestDTO;
import com.github.arthurpaixao.ms.apoio.dto.PessoaApoioResponseDTO;
import com.github.arthurpaixao.ms.apoio.entities.PessoaApoio;
import com.github.arthurpaixao.ms.apoio.entities.StatusSeguranca;
import com.github.arthurpaixao.ms.apoio.exceptions.PessoaEmAlertaException;
import com.github.arthurpaixao.ms.apoio.exceptions.ResourceNotFoundException;
import com.github.arthurpaixao.ms.apoio.exceptions.StatusInvalidoException;
import com.github.arthurpaixao.ms.apoio.repositories.PessoaApoioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PessoaApoioService {

    @Autowired
    private PessoaApoioRepository pessoaApoioRepository;

    @Transactional(readOnly = true)
    public List<PessoaApoioResponseDTO> findAllPessoas(StatusSeguranca statusSeguranca) {
        List<PessoaApoio> pessoas = (statusSeguranca == null)
                ? pessoaApoioRepository.findAllByOrderByPrioridadeAscNomeAsc()
                : pessoaApoioRepository.findByStatusSegurancaOrderByPrioridadeAscNomeAsc(statusSeguranca);
        return pessoas.stream()
                .map(PessoaApoioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaApoioResponseDTO findPessoaById(Long id) {
        PessoaApoio pessoa = pessoaApoioRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado. ID: " + id)
        );
        return new PessoaApoioResponseDTO(pessoa);
    }

    @Transactional
    public PessoaApoioResponseDTO savePessoa(PessoaApoioRequestDTO requestDTO) {
        PessoaApoio pessoa = new PessoaApoio();
        copyDtoToPessoa(requestDTO, pessoa);
        // no cadastro o sistema define o status inicial
        pessoa.setStatusSeguranca(StatusSeguranca.CADASTRADA);
        pessoa = pessoaApoioRepository.save(pessoa);
        return new PessoaApoioResponseDTO(pessoa);
    }

    @Transactional
    public PessoaApoioResponseDTO updatePessoa(Long id, PessoaApoioRequestDTO requestDTO) {
        try {
            PessoaApoio pessoa = pessoaApoioRepository.getReferenceById(id);

            if (pessoa.getStatusSeguranca().equals(StatusSeguranca.EM_ALERTA)) {
                throw new PessoaEmAlertaException(
                        String.format("Pessoa id %d está EM_ALERTA e não pode ser alterada", id)
                );
            }

            copyDtoToPessoa(requestDTO, pessoa);
            pessoa = pessoaApoioRepository.save(pessoa);
            return new PessoaApoioResponseDTO(pessoa);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Recurso não encontrado. ID: " + id);
        }
    }

    @Transactional
    public void deletePessoa(Long id) {
        PessoaApoio pessoa = pessoaApoioRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado. ID: " + id)
        );
        if (pessoa.getStatusSeguranca().equals(StatusSeguranca.EM_ALERTA)) {
            throw new PessoaEmAlertaException(
                    String.format("Pessoa id %d está EM_ALERTA e não pode ser excluída", id)
            );
        }
        pessoaApoioRepository.delete(pessoa);
    }

    /**
     * Chamado pelo ms-alertas (OpenFeign) quando o padrinho e acionado.
     * CADASTRADA ou SEGURA -> EM_ALERTA
     */
    @Transactional
    public PessoaApoioResponseDTO alertarPessoa(Long id) {
        PessoaApoio pessoa = pessoaApoioRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Pessoa não encontrada. ID: " + id)
        );
        if (pessoa.getStatusSeguranca().equals(StatusSeguranca.EM_ALERTA)) {
            throw new StatusInvalidoException(
                    String.format("Pessoa id %d já está EM_ALERTA", id)
            );
        }
        pessoa.setStatusSeguranca(StatusSeguranca.EM_ALERTA);
        pessoa = pessoaApoioRepository.save(pessoa);
        return new PessoaApoioResponseDTO(pessoa);
    }

    /**
     * Chamado pelo ms-alertas (OpenFeign) quando o padrinho confirma "estou seguro".
     * EM_ALERTA -> SEGURA
     */
    @Transactional
    public PessoaApoioResponseDTO marcarPessoaSegura(Long id) {
        PessoaApoio pessoa = pessoaApoioRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Pessoa não encontrada. ID: " + id)
        );
        if (!pessoa.getStatusSeguranca().equals(StatusSeguranca.EM_ALERTA)) {
            throw new StatusInvalidoException(
                    String.format("Pessoa id %d não está EM_ALERTA (status atual: %s)",
                            id, pessoa.getStatusSeguranca())
            );
        }
        pessoa.setStatusSeguranca(StatusSeguranca.SEGURA);
        pessoa = pessoaApoioRepository.save(pessoa);
        return new PessoaApoioResponseDTO(pessoa);
    }

    private void copyDtoToPessoa(PessoaApoioRequestDTO requestDTO, PessoaApoio pessoa) {
        pessoa.setNome(requestDTO.getNome());
        pessoa.setEndereco(requestDTO.getEndereco());
        pessoa.setBairro(requestDTO.getBairro());
        pessoa.setNecessidade(requestDTO.getNecessidade());
        pessoa.setPrioridade(requestDTO.getPrioridade());
        pessoa.setNomePadrinho(requestDTO.getNomePadrinho());
        pessoa.setTelefonePadrinho(requestDTO.getTelefonePadrinho());
        pessoa.setConsentimentoLgpd(requestDTO.getConsentimentoLgpd());
    }
}
