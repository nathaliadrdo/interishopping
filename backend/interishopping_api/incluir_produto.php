<?php
require __DIR__ . '/config.php';
exigir_post();

$uid  = usuario_id_post();
$nome = campo('nome');
$cat  = campo('categoria');
$preco = campo('preco');
$est  = campo('estoque');

if ($nome === '' || mb_strlen($nome) > 150 || !is_numeric($preco) || (float)$preco < 0 || !ctype_digit($est)) {
    json_out(['error' => 'Dados inválidos'], 400);
}

com_erro_tratado(function () use ($uid, $nome, $cat, $preco, $est) {
    $st = pdo()->prepare('INSERT INTO produto (usuario_id, nome, categoria, preco, estoque) VALUES (:u, :n, :c, :p, :e)');
    $st->execute(['u' => $uid, 'n' => $nome, 'c' => $cat, 'p' => $preco, 'e' => (int)$est]);
    json_out(['status' => 'ok', 'id' => (int)pdo()->lastInsertId()], 201);
});
