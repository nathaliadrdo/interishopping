# InteriShopping v2: explicação do código

Guia para entender (e defender na apresentação) cada arquivo. Ordem: como o app se organiza, o back-end PHP, o banco, o app Kotlin arquivo por arquivo e os recursos (layouts, textos, cores).

## 1. Visão geral da arquitetura

```
 Celular (Kotlin)  --HTTP/JSON (Retrofit)-->  Apache + PHP (XAMPP)  --PDO-->  MySQL (banco da universidade)
        |
        +--link wa.me--> WhatsApp (app instalado no aparelho)
```

- O app **não fala com o banco**. Ele chama endpoints PHP; o PHP valida, consulta o MySQL e responde JSON.
- Cada tela do app é uma **Activity** (classe Kotlin) com um **layout XML**. Os dados vêm de `ApiClient.service` (Retrofit).
- Toda consulta é filtrada por `usuario_id`: cada vendedor só enxerga seus dados.

## 2. Back-end PHP (`backend/interishopping_api`)

### 2.1 `config.php`: base de tudo
- Carrega `config.local.php` se existir (credenciais reais) e define `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`. `defined(...) || define(...)` significa "use o valor já definido; se não houver, use variável de ambiente ou o exemplo".
- `pdo()`: abre **uma** conexão PDO reaproveitada (`static`). Opções importantes: `ERRMODE_EXCEPTION` (erros viram exceções), `EMULATE_PREPARES = false` (o MySQL prepara de verdade a consulta, o que reforça a proteção contra injeção de SQL), timeout de 5 s.
- `json_out($dados, $codigo)`: define o código HTTP, o tipo `application/json` e encerra. Todos os endpoints respondem por ela.
- `exigir_post()`, `campo()`, `usuario_id_post()`, `usuario_id_get()`: leitura e validação de entrada.
- `com_erro_tratado(fn)`: executa o endpoint e, se o banco falhar, grava o detalhe no log do servidor (`error_log`) e devolve só "Erro no servidor" ao app (não vaza detalhe técnico).
- `STATUS_PEDIDO`, `so_digitos()`, `telefone_ok()`: regras compartilhadas (mesmas do app).

### 2.2 Autenticação
- `cadastro.php` (POST): valida nome, e-mail, telefone (11 dígitos, o 3º é 9) e senha (>= 6). Guarda `password_hash($senha)`; nunca a senha pura. Se violar a chave única (e-mail ou telefone repetido) devolve **409**.
- `login.php` (POST): aceita e-mail **ou** telefone; compara com `password_verify`. Usuário inexistente e senha errada dão a mesma resposta (**401**), para não revelar qual existe. Retorna `usuario_id` e `nome`.

### 2.3 Produtos (CRUD)
- `produtos.php` (GET): lista os produtos ativos do usuário, com busca opcional `nome LIKE`.
- `incluir_produto.php`, `editar_produto.php` (POST): validam preço >= 0 e estoque >= 0 e gravam com consultas preparadas.
- `deletar_produto.php` (POST): **exclusão lógica** (`ativo = 0`). O produto some do catálogo, mas pedidos antigos continuam consistentes.

### 2.4 Pedidos
- `criar_pedido.php` (POST): o endpoint mais importante.
  1. Valida entrada (`itens` chega como JSON em texto e é decodificado).
  2. `beginTransaction()`: tudo ou nada.
  3. **Cliente:** procura pelo telefone; se não existe, cria. Um cliente recorrente não duplica.
  4. **Itens:** para cada item, lê preço e estoque com `SELECT ... FOR UPDATE` (trava a linha enquanto o pedido é processado, evitando que dois pedidos simultâneos vendam o mesmo estoque). Se faltar estoque: rollback e **409**.
  5. **Total** = soma(preço do banco × quantidade) + entrega − desconto, nunca negativo. O app não manda total.
  6. Grava `pedido` (status inicial `pendente`), os `item_pedido` (com `preco_unitario` congelado) e faz a **baixa de estoque**.
  7. `commit()` e responde **201** com `id` e `total`. Qualquer falha de banco faz rollback.
- `pedidos.php` (GET): lista com filtro `status` (validado contra o ENUM) e busca por nome ou código. Um `GROUP_CONCAT` monta o resumo "Bolo x1, Brigadeiro x10".
- `pedido.php` (GET): cabeçalho + itens de um pedido; se o pedido não é do usuário, **404**.
- `atualizar_status.php` (POST): só aceita os 4 valores do ENUM; confere a existência do pedido separadamente porque `rowCount()` vale 0 também quando o status não muda.

