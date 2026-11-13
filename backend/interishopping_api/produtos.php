<?php
require __DIR__ . '/config.php';

$uid = $_GET['usuario_id'] ?? '';
if (!ctype_digit((string)$uid) || (int)$uid <= 0) json_out(['error' => 'Usuário inválido'], 400);
$q = trim((string)($_GET['q'] ?? ''));

com_erro_tratado(function () use ($uid, $q) {
    $sql = 'SELECT id, nome, categoria, preco, estoque FROM produto WHERE usuario_id = :u AND ativo = 1';
    $par = ['u' => (int)$uid];
    if ($q !== '') {
        $sql .= ' AND nome LIKE :q';
        $par['q'] = '%' . $q . '%';
    }
    $st = pdo()->prepare($sql . ' ORDER BY nome');
    $st->execute($par);
    $linhas = $st->fetchAll();
    foreach ($linhas as &$l) { $l['id'] = (int)$l['id']; $l['estoque'] = (int)$l['estoque']; }
    json_out($linhas);
});
