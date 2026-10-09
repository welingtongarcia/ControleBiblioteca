# Clarificação — Financeiro

## Evidência do ambiente

Edition tem reference_paid_price e cover_price, mas não data de compra. PhysicalCopy possui acquisition_price e acquisition_date. Datas de criação não equivalem a compra. Personagem não existe; Creator é genérico, sem papel de autoria. O resumo inicial já soma valores de referência de edição e separa aquisição de cópias.

## Premissas propostas

- Coleção = série editorial atual; autor mostrado como criador enquanto não houver papéis cadastrados.
- Personagens e datas reais preenchidos manualmente; antigos permanecem desconhecidos.
- Filtros AND entre dimensões; rankings com vínculos múltiplos são compartilhados e não aditivos.
- Gráficos acessíveis no servidor; nenhuma API financeira/Google Books necessária.

## Decisão pendente

Fonte: valor por edição ou compras individuais. Perguntar ao usuário antes de fechar modelo/migrations e implementar. Esta escolha altera o significado de quanto gastei e os fluxos de cadastro.
