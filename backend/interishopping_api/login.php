<?php
require __DIR__ . '/config.php';
exigir_post();

$login = strtolower(campo('login'));
$senha = (string)($_POST['senha'] ?? '');
if ($login === '' || $senha === '') json_out(['error' => 'Dados incompletos'], 400);

com_erro_tratado(function () use ($login, $senha) {
    $digitos = preg_replace('/\D/', '', $login);
    $st = pdo()->prepare('SELECT id, nome, senha_hash FROM usuario WHERE email = :e OR telefone = :t LIMIT 1');
    $st->execute(['e' => $login, 't' => $digitos]);
    $u = $st->fetch();

    if (!$u || !password_verify($senha, $u['senha_hash'])) json_out(['error' => 'Credenciais inválidas'], 401);

    json_out(['usuario_id' => (int)$u['id'], 'nome' => $u['nome']]);
});
