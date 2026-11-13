# InteriShopping v2: instalação e execução

Aplicativo Android (Kotlin) para microempreendedores gerenciarem pedidos, catálogo e clientes, com aviso ao cliente pelo WhatsApp. Back-end em PHP + MySQL.

> **Situação honesta do código:** tudo foi escrito e conferido por leitura e verificação automática de sintaxe (XML bem formado, referências `R.id`/`R.string`/`R.drawable` existentes, `php -l` em todos os `.php`). **Nada foi executado**: não havia emulador Android nem servidor MySQL disponíveis onde o código foi escrito. O primeiro "Run" é o teste real; se o Android Studio ou o PHP apontarem erro, copie a mensagem e corrija com ela.

## O que vem na pasta

| Caminho | Conteúdo |
|---|---|
| `backend/interishopping_api/` | API PHP (13 endpoints), `schema.sql`, `dados_exemplo.sql`, `config.local.php.exemplo` |
| `app/src/main/` | Código Kotlin, layouts, textos em 3 idiomas, cores (claro e escuro), `AndroidManifest.xml` |
| `app_build.gradle.kts`, `gradle_dependencias.txt` | Dependências (Retrofit, OkHttp, Material 3, RecyclerView) |
| `docs/` | `PASSO_A_PASSO_EVOLUCAO.md` e `EXPLICACAO_DO_CODIGO.md` (e versões em PDF) |

## 1. Servidor: XAMPP + banco da universidade

1. Instale e abra o **XAMPP Control Panel**. Clique em **Start** no **Apache**. Só inicie o **MySQL** do XAMPP se for usar um banco local (veja a opção B).
2. Copie a pasta `backend/interishopping_api` para `C:\xampp\htdocs\interishopping_api`.
3. **Credenciais do banco** (o professor forneceu host, banco, usuário e senha):
   - Na pasta `interishopping_api`, copie `config.local.php.exemplo` para `config.local.php`.
   - Abra `config.local.php` e troque os valores `COLOQUE_...` pelos dados reais.
   - `config.local.php` nunca deve ir para o relatório, para o GitHub nem para prints.
4. **Opção A: banco fornecido pela universidade (é o que o professor pediu).** Use o host e a porta informados (se for um servidor remoto, o computador precisa alcançá-lo, às vezes só pela rede/VPN da universidade).
5. **Opção B: banco local do XAMPP (para testar em casa).** Inicie o MySQL no XAMPP; no Beekeeper crie uma conexão para `127.0.0.1`, porta `3306`, usuário `root`, senha vazia, e crie o banco `interishopping` (`CREATE DATABASE interishopping CHARACTER SET utf8mb4;`). Use esses mesmos dados no `config.local.php`.

## 2. Beekeeper Studio: criar as tabelas

1. Abra o **Beekeeper Studio** > **New Connection** > tipo **MySQL**.
2. Preencha host, porta, usuário, senha e banco (os mesmos do `config.local.php`) > **Test** > **Connect**.
3. Abra `schema.sql` (File > Open) e clique em **Run** (ou Ctrl+Enter). Ele cria `usuario`, `cliente`, `produto`, `pedido` e `item_pedido`. Pode rodar de novo sem problema (`CREATE TABLE IF NOT EXISTS`).
4. **Opcional:** crie sua conta pelo app primeiro e confira o `id` dela na tabela `usuario`; se não for `1`, troque `SET @uid = 1;` no início de `dados_exemplo.sql` e rode o arquivo. Ele insere 4 produtos, 3 clientes e 3 pedidos **fictícios** para o painel já nascer com números.

## 3. Testar a API pelo navegador

| URL | Resultado esperado |
|---|---|
| `http://localhost/interishopping_api/ping.php` | JSON com `"status":"ok"`, a versão do MySQL, o nome do banco e a lista de tabelas |
| `http://localhost/interishopping_api/produtos.php?usuario_id=1` | `[]` (ou os produtos de exemplo) |
| `http://localhost/interishopping_api/dashboard.php?usuario_id=1` | JSON com `faturamento_mes`, `por_status`, `vendas_semana`... |

