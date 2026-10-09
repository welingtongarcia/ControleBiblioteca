# Verificação — controle e progresso de leitura

Data: 2026-10-09. T026, FR-042–044, AC-014.

## Decisões e arquivos

Constituição revista; spec/plan/tasks/analysis atualizados antes da implementação. Clarificação comunicada: coleção específica significa série editorial; denominador somente edições cadastradas, não checklist ou unidades físicas. Geral inclui digitais e sem série. Sem inferir leitura de posse. Contagens financeiras existentes preservadas.

- V13__add_edition_reading_status.sql: coluna boolean not null default false em editions; anteriores começam Não li, sem eliminar dados.
- Edition.java/EditionView.java: estado por edição; alteração explícita, metadados não zeram leitura.
- collection/application/ReadingService.java/ReadingProgress.java: transação, autorização por dono, lock do proprietário compatível com alterações bibliográficas; estado explícito idempotente; percentual decimal HALF_UP com uma casa e rótulo pt-BR.
- collection/infrastructure/ReadingQueries.java: agregações no banco, dono obrigatório; geral e lote das séries da página; sem N+1, sem somar exemplares. ReadingController.java: POST/CSRF, parâmetro explícito e PRG com contexto permitido.
- DashboardController, ComicSeriesController e ChecklistController: DTO de progresso no modelo, sem nova exposição JPA. Lista de séries recebe agregados de até vinte IDs, checklist/faltantes autoriza a série antes da consulta.
- fragments/reading.html e templates de catálogo/detalhe, séries, checklist e inicial: Já li/Não li e ação inversa, barras nativas com rótulos acessíveis, contagem/percentuais, links para marcar leituras e explicação do denominador. dashboard.css: layout flexível e alvos mínimos de 44 pixels.
- ReadingTest.java e MinhaGibitecaApplicationTest.java: unidade, MVC/PostgreSQL, upgrade e prévia descartável target/ui-reading.

## Evidências executadas

Compilação inicial `./mvnw test -q -DskipTests`: código 0 (não equivale a execução de testes).

`./mvnw clean verify -q`: código 0, JAR gerado; **83 testes, zero falhas/erros/ignorados**: 36 integrados, 22 navegação, 10 checklist, 8 exemplares, 4 leitura e 3 resumo. PostgreSQL 16 em Testcontainers, schema limpo V1–V13. Upgrade de V7 até V13 preserva edição anterior e is_read=false.

Novo fluxo MVC: série com três edições, outra vazia, digital sem série, duas cópias da mesma edição e item de checklist sem edição. POST sem CSRF retorna 403; outro usuário recebe 404; estado ausente 400. Marcar duas vezes a mesma edição → série 33,3% (1/3), geral 25,0% (1/4), sem multiplicação por cópias/checklist. Retorno ao catálogo preserva q/página. Alterar metadados/série mantém leitura, série antiga vira 0/2 e nova 1/1. Desmarcar e excluir digital atualizam progresso; retorno externo ignorado. Consulta de série alheia rejeitada. Outro teste usa 21 edições (sete lidas) e 21 séries, confirma 33,3% e total além da página; outro dono não entra nos agregados. Páginas seguintes de catálogo/séries renderizam. Unitários validam 0/0, 1/3, 2/3, 100%, reversibilidade/idempotência, preservação após metadados e autorização antes de gravar.

Prévia de HTML efetivamente renderizado: desktop padrão de 1280 pixels conferido nas séries; navegação catálogo → detalhe → início → séries conferida. Celular 320 × 780 com capturas completas da inicial, séries e catálogo: porcentagens/controles legíveis, barras e ações reorganizadas, documento 320/320 sem rolagem horizontal. Detalhe também conferido na árvore de acessibilidade. Pular ao conteúdo por teclado focou main-content. Nenhum POST submetido na prévia estática; comportamento de gravação executado via MockMvc. Viewport restaurado, aba fechada e servidor temporário encerrado.

`git diff --check`: sem erros. data/ preservado e não versionado. Nenhuma edição do banco real; aplicação em uso não reiniciada.

## Limites e pendências

Não mede leitura dos números ainda não cadastrados nem usa checklist como denominador. Não representa progresso de páginas/capítulos nem datas/histórico de releitura. Aba já aberta requer recarregamento; sem polling/push. Desempenho em massa, E2E da sessão real e auditoria WCAG completa continuam em T018/T020; demais indicadores T016 e entrega T017–T020 pendentes. Reiniciar IntelliJ aplica V13 automaticamente via Flyway e carrega os controles/indicadores.
