<?php
require __DIR__ . '/config.php';
exigir_post();

$uid = usuario_id_post();
$id  = campo('id');
if (!ctype_digit($id)) json_out(['error' => 'Dados inválidos'], 400);

com_erro_tratado(function () use ($uid, $id) {
    $st = pdo()->prepare('UPDATE produto SET ativo = 0 WHERE id = :id AND usuario_id = :u AND ativo = 1');
    $st->execute(['id' => (int)$id, 'u' => $uid]);
    if ($st->rowCount() === 0) json_out(['error' => 'Produto não encontrado'], 404);
    json_out(['status' => 'ok']);
});
