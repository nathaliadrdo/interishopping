<?php
require __DIR__ . '/config.php';

com_erro_tratado(function () {
    $versao = pdo()->query('SELECT VERSION() AS v')->fetch()['v'];
    $tabelas = pdo()->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
    json_out(['status' => 'ok', 'mysql' => $versao, 'banco' => DB_NAME, 'tabelas' => $tabelas]);
});