### 2.5 Painel e clientes
- `dashboard.php` (GET): várias consultas pequenas: faturamento do mês e do mês anterior (`CASE WHEN` por `DATE_FORMAT`), pedidos por status (`GROUP BY`), pedidos de hoje, estoque baixo (`< 5`) e vendas dos 7 últimos dias. O PHP preenche com zero os dias sem venda e devolve `dia` de 1 (segunda) a 7 (domingo).
- `clientes.php` (GET): `LEFT JOIN` com pedidos para contar pedidos e somar o gasto; busca por nome ou telefone (os dois parâmetros têm nomes diferentes, `:q1` e `:q2`, porque com `EMULATE_PREPARES = false` o PDO não aceita o mesmo nome repetido).
- `ping.php` (GET): diagnóstico: versão do MySQL, nome do banco e tabelas.

### 2.6 `schema.sql` e `dados_exemplo.sql`
- 5 tabelas com chaves estrangeiras, `CHECK` (preço e estoque >= 0, quantidade > 0), `UNIQUE (usuario_id, telefone)` em `cliente` e índices em `pedido` para o painel e a lista.
- `dados_exemplo.sql`: dados fictícios opcionais, atrelados a `@uid`.

## 3. App Kotlin (`app/src/main/java/com/example/interishopping`)

### 3.1 `Api.kt`
- **Modelos** (`data class`): espelham o JSON da API. Dinheiro chega como `String` porque o PDO devolve `DECIMAL` como texto; é convertido na hora de mostrar (`Validar.numero`, `Validar.moeda`).
- **`ApiService`** (interface Retrofit): cada função é um endpoint (`@GET` com `@Query`, `@POST` com `@FormUrlEncoded` e `@Field`). `Call<T>` é uma chamada que será executada em segundo plano.
- **`ApiClient`**: monta o Retrofit uma vez (`by lazy`) com `BASE_URL`, timeout de 15 s e log do OkHttp em nível BASIC (não registra corpo, então a senha não aparece no Logcat).
- **`Session`**: guarda `usuario_id` e `nome` em `SharedPreferences` (arquivo privado do app). `sair()` apaga.
- **`Validar`**: `telefone`, `valor` (aceita vírgula), `moeda` (formata "R$ 93,00"), `numero`.
- **`executar`**: função de extensão que roda a chamada, devolve o resultado em `onOk` ou mostra um Toast (falta de conexão, 400, 401, 409 com distinção de "estoque insuficiente" ou "já cadastrado", ou erro de servidor). Centraliza o tratamento de erro; as telas só dizem o que fazer quando dá certo.

### 3.2 `Whatsapp.kt`
- `Whats.link()`: `https://wa.me/55<DDD+número>?text=<mensagem>`; `Uri.encode` transforma espaços e acentos em formato seguro para URL.
- `Whats.abrir()`: `Intent.ACTION_VIEW`. Se não houver app para o link, captura `ActivityNotFoundException` e avisa.
- `StatusPedido`: enum com `chave` (texto gravado no banco), `rotulo` (texto exibido) e `mensagemWhats` (modelo de mensagem do status).
- `montarMensagem()`: preenche o modelo com primeiro nome, número do pedido, itens e total.

### 3.3 `Nav.kt`: navegação inferior
- `enum Aba` liga cada botão da barra (`include_nav.xml`) à sua tela.
- `Nav.configurar(activity, abaAtiva)`: pinta a aba ativa de verde e põe o clique nas demais. O **Início é a raiz**: ir para ele usa `CLEAR_TOP | SINGLE_TOP` (desempilha o resto); ir de uma aba para outra faz `finish()` na atual, então "voltar" sempre chega ao Início.

### 3.4 `ChartViews.kt`: gráficos sem biblioteca
Cada gráfico é uma `View` com `onDraw(canvas)`.
- `BarChartView`: divide a largura em 7 fatias; altura proporcional ao maior valor (mínimo de 4 dp para a barra aparecer); a maior barra é verde-escuro, as outras verde-claro.
- `DonutChartView`: `drawArc` por status; ângulo = valor × 360 / total; 2° de folga entre fatias; o total aparece no centro. Sem pedidos, mostra o anel cinza.
- `StatusStepperView`: 4 bolinhas ligadas por linha; as anteriores ao status atual têm "check" verde, a atual fica açafrão, as seguintes ficam vazias.
- `dp()` converte dp para pixel usando a densidade da tela, para o desenho ter o mesmo tamanho em qualquer aparelho.