Se `ping.php` mostrar erro de conexão, o problema está nas credenciais, no host/porta ou na rede, e não no app.

## 4. App: Android Studio

1. Abra o projeto InteriShopping. Copie `app/src/main/java`, `app/src/main/res` e `AndroidManifest.xml` para o mesmo caminho do seu projeto, substituindo os de mesmo nome.
2. **Apague o `Telas.kt` antigo** (os arquivos novos `PedidosActivity.kt` e `NovoPedidoActivity.kt` substituem as classes que estavam nele; se ficar os dois, o Kotlin reclama de classe duplicada).
3. **Package:** o código usa `com.example.interishopping`. Se o seu for diferente, troque a primeira linha de cada `.kt` e os nomes de classe nos layouts (`com.example.interishopping.BarChartView` etc.).
4. Dependências: confira o que está em `gradle_dependencias.txt` no `build.gradle.kts` do módulo `app` e clique em **Sync Now**. Não há biblioteca nova de gráficos (são desenhados em Canvas).
5. `Api.kt` > `ApiClient.BASE_URL`:
   - **Emulador:** `http://10.0.2.2/interishopping_api/` (já está; `10.0.2.2` é o computador visto de dentro do emulador).
   - **Celular real:** `http://<IPv4 do PC>/interishopping_api/` (veja com `ipconfig`), celular na mesma rede Wi-Fi, e libere a porta 80 no firewall do Windows se precisar.
6. Rode (**Run**). Fluxo: Login/Cadastro > Início > demais abas.

## 5. Roteiro de testes (preencha a coluna "Resultado" quando executar)

| # | Passo | Resultado esperado | Resultado |
|---|---|---|---|
| 1 | Criar conta com telefone `119999` | Erro "Telefone incompleto..." | |
| 2 | Criar conta válida e entrar | Abre o **Início** com "Olá, <nome>" | |
| 3 | Catálogo > + Novo produto (Bolo de cenoura, 70,00, estoque 12) | Card na grade, barra de estoque verde | |
| 4 | Editar estoque para 4 | Barra e texto em vermelho; Início mostra "Estoque baixo: 1" | |
| 5 | Pedidos > Novo pedido: cliente, WhatsApp `11988881111`, item Bolo × 2, entrega 8 | Total estimado R$ 148,00 | |
| 6 | Salvar pedido | "Pedido salvo!"; estoque do bolo cai de 4 para 2 | |
| 7 | Pedir quantidade maior que o estoque | Mensagem de estoque insuficiente; nada é gravado | |
| 8 | Abrir o pedido > escolher "A caminho" > Atualizar status | Andamento avança; status muda na lista | |
| 9 | Enviar pelo WhatsApp | WhatsApp abre com a conversa do cliente e a mensagem pronta | |
| 10 | Início | Faturamento, gráfico da semana e rosca com os números dos pedidos | |
| 11 | Clientes | Cliente aparece com nº de pedidos e total gasto | |
| 12 | Perfil > Sair da conta | Volta ao login e "voltar" não reabre o app logado | |
| 13 | Idioma do aparelho em English / Español | Todos os textos mudam | |
| 14 | Modo escuro | Telas legíveis | |
| 15 | Desligar o Apache e abrir o Início | Aviso de falta de conexão, sem travar | |

## 6. Problemas comuns

| Sintoma | Causa provável | Como resolver |
|---|---|---|
| App mostra "Sem conexão com o servidor" | Apache desligado, `BASE_URL` errada ou firewall | Teste a URL no navegador do aparelho; confira o IP |
| `{"error":"Erro no servidor"}` | Credenciais erradas ou tabelas não criadas | Abra `ping.php`; rode `schema.sql`; veja `C:\xampp\apache\logs\error.log` |
| Erro 404 nas rotas | Pasta no lugar errado | Deve ser `htdocs\interishopping_api\` (sem pasta extra por dentro) |
| WhatsApp não abre | Aparelho sem WhatsApp/navegador | O app avisa por mensagem; instale o WhatsApp ou teste num celular |
| Emulador não abre | Virtualização desativada no Windows | Ative a "Plataforma do Hipervisor do Windows" (ou a virtualização na BIOS) ou teste num celular real com depuração USB |
