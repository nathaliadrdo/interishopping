<?php
// POST: usuario_id, id, status  ->  muda o status do pedido (pendente > em_producao > a_caminho > finalizado).
require __DIR__ . '/config.php';
exigir_post();

$uid = usuario_id_post();
$id = campo('id');
$status = campo('status');
if (!ctype_digit($id) || (int)$id <= 0 || !in_array($status, STATUS_PEDIDO, true)) {
    json_out(['error' => 'Dados inválidos'], 400);
}

com_erro_tratado(function () use ($uid, $id, $status) {
    $st = pdo()->prepare('UPDATE pedido SET status = :s WHERE id = :id AND usuario_id = :u');
    $st->execute(['s' => $status, 'id' => (int)$id, 'u' => $uid]);
    // rowCount() = 0 também ocorre quando o status já era o mesmo; por isso conferimos se o pedido existe.
    $ex = pdo()->prepare('SELECT 1 FROM pedido WHERE id = :id AND usuario_id = :u');
    $ex->execute(['id' => (int)$id, 'u' => $uid]);
    if (!$ex->fetch()) json_out(['error' => 'Pedido não encontrado'], 404);
    json_out(['status' => 'ok', 'id' => (int)$id]);
});
