# InteriShopping: passo a passo da evolução do aplicativo

Equipe E: Gabriela de Alencar Marin e Nathalia Dourado Luz. Disciplina Sistemas para Dispositivos Móveis (PI5), Prof. Me. Ozias G. Santos. Tema: ODS 8, trabalho decente e crescimento econômico (microempreendedores que vendem pelo WhatsApp).

Este documento conta, em ordem, como o app saiu da ideia até a versão atual (v2), o que mudou em cada fase, em quais arquivos e como conferir. Onde algo não foi executado, está dito.

## Visão geral das fases

| Fase | Foco | Resultado |
|---|---|---|
| Etapa 1 | Ideia, público, guia de estilo | Problema, personas, cores, fontes, telas planejadas |
| Etapa 2 | Protótipo, validações, modelo de dados | Telas, regras de validação, modelo de 5 tabelas |
| Etapa 3 | Código real: API, banco, CRUD, i18n, acessibilidade | Login, cadastro, catálogo (CRUD) com API PHP e MySQL |
| v2, fase A | Back-end completo de pedidos | Pedidos, clientes, painel, status, baixa de estoque |
| v2, fase B | Integração com WhatsApp | Mensagem pronta por status, abertura por link `wa.me` |
| v2, fase C | Novo design e gráficos | Início com gráficos, navegação inferior, cartões, detalhe do pedido |
| v2, fase D | Configuração e entrega | `config.local.php`, `ping.php`, dados de exemplo, documentação |

## Etapa 1: ideia e identidade

- **Problema:** quem vende por WhatsApp (doces, artesanato, serviços) anota pedido em caderno ou em conversas soltas, perde controle de estoque e de quem já pagou.
- **Solução:** app simples para registrar pedido, acompanhar o status e avisar o cliente.
- **Guia de estilo** (vira `colors.xml`): verde-pinheiro `#14503F`, açafrão `#F4B23E`, menta `#DDF3E6`, névoa `#F6F8F5`, tinta `#1B2420`. Fontes planejadas: Bricolage Grotesque (títulos) e Figtree (texto).
- **Por que importa depois:** todas as telas da v2 reutilizam essas cores; mudar uma cor em um lugar muda o app inteiro.

## Etapa 2: protótipo, regras e dados

- **Validações definidas** (viraram o objeto `Validar` em `Api.kt` e as mesmas regras em PHP): telefone = DDD + 9 dígitos; valor monetário >= 0, aceitando vírgula ou ponto; campos obrigatórios com mensagem clara.
- **Modelo de dados:** `usuario`, `cliente`, `produto`, `pedido`, `item_pedido` (arquivo `schema.sql`). Decisões: senha só como hash; `ativo` para exclusão lógica; `status` como ENUM de 4 valores; `preco_unitario` copiado para `item_pedido` (se o preço do produto mudar depois, pedidos antigos não mudam).
- **Como conferir:** abrir `schema.sql` no Beekeeper e ver o diagrama de tabelas.

## Etapa 3: primeira versão funcional

1. **Back-end:** `config.php` (conexão PDO), `cadastro.php`, `login.php`, `produtos.php`, `incluir_produto.php`, `editar_produto.php`, `deletar_produto.php`.
2. **App:** `LoginActivity`, `CadastroActivity`, `ProdutosActivity` (lista), `ProdutoFormActivity` (incluir/editar), `ProdutoAdapter`, tudo ligado por Retrofit em `Api.kt`.
3. **Internacionalização:** textos em `values` (português), `values-en`, `values-es`.
4. **Acessibilidade:** modo escuro (`values-night`), áreas de toque de 44 dp, erro em cada campo, `contentDescription`, contrastes calculados (WCAG).
5. **Segurança básica:** consultas preparadas (PDO) contra injeção de SQL; senha com `password_hash`; cada consulta filtra por `usuario_id`.
6. **Limitação conhecida:** a API confia no `usuario_id` enviado pelo app. Em produção seria trocado por token (JWT).

## v2, fase A: pedidos, clientes e painel (back-end)

Objetivo: o pedido deixar de ser só tela e passar a ser gravado no banco.

| Passo | O que foi feito | Arquivo |
|---|---|---|
| A1 | Ajustes no banco: telefone único por vendedor no cliente; índices por data e status no pedido | `schema.sql` |
| A2 | Funções de apoio: `usuario_id_get`, `STATUS_PEDIDO`, `so_digitos`, `telefone_ok` | `config.php` |
| A3 | **Criar pedido em transação:** reaproveita ou cria o cliente pelo telefone; trava as linhas de produto (`FOR UPDATE`); recusa estoque insuficiente (409); calcula o total no servidor; grava pedido e itens; baixa o estoque; se algo falhar, desfaz tudo (rollback) | `criar_pedido.php` |
| A4 | Listar pedidos com filtro por status e busca por cliente/código | `pedidos.php` |
| A5 | Detalhe do pedido com itens (404 se o pedido não for do usuário) | `pedido.php` |
| A6 | Atualizar status (só valores do ENUM) | `atualizar_status.php` |
| A7 | Listar clientes com nº de pedidos e total gasto | `clientes.php` |
| A8 | Painel: faturamento do mês e do anterior, pedidos de hoje, a caminho, estoque baixo, pedidos por status, vendas dos últimos 7 dias | `dashboard.php` |

