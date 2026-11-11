CREATE TABLE IF NOT EXISTS tb_pessoa_apoio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    endereco VARCHAR(150) NOT NULL,
    bairro VARCHAR(80) NOT NULL,
    necessidade VARCHAR(255) NOT NULL,
    prioridade INT NOT NULL,
    nome_padrinho VARCHAR(100) NOT NULL,
    telefone_padrinho VARCHAR(11) NOT NULL,
    consentimento_lgpd BOOLEAN NOT NULL,
    status_seguranca VARCHAR(20) NOT NULL,
    CONSTRAINT pk_tb_pessoa_apoio PRIMARY KEY (id)
);
