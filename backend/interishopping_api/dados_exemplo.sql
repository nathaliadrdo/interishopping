SET @uid = 1;

INSERT INTO produto (usuario_id, nome, categoria, preco, estoque) VALUES
 (@uid, 'Brigadeiro',       'Doces',  1.50, 24),
 (@uid, 'Bolo de cenoura',  'Bolos', 70.00, 12),
 (@uid, 'Torta de limão',   'Tortas',58.00,  3),
 (@uid, 'Pão de mel',       'Doces',  6.00,  2);

INSERT INTO cliente (usuario_id, nome, telefone) VALUES
 (@uid, 'Carla Souza', '11988881111'),
 (@uid, 'Marcos Lima', '11977772222'),
 (@uid, 'Joana Reis',  '11966663333');

INSERT INTO pedido (usuario_id, cliente_id, status, entrega, pagamento, desconto, total)
 SELECT @uid, id, 'a_caminho', 8, 'Pix', 0, 93 FROM cliente WHERE usuario_id=@uid AND telefone='11988881111';
SET @p1 = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, produto_id, quantidade, preco_unitario)
 SELECT @p1, id, 1, 70.00 FROM produto WHERE usuario_id=@uid AND nome='Bolo de cenoura';
INSERT INTO item_pedido (pedido_id, produto_id, quantidade, preco_unitario)
 SELECT @p1, id, 10, 1.50 FROM produto WHERE usuario_id=@uid AND nome='Brigadeiro';

INSERT INTO pedido (usuario_id, cliente_id, status, entrega, pagamento, desconto, total, criado_em)
 SELECT @uid, id, 'pendente', 0, 'Dinheiro', 0, 58, DATE_SUB(NOW(), INTERVAL 1 DAY) FROM cliente WHERE usuario_id=@uid AND telefone='11977772222';
SET @p2 = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, produto_id, quantidade, preco_unitario)
 SELECT @p2, id, 1, 58.00 FROM produto WHERE usuario_id=@uid AND nome='Torta de limão';

INSERT INTO pedido (usuario_id, cliente_id, status, entrega, pagamento, desconto, total, criado_em)
 SELECT @uid, id, 'em_producao', 0, 'Pix', 0, 30, DATE_SUB(NOW(), INTERVAL 3 DAY) FROM cliente WHERE usuario_id=@uid AND telefone='11966663333';
SET @p3 = LAST_INSERT_ID();
INSERT INTO item_pedido (pedido_id, produto_id, quantidade, preco_unitario)
 SELECT @p3, id, 20, 1.50 FROM produto WHERE usuario_id=@uid AND nome='Brigadeiro';
