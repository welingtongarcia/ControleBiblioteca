# Verificação T032 — interruptor de leitura

2026-10-09. FR-048/AC-020; regressão AC-014/013.

## Arquivos

spec.md, plan.md, tasks.md e analysis.md atualizados antes da implementação. Fragment fragments/reading.html substitui botão textual por submit com role=switch, nome estável por edição, aria-checked atual, indicador visual e texto Já li/Não li. CSS dashboard.css define aparência e alvo 44px; main.css remove regras antigas conflitantes do cartão. Teste adicional em MinhaGibitecaApplicationTest.java cobre estados/retornos e produz fixtures sintéticas target/ui-reading-switch.

## Execução e resultados

- ./mvnw clean verify -q: código 0; 116 testes, zero falhas/erros/skips; PostgreSQL Testcontainers/MockMvc/Thymeleaf e geração de JAR.
- Catálogo e detalhe renderizam ligado/desligado, nome estável e próximo valor booleano correto. POST com CSRF alterna nos dois sentidos, mantém consulta de origem; GET seguinte comprova persistência.
- Regressões de proprietário/CSRF, atualização de percentuais, idempotência, preços e navegação continuam passando.
- Navegador in-app, HTML realmente renderizado em prévia local com dados de teste: catálogo ligado desktop 1280px (conteúdo 1265px), detalhe desligado 320px (conteúdo 305px), sem overflow horizontal. Interruptor anunciado na árvore de acessibilidade com valores 1/0; foco por Tab visível, altura medida 44px. Aparência do indicador e texto conferida em screenshots. Enter/Espaço usam ativação nativa do botão submit; POST validado por MockMvc, não enviado pela prévia estática.
- Viewport restaurado, aba fechada e servidor temporário encerrado; sem dados reais modificados. git diff --check sem problemas.

## Limites

Sem JavaScript, migração ou novas consultas. Alternância salva via POST/redirect, não muda otimisticamente antes da resposta. Prévia estática não é E2E de produção nem auditoria WCAG integral. Reiniciar aplicação no IntelliJ para carregar recursos atualizados; data/ não versionado preservado.
