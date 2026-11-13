<?php
require __DIR__ . '/config.php';

$uid = usuario_id_get();

com_erro_tratado(function () use ($uid) {
    $db = pdo();

    $f = $db->prepare('SELECT
            COALESCE(SUM(CASE WHEN DATE_FORMAT(criado_em, "%Y-%m") = DATE_FORMAT(CURDATE(), "%Y-%m") THEN total END), 0) AS mes,
            COALESCE(SUM(CASE WHEN DATE_FORMAT(criado_em, "%Y-%m") = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), "%Y-%m") THEN total END), 0) AS mes_anterior
          FROM pedido WHERE usuario_id = :u');
    $f->execute(['u' => $uid]);
    $fat = $f->fetch();

    $s = $db->prepare('SELECT status, COUNT(*) AS n FROM pedido WHERE usuario_id = :u GROUP BY status');
    $s->execute(['u' => $uid]);
    $por = array_fill_keys(STATUS_PEDIDO, 0);
    foreach ($s->fetchAll() as $r) $por[$r['status']] = (int)$r['n'];

    $h = $db->prepare('SELECT COUNT(*) FROM pedido WHERE usuario_id = :u AND DATE(criado_em) = CURDATE()');
    $h->execute(['u' => $uid]);
    $hoje = (int)$h->fetchColumn();

    $e = $db->prepare('SELECT COUNT(*) FROM produto WHERE usuario_id = :u AND ativo = 1 AND estoque < 5');
    $e->execute(['u' => $uid]);
    $baixo = (int)$e->fetchColumn();

    $v = $db->prepare('SELECT DATE(criado_em) AS dia, SUM(total) AS total
                         FROM pedido WHERE usuario_id = :u AND criado_em >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
                        GROUP BY DATE(criado_em)');
    $v->execute(['u' => $uid]);
    $mapa = [];
    foreach ($v->fetchAll() as $r) $mapa[$r['dia']] = (float)$r['total'];
    $semana = [];
    for ($i = 6; $i >= 0; $i--) {
        $d = new DateTime("-$i day");
        $chave = $d->format('Y-m-d');
        $semana[] = ['dia' => (int)$d->format('N'), 'total' => $mapa[$chave] ?? 0];
    }

    json_out([
        'faturamento_mes'          => (float)$fat['mes'],
        'faturamento_mes_anterior' => (float)$fat['mes_anterior'],
        'pedidos_hoje'             => $hoje,
        'a_caminho'                => $por['a_caminho'],
        'estoque_baixo'            => $baixo,
        'por_status'               => $por,
        'vendas_semana'            => $semana,
    ]);
});