### 3.5 Activities
| Arquivo | O que faz |
|---|---|
| `LoginActivity` | Valida campos obrigatórios, chama `login`, salva a sessão, abre o Início e dá `finish()` |
| `CadastroActivity` | Valida nome, e-mail, telefone e senha e chama `cadastro`; o 409 (já cadastrado) é tratado por `executar` |
| `InicioActivity` | No `onResume` chama `dashboard`; preenche faturamento, variação sobre o mês anterior (só calcula se houver mês anterior), gráficos e legenda |
| `PedidosActivity` | Chips filtram por status, busca pelo teclado, lista com `PedidoAdapter`; o botão do card busca o pedido completo e abre o WhatsApp |
| `PedidoAdapter` | RecyclerView dos cards; `estilizarStatus()` troca fundo e cor do selo por status |
| `NovoPedidoActivity` | Carrega o catálogo; diálogo em 2 passos (produto, quantidade); soma quantidades do mesmo produto; mostra subtotal e total estimados; ao salvar valida tudo e envia só ids e quantidades em JSON |
| `PedidoDetalheActivity` | Carrega o pedido; chips escolhem o status e a prévia da mensagem muda na hora; "Atualizar status" grava; "Enviar pelo WhatsApp" abre o link |
| `ClientesActivity` | Lista com busca; botão abre a conversa do cliente |
| `PerfilActivity` | Mostra nome e servidor; "Sair" limpa a sessão e a pilha de telas (`NEW_TASK | CLEAR_TASK`) |
| `ProdutosActivity` | Grade de 2 colunas (`GridLayoutManager`), resumo no cabeçalho, exclusão com confirmação |
| `ProdutoAdapter` | Card com inicial, preço e barra de estoque; abaixo de 5 unidades fica vermelho (mesma regra do `dashboard.php`) |
| `ProdutoFormActivity` | Incluir (id = 0) ou editar (id > 0) com a mesma tela |

**Por que `onResume` e não `onCreate` para carregar listas:** `onCreate` roda só uma vez; `onResume` roda toda vez que a tela volta ao primeiro plano. Assim, depois de salvar um pedido, o painel e as listas já reaparecem atualizados.

## 4. Recursos (`res`)

- **Layouts:** `activity_*.xml` (uma por tela), `item_*.xml` (um item de lista/grade) e `include_nav.xml` (barra inferior reaproveitada com `<include>`).
- **Cabeçalho verde:** `bg_header.xml` (forma com base arredondada), reusado em todas as telas principais; os cartões "sobem" sobre ele com margem superior negativa.
- **Cores:** `colors.xml` e `values-night/colors.xml` (modo escuro). Nas telas só se usa `@color/...`; nunca valores fixos, por isso o modo escuro funciona sem código extra.
- **Textos:** `strings.xml` em português (padrão), `values-en` e `values-es`. O Android escolhe o idioma do aparelho. As mensagens do WhatsApp (`msgw_*`) usam marcadores `%1$s`, `%2$d`... preenchidos por `getString`.
- **Estilos:** `NavItem`, `TituloCabecalho`, `LegendaStatus` em `themes.xml` evitam repetir atributos.
- **Acessibilidade:** `contentDescription` nos gráficos, alvos de toque >= 44 dp, erro em cada campo, contraste verificado, modo escuro.
- **Manifesto:** `INTERNET`, `usesCleartextTraffic="true"` (permite HTTP simples no XAMPP local; em produção sai e entra HTTPS) e a lista de Activities, com `LoginActivity` como tela inicial.

## 5. Fluxo completo: "criar pedido e avisar o cliente"

1. Pedidos > **Novo pedido**. A tela busca o catálogo (`produtos.php`).
2. Usuário escolhe produto e quantidade, informa cliente, WhatsApp, entrega e pagamento.
3. **Salvar** > validações no app > `criar_pedido.php` > transação no MySQL > resposta 201.
4. A tela fecha; `PedidosActivity.onResume` recarrega a lista (`pedidos.php`).
5. Toque no card > `PedidoDetalheActivity` (`pedido.php`) > escolhe "Em produção" > **Atualizar status** (`atualizar_status.php`).
6. **Enviar pelo WhatsApp** > `Whats.abrir` > o WhatsApp abre na conversa do cliente com o texto pronto > o vendedor toca em enviar.
7. Voltando ao Início, `dashboard.php` já traz os números novos.

## 6. Decisões de segurança e limites

| Tema | Como está | Limite |
|---|---|---|
| SQL injection | PDO com consultas preparadas e `EMULATE_PREPARES = false` | Nenhum conhecido no código escrito |
| Senhas | `password_hash`/`password_verify` | Sem bloqueio por tentativas |
| Identificação do usuário | `usuario_id` enviado pelo app | Quem souber a API pode informar outro id; o correto é token (JWT) |
| Transporte | HTTP no XAMPP local | Em produção, HTTPS |
| Credenciais do banco | `config.local.php` fora do código versionado | O arquivo precisa ficar fora do repositório |
| Preço do pedido | Calculado no servidor | n/a |
| WhatsApp | Link `wa.me` (o vendedor envia) | Sem confirmação de entrega nem automação |

## 7. Termos para a apresentação
- **Activity:** uma tela. **Intent:** pedido para abrir outra tela ou app. **RecyclerView:** lista que reaproveita os cards. **Retrofit:** biblioteca que transforma uma interface Kotlin em chamadas HTTP. **PDO:** acesso ao banco em PHP. **Transação:** conjunto de comandos que ou acontecem todos ou nenhum. **ENUM:** coluna que só aceita valores de uma lista. **Exclusão lógica:** marcar como inativo em vez de apagar.
