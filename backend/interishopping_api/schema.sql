-- InteriShopping - modelo da Etapa 2 (item f): usuario, cliente, produto, pedido, item_pedido.
-- Rode este script no banco fornecido (Beekeeper Studio, phpMyAdmin ou cliente MySQL). Não cria o banco: use o que a universidade deu.

CREATE TABLE IF NOT EXISTS usuario (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    telefone    VARCHAR(11)  NOT NULL UNIQUE,          -- só dígitos: DDD + 9 dígitos
    senha_hash  VARCHAR(255) NOT NULL,                 -- password_hash(), nunca a senha pura
    criado_em   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cliente (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT NOT NULL,
    nome        VARCHAR(100) NOT NULL,
    telefone    VARCHAR(11)  NOT NULL,
    UNIQUE KEY uq_cliente_usuario_tel (usuario_id, telefone),   -- v2: um telefone por vendedor
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS produto (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT NOT NULL,                          -- isolamento: cada vendedor só vê os seus
    nome        VARCHAR(150) NOT NULL,
    categoria   VARCHAR(60),
    preco       DECIMAL(10,2) NOT NULL CHECK (preco >= 0),
    estoque     INT NOT NULL DEFAULT 0 CHECK (estoque >= 0),
    ativo       TINYINT(1) NOT NULL DEFAULT 1,         -- exclusão lógica (ativo = 0)
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pedido (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT NOT NULL,
    cliente_id  INT NOT NULL,
    status      ENUM('pendente','em_producao','a_caminho','finalizado') NOT NULL DEFAULT 'pendente',
    entrega     DECIMAL(10,2) NOT NULL DEFAULT 0,
    pagamento   VARCHAR(30),
    desconto    DECIMAL(10,2) NOT NULL DEFAULT 0,
    total       DECIMAL(10,2) NOT NULL DEFAULT 0,
    criado_em   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pedido_usuario_data (usuario_id, criado_em),       -- v2: acelera painel e lista
    INDEX idx_pedido_usuario_status (usuario_id, status),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS item_pedido (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id       INT NOT NULL,
    produto_id      INT NOT NULL,
    quantidade      INT NOT NULL CHECK (quantidade > 0),
    preco_unitario  DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (pedido_id)  REFERENCES pedido(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

