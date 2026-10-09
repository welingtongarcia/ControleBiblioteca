# Verificação — correção do resumo por cadastro

Data: 2026-10-09. T025, FR-050, AC-007/T025.

## Decisão e arquivos

Após relato e imagem do usuário (1 edição e nenhum exemplar), inspeção confirmou que a soma anterior considerava apenas aquisição física. Usuário aprovou somar Valor pago dos cadastros, contar físicos/digitais e separar exemplares. Constituição preservada e spec/plan/tasks/analysis atualizados antes do código. Esta entrega substitui a semântica principal anterior de T024; evidências anteriores são históricas, não o cálculo vigente.

- CollectionSummary.java: indicadores de edições físicas/digitais, suporte desconhecido, cobertura de valores e soma registrada com BRL decimal; mantém métricas reais de exemplares separadas.
- DashboardQueries.java: conta media_type e soma reference_paid_price por dono; agregados independentes de physical_copies, consulta única sem N+1, sem multiplicar referências por quantidade de cópias.
- templates/dashboard/index.html: seis cartões, total principal de cadastros, cobertura, desconhecidos e aquisição de exemplares separada; preço de capa excluído; rótulos distinguem edições e cópias.
- dashboard.css: três colunas em desktop, duas/uma nos breakpoints existentes.
- CollectionSummaryTest.java e MinhaGibitecaApplicationTest.java: formatação, cadastro/edição, somas separadas e dados isolados; prévia do HTML de teste em target/ui-summary.

## Evidências executadas

`./mvnw clean verify -q`: código 0, JAR gerado. **77 testes, zero falhas/erros/ignorados**: 34 integrados PostgreSQL/MockMvc, 22 navegação, 10 checklist, 8 exemplares, 3 resumo/serviço. Migrations V1–V12 e upgrade de schema continuam passando em Testcontainers PostgreSQL 16.

Novo teste MVC cadastra edição física de R$ 29,90 sem exemplar: total 29,90, física 1, digital 0, cópias 0. POST de edição troca para digital com valor zero: física 0, digital 1, preço conhecido zero; remoção do valor mostra Não informado e cobertura ausente. Preço de capa de R$ 99,00 não é somado. Teste de resumo existente ampliado: física de R$ 999,00, digital de R$ 500,00 e terceira edição sem suporte/preço produzem R$ 1.499,00, física 1, digital 1, desconhecida 1. Mesmo com quatro cópias e aquisições de R$ 30,30, referência permanece 1.499,00; após mudanças de cópias, aquisição fica 35,35 e referência continua inalterada. Dados do outro proprietário não entram nas contagens/soma física. Unitários verificam zero/nulo, valor acima do limite individual, suporte e cobertura, dono exato.

Navegador: prévia do HTML realmente renderizado em desktop padrão (1280 pixels) e 320 × 780, com seis cartões em três/uma coluna, duas fontes financeiras distintas e avisos legíveis. Capturas inspecionadas; documento 320/320, sem rolagem horizontal. Sem POST pela prévia, que não substitui teste da sessão real. Viewport restaurado, aba fechada e servidor temporário encerrado.

`git diff --check`: sem erros. Sem migration, backfill ou alterações em data/; aplicação real não reiniciada.

## Limites e pendências

Total principal é a soma das referências pagas nas edições, uma por cadastro; não pretende representar gasto exato com todas as cópias nem avaliação de mercado. Ausências não são inferidas e fontes não são somadas entre si. Inicial já aberta precisa de recarregamento. Reiniciar IntelliJ para carregar consulta/DTO/template novos. T016 amplo, T017–T020 e auditoria WCAG/performance em massa permanecem pendentes.
