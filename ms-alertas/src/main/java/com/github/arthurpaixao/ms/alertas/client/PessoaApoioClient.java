package com.github.arthurpaixao.ms.alertas.client;

import com.github.arthurpaixao.ms.alertas.dto.PessoaApoioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Cliente HTTP declarativo para o ms-apoio.
 * Sem URL fixa: o nome "ms-apoio" e resolvido pelo Eureka
 * e as chamadas sao distribuidas pelo Spring Cloud LoadBalancer.
 */
@FeignClient("ms-apoio")
public interface PessoaApoioClient {

    @RequestMapping(method = RequestMethod.GET,
            value = "/api/v1/pessoas/{pessoaId}")
    PessoaApoioDTO buscarPessoa(@PathVariable Long pessoaId);

    @RequestMapping(method = RequestMethod.PUT,
            value = "/api/v1/pessoas/{pessoaId}/alertar") // endpoint da requisicao
    void alertarPessoa(@PathVariable Long pessoaId);

    @RequestMapping(method = RequestMethod.PUT,
            value = "/api/v1/pessoas/{pessoaId}/segura") // endpoint da requisicao
    void marcarPessoaSegura(@PathVariable Long pessoaId);
}
