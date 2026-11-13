# Checklist da entrega (marque quando estiver pronto)

## Projeto Android Studio (interfaces, acessibilidade, internacionalização)
- [ ] App roda no celular/emulador sem travar (**ainda não foi executado**: rode e corrija o que o Android Studio apontar)
- [ ] Fluxo completo funciona: cadastro, login, Início, produto, pedido, status, WhatsApp, clientes, perfil
- Interfaces (10 telas): Login, Cadastro, Início, Pedidos, Novo pedido, Detalhe do pedido, Catálogo, Formulário de produto, Clientes, Perfil
- Acessibilidade implementada no código: modo escuro (`res/values-night/colors.xml`), `contentDescription` nos gráficos e botões, erro em cada campo, alvos de toque >= 44 dp, contrastes calculados (ver `README_INSTALACAO.md`)
- [ ] Verificar no aparelho: modo escuro, fonte do sistema ampliada, leitor de tela (TalkBack)
- Internacionalização: `values` (português, padrão), `values-en`, `values-es`, 138 textos em cada
- [ ] Verificar no aparelho: trocar o idioma para English e Español e abrir todas as telas

## Itens da atividade
- [ ] 1. Vídeo .mp4 (2 a 3 min), 1 jornada stakeholder + 1 jornada público geral: roteiro em `Roteiro_Video.pdf`
- [ ] 2. Relatório dos stakeholders em PDF (Thayane, Thais e Aruana): aplicar `Questionario_Stakeholders.pdf`, digitar na planilha, rodar `gerar_relatorios.py`
- [ ] 3. Relatório Likert em PDF (>= 10 pessoas; 2 por faixa: 15-26, 27-39, 40-52, 53-64, 65+): aplicar `Questionario_Likert_Publico.pdf`, digitar na planilha, rodar `gerar_relatorios.py`
- [ ] Planilha `RESUMO` mostra "OK" na cobertura mínima

## Antes de zipar
- [ ] Apagar `config.local.php` com a senha real (não entregar nem subir)
- [ ] Colocar o vídeo e os dois relatórios finais na pasta `entrega/`
