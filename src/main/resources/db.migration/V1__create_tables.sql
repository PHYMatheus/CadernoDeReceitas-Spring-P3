-- Criação da tabela principal de receitas
CREATE TABLE receita_culinaria (
                                   id SERIAL PRIMARY KEY,
                                   nome VARCHAR(255) NOT NULL,
                                   duracao_do_preparo_minutos REAL NOT NULL,
                                   tutorial_preparo TEXT NOT NULL
);

-- Criação da tabela auxiliar para armazenar a lista de ingredientes (mapeamento padrão do JPA)
CREATE TABLE receita_culinaria_ingredientes (
                                                receita_culinaria_id BIGINT NOT NULL,
                                                ingredientes VARCHAR(255) NOT NULL,
                                                CONSTRAINT fk_receita_ingredientes
                                                    FOREIGN KEY (receita_culinaria_id)
                                                        REFERENCES receita_culinaria(id)
                                                        ON DELETE CASCADE
);