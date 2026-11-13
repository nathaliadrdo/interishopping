<?php
require __DIR__ . '/config.php';

$uid = usuario_id_get();
$status = trim((string)($_GET['status'] ?? ''));
$q = trim((string)($_GET['q'] ?? ''));
if ($status !== '' && !in_array($status, STATUS_PEDIDO, true)) json_out(['error' => 'Status inválido'], 400);

com_erro_tratado(function () use ($uid, $status, $q) {
    $sql = 'SELECT p.id, p.status, p.total, p.criado_em,
                   c.nome AS cliente_nome, c.telefone AS cliente_telefone,
                   (SELECT GROUP_CONCAT(CONCAT(pr.nome, " x", i.quantidade) SEPARATOR ", ")
                      FROM item_pedido i JOIN produto pr ON pr.id = i.produto_id
                     WHERE i.pedido_id = p.id) AS resumo
            FROM pedido p
            JOIN cliente c ON c.id = p.cliente_id
            WHERE p.usuario_id = :u';
    $par = ['u' => $uid];
    if ($status !== '') {
        $sql .= ' AND p.status = :s';
        $par['s'] = $status;
    }
    if ($q !== '') {
        $sql .= ' AND (c.nome LIKE :q OR p.id = :qid)';
        $par['q'] = '%' . $q . '%';
        $par['qid'] = ctype_digit($q) ? (int)$q : 0;
    }
    $st = pdo()->prepare($sql . ' ORDER BY p.criado_em DESC, p.id DESC');
    $st->execute($par);
    $linhas = $st->fetchAll();
    foreach ($linhas as &$l) {
        $l['id'] = (int)$l['id'];
        $l['resumo'] = $l['resumo'] ?? '';
    }
    json_out($linhas);
});
