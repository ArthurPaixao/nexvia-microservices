-- pessoa_id referencia tb_pessoa_apoio do ms-apoio (banco independente, sem FK entre servicos)
INSERT INTO tb_acionamento (pessoa_id, tipo_risco_id, rua, gravidade, descricao, data_hora_abertura, status) VALUES
(1, 1, 'Rua das Palmeiras', 'ALTA', 'Três moradores relatam rachaduras na encosta atrás das casas', '2026-11-10 22:00:00', 'ABERTO'),
(4, 2, 'Rua Beira-Rio', 'MEDIA', 'Córrego transbordando na altura do número 300', '2026-11-10 22:05:00', 'ABERTO'),
(3, 2, 'Rua das Palmeiras', 'MEDIA', 'Simulado do bairro: alagamento na parte baixa da rua', '2026-10-18 09:00:00', 'CONCLUIDO');
