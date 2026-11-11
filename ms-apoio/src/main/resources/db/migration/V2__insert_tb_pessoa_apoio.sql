-- Dados ficticios para o simulado do bairro
INSERT INTO tb_pessoa_apoio (nome, endereco, bairro, necessidade, prioridade, nome_padrinho, telefone_padrinho, consentimento_lgpd, status_seguranca) VALUES
('Maria Aparecida dos Santos', 'Rua das Palmeiras, 120 - casa 2', 'Morro da Esperança', 'Acamada, 82 anos, usa concentrador de oxigênio', 1, 'João Pereira', '11987654321', TRUE, 'CADASTRADA'),
('Antônio Carlos Ribeiro', 'Travessa do Córrego, 45', 'Morro da Esperança', 'Cadeirante, 77 anos, mora sozinho', 1, 'Luciana Ribeiro', '11976543210', TRUE, 'CADASTRADA'),
('Ana Beatriz Souza', 'Rua das Palmeiras, 98', 'Morro da Esperança', 'Pessoa surda: aviso somente por SMS', 2, 'Carlos Mendes', '11965432109', TRUE, 'SEGURA'),
('Benedita Lima', 'Rua Beira-Rio, 310', 'Vila Ribeirinha', 'Idosa, 85 anos, mobilidade reduzida', 2, 'Patrícia Lima', '11954321098', TRUE, 'CADASTRADA'),
('Rafael Nogueira', 'Rua Beira-Rio, 52 - fundos', 'Vila Ribeirinha', 'Deficiência visual', 3, 'Marcos Nogueira', '11943210987', TRUE, 'CADASTRADA');
