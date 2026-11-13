<?php
// GET: usuario_id, id  ->  detalhe de UM pedido (cabeçalho + itens). Só do próprio vendedor.
require __DIR__ . '/config.php';

$uid = usuario_id_get();
$id = (string)($_GET['id'] ?? '');
if (!ctype_digit($id) || (int)$id <= 0) json_out(['error' => 'Pedido inválido'], 400);

com_erro_tratado(function () use ($uid, $id) {
    $st = pdo()->prepare(
        'SELECT p.id, p.status, p.entrega, p.pagamento, p.desconto, p.total, p.criado_em,
                c.nome AS cliente_nome, c.telefone AS cliente_telefone
           FROM pedido p JOIN cliente c ON c.id = p.cliente_id
          WHERE p.id = :id AND p.usuario_id = :u');
    $st->execute(['id' => (int)$id, 'u' => $uid]);
    $pedido = $st->fetch();
    if (!$pedido) json_out(['error' => 'Pedido não encontrado'], 404);

    $it = pdo()->prepare(
        'SELECT pr.nome, i.quantidade, i.preco_unitario
           FROM item_pedido i JOIN produto pr ON pr.id = i.produto_id
          WHERE i.pedido_id = :id ORDER BY i.id');
    $it->execute(['id' => (int)$id]);
    $itens = $it->fetchAll();
    foreach ($itens as &$i) $i['quantidade'] = (int)$i['quantidade'];

    $pedido['id'] = (int)$pedido['id'];
    $pedido['itens'] = $itens;
    json_out($pedido);
});
