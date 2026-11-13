<?php
require __DIR__ . '/config.php';
exigir_post();

$nome  = campo('nome');
$email = strtolower(campo('email'));
$tel   = preg_replace('/\D/', '', campo('telefone'));
$senha = (string)($_POST['senha'] ?? '');

if (mb_strlen($nome) < 2 || !filter_var($email, FILTER_VALIDATE_EMAIL)
    || strlen($tel) !== 11 || $tel[2] !== '9' || strlen($senha) < 6) {
    json_out(['error' => 'Dados inválidos'], 400);
}

try {
    $st = pdo()->prepare('INSERT INTO usuario (nome, email, telefone, senha_hash) VALUES (:n, :e, :t, :h)');
    $st->execute(['n' => $nome, 'e' => $email, 't' => $tel, 'h' => password_hash($senha, PASSWORD_DEFAULT)]);
    json_out(['status' => 'ok', 'id' => (int)pdo()->lastInsertId()], 201);
} catch (PDOException $e) {
    if ($e->getCode() === '23000') json_out(['error' => 'E-mail ou telefone já cadastrado'], 409);
    error_log('[InteriShopping] ' . $e->getMessage());
    json_out(['error' => 'Erro no servidor'], 500);
}
