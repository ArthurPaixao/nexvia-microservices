CREATE TABLE IF NOT EXISTS tb_acionamento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pessoa_id BIGINT NOT NULL,
    tipo_risco_id BIGINT NOT NULL,
    rua VARCHAR(150) NOT NULL,
    gravidade VARCHAR(10) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    data_hora_abertura DATETIME NOT NULL,
    status VARCHAR(25) NOT NULL,
    CONSTRAINT pk_tb_acionamento PRIMARY KEY (id),
    CONSTRAINT fk_acionamento_tipo_risco
        FOREIGN KEY (tipo_risco_id)
        REFERENCES tb_tipo_risco (id)
);
