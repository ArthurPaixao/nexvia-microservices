package com.github.arthurpaixao.ms.alertas.service;

import com.github.arthurpaixao.ms.alertas.client.PessoaApoioClient;
import com.github.arthurpaixao.ms.alertas.dto.AcionamentoRequestDTO;
import com.github.arthurpaixao.ms.alertas.dto.AcionamentoResponseDTO;
import com.github.arthurpaixao.ms.alertas.entities.Acionamento;
import com.github.arthurpaixao.ms.alertas.entities.StatusAcionamento;
import com.github.arthurpaixao.ms.alertas.entities.TipoRisco;
import com.github.arthurpaixao.ms.alertas.exceptions.ComunicacaoException;
import com.github.arthurpaixao.ms.alertas.exceptions.DatabaseException;
import com.github.arthurpaixao.ms.alertas.exceptions.ResourceNotFoundException;
import com.github.arthurpaixao.ms.alertas.exceptions.StatusInvalidoException;
import com.github.arthurpaixao.ms.alertas.repositories.AcionamentoRepository;
import com.github.arthurpaixao.ms.alertas.repositories.TipoRiscoRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AcionamentoService {

    @Autowired
    private AcionamentoRepository acionamentoRepository;

    @Autowired
    private TipoRiscoRepository tipoRiscoRepository;

    @Autowired
    private PessoaApoioClient pessoaApoioClient;

    @Transactional(readOnly = true)
    public List<AcionamentoResponseDTO> findAllAcionamentos() {
        List<Acionamento> list = acionamentoRepository.findAll();
        return list.stream()
                .map(AcionamentoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcionamentoResponseDTO findAcionamentoById(Long id) {
        Acionamento acionamento = acionamentoRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado. ID: " + id)
        );
        return new AcionamentoResponseDTO(acionamento);
    }

    @Transactional
    public AcionamentoResponseDTO saveAcionamento(AcionamentoRequestDTO requestDTO) {
        // comunicacao sincrona: a pessoa precisa existir no ms-apoio
        validarPessoa(requestDTO.getPessoaId());

        Acionamento acionamento = new Acionamento();
        copyDtoToAcionamento(requestDTO, acionamento);
        // no cadastro o sistema define o status inicial e a data/hora
        acionamento.setStatus(StatusAcionamento.ABERTO);
        acionamento.setDataHoraAbertura(LocalDateTime.now());
        acionamento = acionamentoRepository.save(acionamento);
        return new AcionamentoResponseDTO(acionamento);
    }

    @Transactional
    public AcionamentoResponseDTO updateAcionamento(Long id, AcionamentoRequestDTO requestDTO) {
        try {
            Acionamento acionamento = acionamentoRepository.getReferenceById(id);

            if (!acionamento.getStatus().equals(StatusAcionamento.ABERTO)) {
                throw new StatusInvalidoException(
                        String.format("Acionamento id %d está %s e só pode ser alterado com status ABERTO",
                                id, acionamento.getStatus())
                );
            }

            if (!acionamento.getPessoaId().equals(requestDTO.getPessoaId())) {
                validarPessoa(requestDTO.getPessoaId());
            }

            copyDtoToAcionamento(requestDTO, acionamento);
            acionamento = acionamentoRepository.save(acionamento);
            return new AcionamentoResponseDTO(acionamento);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Recurso não encontrado. ID: " + id);
        }
    }

    @Transactional
    public void deleteAcionamento(Long id) {
        Acionamento acionamento = acionamentoRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado. ID: " + id)
        );
        if (acionamento.getStatus().equals(StatusAcionamento.PADRINHO_ACIONADO)) {
            throw new StatusInvalidoException(
                    String.format("Acionamento id %d está PADRINHO_ACIONADO (pessoa em alerta) "
                            + "e não pode ser excluído", id)
            );
        }
        acionamentoRepository.delete(acionamento);
    }

    /**
     * Aciona o padrinho: ABERTO (ou CONFIRMACAO_PENDENTE) -> PADRINHO_ACIONADO
     * e, via OpenFeign, coloca a pessoa EM_ALERTA no ms-apoio.
     * Se o ms-apoio falhar, a excecao desfaz a transacao e o Circuit Breaker
     * da controller chama o fallback.
     */
    @Transactional
    public AcionamentoResponseDTO acionarPadrinho(Long id) {
        Acionamento acionamento = acionamentoRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado. ID: " + id)
        );

        if (!acionamento.getStatus().equals(StatusAcionamento.ABERTO)
                && !acionamento.getStatus().equals(StatusAcionamento.CONFIRMACAO_PENDENTE)) {
            throw new StatusInvalidoException(
                    String.format("Acionamento id %d está %s. Só é possível acionar o padrinho "
                            + "com status ABERTO ou CONFIRMACAO_PENDENTE", id, acionamento.getStatus())
            );
        }

        acionamento.setStatus(StatusAcionamento.PADRINHO_ACIONADO);
        acionamentoRepository.save(acionamento);

        try {
            pessoaApoioClient.alertarPessoa(acionamento.getPessoaId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Pessoa não encontrada no ms-apoio. ID: "
                    + acionamento.getPessoaId());
        } catch (FeignException.Conflict e) {
            throw new StatusInvalidoException("Pessoa id " + acionamento.getPessoaId()
                    + " já está EM_ALERTA no ms-apoio");
        } catch (FeignException e) {
            throw new ComunicacaoException("Falha ao comunicar com ms-apoio", e);
        }

        return new AcionamentoResponseDTO(acionamento);
    }

    /**
     * Padrinho confirma "estou seguro": PADRINHO_ACIONADO -> CONCLUIDO
     * e, via OpenFeign, marca a pessoa como SEGURA no ms-apoio.
     */
    @Transactional
    public AcionamentoResponseDTO concluirAcionamento(Long id) {
        Acionamento acionamento = acionamentoRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado. ID: " + id)
        );

        if (!acionamento.getStatus().equals(StatusAcionamento.PADRINHO_ACIONADO)) {
            throw new StatusInvalidoException(
                    String.format("Acionamento id %d está %s. Só é possível concluir "
                            + "com status PADRINHO_ACIONADO", id, acionamento.getStatus())
            );
        }

        acionamento.setStatus(StatusAcionamento.CONCLUIDO);
        acionamentoRepository.save(acionamento);

        try {
            pessoaApoioClient.marcarPessoaSegura(acionamento.getPessoaId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Pessoa não encontrada no ms-apoio. ID: "
                    + acionamento.getPessoaId());
        } catch (FeignException.Conflict e) {
            throw new StatusInvalidoException("Pessoa id " + acionamento.getPessoaId()
                    + " não está EM_ALERTA no ms-apoio");
        } catch (FeignException e) {
            throw new ComunicacaoException("Falha ao comunicar com ms-apoio", e);
        }

        return new AcionamentoResponseDTO(acionamento);
    }

    /**
     * Usado pelo fallback do Circuit Breaker: o ms-apoio nao respondeu,
     * entao o acionamento fica CONFIRMACAO_PENDENTE para ser reenviado
     * (escalonamento para o coordenador).
     */
    @Transactional
    public AcionamentoResponseDTO marcarConfirmacaoPendente(Long id) {
        Acionamento acionamento = acionamentoRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não encontrado. ID: " + id)
        );
        acionamento.setStatus(StatusAcionamento.CONFIRMACAO_PENDENTE);
        acionamento = acionamentoRepository.save(acionamento);
        return new AcionamentoResponseDTO(acionamento);
    }

    private void validarPessoa(Long pessoaId) {
        try {
            pessoaApoioClient.buscarPessoa(pessoaId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Pessoa não encontrada no ms-apoio. ID: " + pessoaId);
        } catch (FeignException e) {
            throw new ComunicacaoException("Falha ao comunicar com ms-apoio", e);
        }
    }

    private void copyDtoToAcionamento(AcionamentoRequestDTO requestDTO, Acionamento acionamento) {
        acionamento.setPessoaId(requestDTO.getPessoaId());
        acionamento.setRua(requestDTO.getRua());
        acionamento.setGravidade(requestDTO.getGravidade());
        acionamento.setDescricao(requestDTO.getDescricao());

        TipoRisco tipoRisco = tipoRiscoRepository.findById(requestDTO.getTipoRiscoId()).orElseThrow(
                () -> new DatabaseException("Não foi possível salvar o Acionamento. Tipo de risco inexistente. "
                        + "(ID: " + requestDTO.getTipoRiscoId() + ")")
        );
        acionamento.setTipoRisco(tipoRisco);
    }
}