**Decisão de projeto:** o app **não envia o total**. Ele envia ids e quantidades; o servidor consulta os preços no banco. Assim, ninguém altera o preço adulterando a requisição.

## v2, fase B: integração com o WhatsApp

- **Escolha:** link `https://wa.me/55<telefone>?text=<mensagem>` aberto com `Intent.ACTION_VIEW`.
- **Vantagens:** não exige conta comercial nem chave de API nem permissão extra no manifesto; o vendedor revisa a mensagem e toca em enviar (o envio é feito por ele, não pelo app).
- **Limite:** o app não sabe se a mensagem foi realmente enviada nem recebe respostas. A **API oficial do WhatsApp Business (Meta Cloud API)** permitiria envio automático e recebimento, mas exige conta Business verificada, número dedicado, modelos de mensagem aprovados e um servidor com HTTPS. Fica como evolução futura, não está implementada.
- **Código:** `Whatsapp.kt` (`Whats.abrir`, `StatusPedido`, `montarMensagem`). Os 4 modelos de mensagem ficam em `strings.xml` (`msgw_*`), nos 3 idiomas, com os campos nome, número do pedido, itens e total.

## v2, fase C: novo design e gráficos

Primeiro foi feita uma **prévia de design** (Início, Pedidos, Catálogo, Detalhe) para aprovar o visual. Depois ela foi traduzida para Android.

| Passo | O que foi feito | Arquivos |
|---|---|---|
| C1 | Cores novas (verde-médio, azuis, âmbar de texto), com versão escura | `colors.xml` (claro e night) |
| C2 | Ícones vetoriais e formas: cabeçalho de base arredondada, selos de status, avatar, bolinha de legenda | `res/drawable/*` |
| C3 | Estilos reutilizáveis: item da barra inferior, título de cabeçalho, linha de legenda | `themes.xml` |
| C4 | Barra de navegação inferior compartilhada | `include_nav.xml`, `Nav.kt` |
| C5 | Gráficos desenhados em Canvas: barras, rosca, andamento do pedido | `ChartViews.kt` |
| C6 | Tela **Início** com faturamento, barras da semana, 3 cartões e rosca | `activity_inicio.xml`, `InicioActivity.kt` |
| C7 | **Pedidos** com chips de filtro, busca, cards e botão "Avisar no WhatsApp" | `activity_pedidos.xml`, `item_pedido.xml`, `PedidosActivity.kt`, `PedidoAdapter.kt` |
| C8 | **Novo pedido** com itens dinâmicos, entrega, desconto, pagamento e total estimado | `activity_novo_pedido.xml`, `NovoPedidoActivity.kt` |
| C9 | **Detalhe do pedido** com andamento, itens, escolha de status e prévia da mensagem | `activity_pedido_detalhe.xml`, `PedidoDetalheActivity.kt` |
| C10 | **Clientes** e **Perfil** | `ClientesActivity.kt`, `PerfilActivity.kt` e layouts |
| C11 | **Catálogo** em grade com barra de estoque | `ProdutosActivity.kt`, `ProdutoAdapter.kt`, `item_produto.xml` |
| C12 | Após o login o app abre o Início; novas telas registradas no manifesto | `LoginActivity.kt`, `AndroidManifest.xml` |
| C13 | Textos novos (66 chaves) em português, inglês e espanhol | `strings.xml` x3 |

**Por que Canvas e não uma biblioteca de gráficos:** menos dependência, menos tamanho de app e o aluno entende cada linha. A desvantagem é não ter animação nem toque nos gráficos.

## v2, fase D: configuração e entrega

- `config.local.php` (copiado de `config.local.php.exemplo`) guarda as credenciais reais fora do código versionado. Ordem de prioridade: `config.local.php`, depois variáveis de ambiente `IS_DB_*`, depois os valores de exemplo.
- `ping.php` testa a conexão com o banco pelo navegador.
- `dados_exemplo.sql` (opcional) cria dados fictícios para a demonstração.
- `README_INSTALACAO.md` traz o passo a passo com XAMPP e Beekeeper e uma tabela de testes.

## Como conferir cada fase (resumo)

| Fase | Verificação |
|---|---|
| Banco | Beekeeper: 5 tabelas existem; `ping.php` lista as tabelas |
| API | Abrir as URLs de teste do README no navegador |
| Pedido | Salvar pedido e ver `pedido`, `item_pedido` e o `estoque` mudarem no Beekeeper |
| WhatsApp | Tocar em "Enviar pelo WhatsApp": o app abre com o texto pronto |
| Design | Comparar as telas do app com a prévia de design |

## O que ainda não está feito

Autenticação por token (JWT); bloqueio após tentativas de login erradas; recuperação de senha; envio automático pela API oficial do WhatsApp; fontes Bricolage Grotesque e Figtree dentro do app (usa a fonte padrão do Android); logotipo; testes automatizados; execução e prints reais das telas novas (dependem de rodar o app).
