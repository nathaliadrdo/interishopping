<?php
require __DIR__ . '/config.php';

$uid = usuario_id_get();
$q = trim((string)($_GET['q'] ?? ''));

com_erro_tratado(function () use ($uid, $q) {
    $sql = 'SELECT c.id, c.nome, c.telefone,
                   COUNT(p.id) AS pedidos,
                   COALESCE(SUM(p.total), 0) AS total_gasto
            FROM cliente c
            LEFT JOIN pedido p ON p.cliente_id = c.id AND p.usuario_id = c.usuario_id
            WHERE c.usuario_id = :u';
    $par = ['u' => $uid];
    if ($q !== '') {
        $sql .= ' AND (c.nome LIKE :q1 OR c.telefone LIKE :q2)';
        $par['q1'] = '%' . $q . '%';
        $d = so_digitos($q);
        $par['q2'] = $d === '' ? '#' : '%' . $d . '%';
    }
    $st = pdo()->prepare($sql . ' GROUP BY c.id, c.nome, c.telefone ORDER BY c.nome');
    $st->execute($par);
    $linhas = $st->fetchAll();
    foreach ($linhas as &$l) {
        $l['id'] = (int)$l['id'];
        $l['pedidos'] = (int)$l['pedidos'];
    }
    json_out($linhas);
});
