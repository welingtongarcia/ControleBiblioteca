# Verificação — Financeiro

2026-10-09. Fonte confirmada pelo usuário: opção 2, compras individuais. Planejamento registrado no commit 0668c9d; especificação inicial no 55d25a3.

## Arquivos e entrega

- `src/main/java/br/com/minhagibiteca/finance/`: regras decimais, filtros/DTOs, agregações JDBC por proprietário, serviço transacional e controller MVC com Bean Validation, CSRF e PRG.
- `src/main/resources/db/migration/V14__create_financial_purchases_and_characters.sql`: compras, personagens e vínculos; FKs compostas por proprietário e índices de consultas. Sem importação de valores/datas e sem alteração de preços anteriores.
- Módulo catalog: personagens privados, cadastro de vínculos no formulário de edição e fachada transacional para evitar cadastros parciais. Bloqueio de exclusão de edição com compras vinculadas.
- `templates/financeiro/`, `templates/personagens/`, fragmentos de navegação, formulários/detalhes de edições, CSS e mensagens pt-BR: fluxos de cadastro/edição/exclusão, filtros, SVG, tabelas, paginação e contexto de retorno.
- `src/test/java/br/com/minhagibiteca/finance/`: testes unitários/Mockito e integração PostgreSQL/MockMvc. `docs/financeiro.md`: guia de uso e diferenças do resumo inicial.

## Evidência executada

- `./mvnw clean verify -q`: exit 0. Relatórios Surefire: **129 testes, 0 falhas, 0 erros, 0 ignorados** (116 anteriores e 13 novos). PostgreSQL 16 em Testcontainers; Flyway validou/aplicou V14 nos bancos de teste e no fluxo existente de atualização de esquema.
- `git diff --check`: exit 0 antes do fechamento.
- Primeira rodada integrada encontrou nome incorreto de tabela na limpeza de testes; corrigido para `series_checklist_items` antes da verificação completa bem-sucedida. Nenhuma limpeza executada no banco do usuário.
- Prévia local de HTML produzido por MockMvc: painel e formulário conferidos visualmente em desktop e 320 px. Largura do documento 305 px com viewport 320 px (barra de rolagem), sem overflow horizontal global. Tabelas mensal e de personagens abertas por Enter, com foco visível e valores equivalentes. Estado vazio conferido. Teste visual usa dados fictícios e servidor estático; não substitui teste HTTP de autenticação/POST, coberto pelo MockMvc.
- Revisão de consultas: agregação SQL com EXISTS para filtros compartilhados e distinct nas relações de criadores; 5 consultas de agrupamento, 2 de resumo/cobertura, 1 mensal e 1 de listagem por relatório, além das validações/opções de filtros. Sem consultas por linha de compra ou item de gráfico; listagem de compras/personagens paginada em 20. Índices owner/data, owner/edição e relações presentes na V14.

## Rastreabilidade dos aceites

| Aceite | Evidência |
| --- | --- |
| FIN-AC-001 | FinanceServiceTest/FinanceMathTest + FinanceIntegrationTest: zero, ausência de valor rejeitada, sem data, média decimal, suporte físico/digital/não informado; prévia vazia. |
| FIN-AC-002 | Filtros combinados e datas inclusivas em integração; 24 compras com 2 páginas e total independente da página; URLs preservam datas/ranking. |
| FIN-AC-003 | Dois criadores/personagens e exemplar com aquisição prévia não multiplicam compras; total 370, atribuições de personagens 540 explicitamente compartilhadas. |
| FIN-AC-004 | Unitário de ano bissexto/duração e percentuais sem base; integração compara março com período anterior de 31 dias, 150 versus 100, variação 50%; meses vazios preenchidos. |
| FIN-AC-005 | CRUD atualiza total e cobertura; leitura altera gasto em não lidas; vínculo de personagem atualizado atomicamente; POST inválido conserva rascunho e não grava compra. |
| FIN-AC-006 | POST sem CSRF 403, anônimo redirecionado, edição/compra/personagem/filtro alheios 404; XSS escapado, datas/UUID/ranking inválidos 400, retorno externo recusado. |
| FIN-AC-007 | Flyway/Testcontainers e regressão de upgrade; SQL/índices revisados, relações compartilhadas sem multiplicação, paginação e top 10 com residual apenas nas distribuições exclusivas. |
| FIN-AC-008 | Gráficos/tabulação oriundos dos mesmos DTOs; prévia visual e teclado, 320 px, suíte Maven completa acima. |

## Pendências e riscos conhecidos

- Não houve deploy, reinício da aplicação do IntelliJ ou execução de migração no banco do usuário. Ao reiniciar a aplicação atualizada, o Flyway aplicará V14 no banco configurado.
- Acervos grandes não foram submetidos a benchmark ou EXPLAIN em volume; não há alegação de p95. Dropdowns bibliográficos carregam opções próprias, podendo exigir busca paginada em evolução futura.
- Conferência de teclado/reflow e atributos semânticos realizada, mas não auditoria integral WCAG 2.2 AA nem validação com todos os leitores de tela.
- Aviso existente de autoanexação do agente Mockito no Java 21 não impediu os testes; adaptação para versões futuras do JDK fica separada desta entrega.
- Compras antigas não entram automaticamente; datas desconhecidas ficam fora de períodos. Classificações atuais das edições alteram a distribuição histórica. Personagens/criadores compartilhados não constituem parcelas somáveis.
- `data/` local não versionado preservado e não incluído no commit.
