# Verificação — resumo financeiro e quantitativo

Data: 2026-10-09. T024, FR-050, AC-007/T024.

## Escopo e arquivos

Constituição revista; spec.md, plan.md, tasks.md e analysis.md atualizados antes do comportamento. Clarificação registrada: total significa gasto conhecido nos exemplares físicos, não avaliação de mercado. Sem alterações de banco, dados privados ou dependências.

- dashboard/application/CollectionSummary.java: DTO, cobertura dos preços e formatação decimal BRL pt-BR, distinguindo vazio de desconhecido.
- dashboard/application/DashboardService.java e dashboard/infrastructure/DashboardQueries.java: leitura transacional e uma consulta agregada com dono obrigatório, sem carregar entidades nem multiplicar cópias por joins.
- dashboard/presentation/DashboardController.java: proprietário da sessão e DTO no modelo para / e /dashboard.
- templates/dashboard/index.html e static/css/dashboard.css: quatro cartões na inicial, explicação do cálculo, quantidade de preços informados, aviso de total parcial e link ao catálogo; duas/uma coluna em telas menores.
- CollectionSummaryTest.java e MinhaGibitecaApplicationTest.java: testes unitários, integração PostgreSQL/MockMvc e prévia descartável target/ui-summary com registros de teste.

## Evidências executadas

`./mvnw clean verify -q`: execução final código 0, JAR gerado. 75 testes, zero falhas, erros ou ignorados: 32 integrados, 22 de navegação, 10 de checklist, 8 de exemplares, 3 de resumo/serviço. PostgreSQL 16 Testcontainers; migrations V1–V12 e atualização de schema continuam passando. Uma primeira execução também passou; execução limpa repetida após a última atualização do template.

Caso integrado: usuário vazio mostra zero; dois registros de edição (um digital com referência de R$ 500,00 e um físico com referência de R$ 999,00) e quatro exemplares da mesma edição, com R$ 10,10, R$ 20,20, zero e desconhecido. Dashboard mostra 2 edições, 4 exemplares, 1 edição com exemplares, 3 preços conhecidos e R$ 30,30. R$ 1.000,00 de outro usuário e referências bibliográficas não entram na soma. Todos os preços desconhecidos mostram Não informado. Após editar R$ 10,10 para R$ 15,15 e remover exemplar desconhecido, novo acesso mostra 3 exemplares e R$ 35,35 sem lacunas de preço. Acesso anônimo redirecionado. Unitários verificam vazio/desconhecido/gratuito, total decimal acima do limite individual e delegação com dono exato.

Prévia do HTML renderizado conferida no navegador em desktop padrão (1280 pixels) e 320 × 780: quatro/uma coluna, valores e explicações legíveis, aviso de cobertura visível; documento de 320 pixels para janela de 320, sem overflow horizontal. Capturas completas inspecionadas. Prévia não usa dados reais nem testa cliques de destinos ou POST. Aba fechada e viewport restaurado.

`git diff --check`: sem erros. Dados em data/ preservados, não versionados. Servidor temporário de prévia encerrado; aplicação real não reiniciada.

## Limites e pendências

Não calcula valor de revenda, preço de capa ou gastos digitais; não infere valores faltantes. Cadastrar apenas edição não adiciona exemplar físico. Demais indicadores de T016 (séries, distribuições, recentes e duplicados) continuam pendentes, assim como T017–T020. Sem aferição de desempenho em massa nem auditoria WCAG completa nesta etapa. Reiniciar no IntelliJ para carregar a mudança.
