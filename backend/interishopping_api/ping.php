<?php
// GET: testa se a API e o banco estão no ar. Abra no navegador:
//   http://localhost/interishopping_api/ping.php
// Se der "ok", a conexão com as credenciais do config está funcionando.
require __DIR__ . '/config.php';

com_erro_tratado(function () {
    $versao = pdo()->query('SELECT VERSION() AS v')->fetch()['v'];
    $tabelas = pdo()->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
    json_out(['status' => 'ok', 'mysql' => $versao, 'banco' => DB_NAME, 'tabelas' => $tabelas]);
});
