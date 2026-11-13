<?php
if (file_exists(__DIR__ . '/config.local.php')) {
    require __DIR__ . '/config.local.php';
}
defined('DB_HOST') || define('DB_HOST', getenv('IS_DB_HOST') ?: 'localhost');
defined('DB_PORT') || define('DB_PORT', getenv('IS_DB_PORT') ?: '3306');
defined('DB_NAME') || define('DB_NAME', getenv('IS_DB_NAME') ?: 'interishopping');
defined('DB_USER') || define('DB_USER', getenv('IS_DB_USER') ?: 'interi_app');
defined('DB_PASS') || define('DB_PASS', getenv('IS_DB_PASS') ?: 'TROCAR_SENHA_AQUI');

function pdo(): PDO {
    static $pdo = null;
    if ($pdo === null) {
        $dsn = 'mysql:host=' . DB_HOST . ';port=' . DB_PORT . ';dbname=' . DB_NAME . ';charset=utf8mb4';
        $pdo = new PDO($dsn, DB_USER, DB_PASS, [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
            PDO::ATTR_TIMEOUT            => 5,
        ]);
    }
    return $pdo;
}

function json_out($dados, $codigo = 200) {
    http_response_code($codigo);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($dados, JSON_UNESCAPED_UNICODE);
    exit;
}

function exigir_post() {
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_out(['error' => 'Use o método POST'], 405);
}

function campo($nome) {
    return trim((string)($_POST[$nome] ?? ''));
}

function usuario_id_post() {
    $id = campo('usuario_id');
    if (!ctype_digit($id) || (int)$id <= 0) json_out(['error' => 'Usuário inválido'], 400);
    return (int)$id;
}

function com_erro_tratado(callable $fn) {
    try {
        $fn();
    } catch (PDOException $e) {
        error_log('[InteriShopping] ' . $e->getMessage());
        json_out(['error' => 'Erro no servidor'], 500);
    }
}

function usuario_id_get() {
    $id = (string)($_GET['usuario_id'] ?? '');
    if (!ctype_digit($id) || (int)$id <= 0) json_out(['error' => 'Usuário inválido'], 400);
    return (int)$id;
}

const STATUS_PEDIDO = ['pendente', 'em_producao', 'a_caminho', 'finalizado'];

function so_digitos($s) {
    return preg_replace('/\D/', '', (string)$s);
}

function telefone_ok($d) {
    return strlen($d) === 11 && $d[2] === '9';
}
