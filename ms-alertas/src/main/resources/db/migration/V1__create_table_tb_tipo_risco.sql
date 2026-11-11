CREATE TABLE IF NOT EXISTS tb_tipo_risco (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(50) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    CONSTRAINT pk_tb_tipo_risco PRIMARY KEY (id)
);
