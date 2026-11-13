# Como gerar os relatórios (Stakeholders e Likert)

1. Aplique os questionários impressos (`Questionario_Stakeholders.pdf` e `Questionario_Likert_Publico.pdf`).
2. Digite as respostas na `Planilha_Tabulacao_Testes.xlsx`, somente nas células amarelas. A aba RESUMO calcula e mostra os gráficos.
3. Gere os PDFs:
   ```
   pip install openpyxl matplotlib reportlab
   python gerar_relatorios.py Planilha_Tabulacao_Testes.xlsx
   ```
   Saem `Relatorio_Stakeholders.pdf` e `Relatorio_Likert_Publico.pdf` na mesma pasta.
4. Se não quiser rodar o script, envie a planilha preenchida ao Claude para montar os PDFs.

Os relatórios usam apenas os dados digitados; nada é inventado. Se faltar gente em alguma faixa etária, o relatório avisa na seção "Amostra".
