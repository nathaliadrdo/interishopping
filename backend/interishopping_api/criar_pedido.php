<?php
// POST: usuario_id, cliente_nome, cliente_telefone, entrega, desconto, pagamento, itens
//   itens = JSON: [{"produto_id":1,"quantidade":2}, ...]
// Cria o pedido numa TRANSAÇÃO: cliente (se novo) + pedido + itens + baixa de estoque.
// O total é calculado AQUI, com os preços do banco (o app não manda o total, para não ser fraudável).
require __DIR__ . '/config.php';
exigir_post();

$uid  = usuario_id_post();
$nome = campo('cliente_nome');
$tel  = so_digitos(campo('cliente_telefone'));
$entrega  = campo('entrega')  === '' ? '0' : campo('entrega');
$desconto = campo('desconto') === '' ? '0' : campo('desconto');
$pagamento = campo('pagamento');
$itens = json_decode((string)($_POST['itens'] ?? '[]'), true);

if ($nome === '' || mb_strlen($nome) > 100 || !telefone_ok($tel)
    || !is_numeric($entrega) || (float)$entrega < 0
    || !is_numeric($desconto) || (float)$desconto < 0
    || !is_array($itens) || count($itens) === 0 || mb_strlen($pagamento) > 30) {
    json_out(['error' => 'Dados inválidos'], 400);
}

try {
    $db = pdo();
    $db->beginTransaction();

    // 1) Cliente: reaproveita pelo telefone, senão cria.
    $st = $db->prepare('SELECT id FROM cliente WHERE usuario_id = :u AND telefone = :t LIMIT 1');
    $st->execute(['u' => $uid, 't' => $tel]);
    $cliente = $st->fetch();
    if ($cliente) {
        $clienteId = (int)$cliente['id'];
    } else {
        $db->prepare('INSERT INTO cliente (usuario_id, nome, telefone) VALUES (:u, :n, :t)')
           ->execute(['u' => $uid, 'n' => $nome, 't' => $tel]);
        $clienteId = (int)$db->lastInsertId();
    }

    // 2) Itens: confere produto (do usuário, ativo), estoque e soma o subtotal com o preço do banco.
    $subtotal = 0.0;
    $linhas = [];
    foreach ($itens as $it) {
        $pid = (int)($it['produto_id'] ?? 0);
        $qtd = (int)($it['quantidade'] ?? 0);
        if ($pid <= 0 || $qtd <= 0) { $db->rollBack(); json_out(['error' => 'Item inválido'], 400); }
        $p = $db->prepare('SELECT preco, estoque FROM produto WHERE id = :id AND usuario_id = :u AND ativo = 1 FOR UPDATE');
        $p->execute(['id' => $pid, 'u' => $uid]);
        $prod = $p->fetch();
        if (!$prod) { $db->rollBack(); json_out(['error' => 'Produto não encontrado'], 400); }
        if ((int)$prod['estoque'] < $qtd) { $db->rollBack(); json_out(['error' => 'Estoque insuficiente'], 409); }
        $subtotal += (float)$prod['preco'] * $qtd;
        $linhas[] = ['pid' => $pid, 'qtd' => $qtd, 'preco' => $prod['preco']];
    }

    $total = max(0, $subtotal + (float)$entrega - (float)$desconto);

    // 3) Pedido + itens + baixa de estoque.
    $db->prepare('INSERT INTO pedido (usuario_id, cliente_id, status, entrega, pagamento, desconto, total)
                  VALUES (:u, :c, "pendente", :e, :pg, :d, :t)')
       ->execute(['u' => $uid, 'c' => $clienteId, 'e' => $entrega, 'pg' => $pagamento, 'd' => $desconto, 't' => $total]);
    $pedidoId = (int)$db->lastInsertId();

    $insItem = $db->prepare('INSERT INTO item_pedido (pedido_id, produto_id, quantidade, preco_unitario) VALUES (:p, :pr, :q, :pu)');
    $baixa = $db->prepare('UPDATE produto SET estoque = estoque - :q WHERE id = :id AND usuario_id = :u');
    foreach ($linhas as $l) {
        $insItem->execute(['p' => $pedidoId, 'pr' => $l['pid'], 'q' => $l['qtd'], 'pu' => $l['preco']]);
        $baixa->execute(['q' => $l['qtd'], 'id' => $l['pid'], 'u' => $uid]);
    }

    $db->commit();
    json_out(['status' => 'ok', 'id' => $pedidoId, 'total' => round($total, 2)], 201);
} catch (PDOException $e) {
    if (isset($db) && $db->inTransaction()) $db->rollBack();
    error_log('[InteriShopping] ' . $e->getMessage());
    json_out(['error' => 'Erro no servidor'], 500);
}
