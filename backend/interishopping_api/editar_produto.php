<?php
require __DIR__ . '/config.php';
exigir_post();

$uid  = usuario_id_post();
$id   = campo('id');
$nome = campo('nome');
$cat  = campo('categoria');
$preco = campo('preco');
$est  = campo('estoque');

if (!ctype_digit($id) || $nome === '' || mb_strlen($nome) > 150 || !is_numeric($preco) || (float)$preco < 0 || !ctype_digit($est)) {
    json_out(['error' => 'Dados inválidos'], 400);
}

com_erro_tratado(function () use ($uid, $id, $nome, $cat, $preco, $est) {
    $st = pdo()->prepare('UPDATE produto SET nome = :n, categoria = :c, preco = :p, estoque = :e
                          WHERE id = :id AND usuario_id = :u AND ativo = 1');
    $st->execute(['n' => $nome, 'c' => $cat, 'p' => $preco, 'e' => (int)$est, 'id' => (int)$id, 'u' => $uid]);

    $chk = pdo()->prepare('SELECT 1 FROM produto WHERE id = :id AND usuario_id = :u AND ativo = 1');
    $chk->execute(['id' => (int)$id, 'u' => $uid]);
    if (!$chk->fetch()) json_out(['error' => 'Produto não encontrado'], 404);

    json_out(['status' => 'ok']);
});
