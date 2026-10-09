# Clarificação — Financeiro

## Evidência do ambiente

Edition tem reference_paid_price e cover_price, mas não data de compra. PhysicalCopy possui acquisition_price e acquisition_date. Datas de criação não equivalem a compra. Personagem não existe; Creator é genérico, sem papel de autoria. O resumo inicial já soma valores de referência de edição e separa aquisição de cópias.

## Premissas propostas

- Coleção = série editorial atual; autor mostrado como criador enquanto não houver papéis cadastrados.
- Personagens e datas reais preenchidos manualmente; antigos permanecem desconhecidos.
- Filtros AND entre dimensões; rankings com vínculos múltiplos são compartilhados e não aditivos.
- Gráficos acessíveis no servidor; nenhuma API financeira/Google Books necessária.

## Decisão confirmada — 2026-10-09

Usuário respondeu 2: compras individuais. Um lançamento representa a compra de uma cópia/edição (física ou digital), com valor obrigatório não negativo e data real opcional. Repetir edição em vários lançamentos é permitido; não cria exemplar automaticamente. Vendedor/origem e observações opcionais. Sem importação automática: valores anteriores continuam separados e podem ser registrados manualmente pelo usuário.

Data futura não é gasto realizado; rejeitar. Sem data continua no total geral, fora dos períodos. Personagens associados manualmente no formulário da edição; criador continua genérico, sem inferir função. Resumo inicial permanece referência por edição, Financeiro é total dos lançamentos; diferença explicitada na tela.

Filtros de datas aceitam início e fim juntos ou nenhum, inclusivos, intervalo máximo 3660 dias. Comparação anterior tem mesma duração. Sem datas, gráfico mensal mostra últimos 12 meses até hoje, claramente rotulado, e demais indicadores/rankings analisam todos os lançamentos. Com intervalo, mensal usa todos os meses do intervalo incluindo meses zerados. Rankings top 10 com acesso por filtro; não somar grupos de criadores/personagens.
