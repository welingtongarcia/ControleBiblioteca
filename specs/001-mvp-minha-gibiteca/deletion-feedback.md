# Exclusões vinculadas — mensagens em vez de erro 500

2026-10-09. Constituição mantida. Diagnóstico: Mythos possui uma edição vinculada; PostgreSQL registrou violations de editions_publisher_id_fkey na tentativa de exclusão. Nenhum dado real excluído.

## Specify / clarify — DEL-001–004

- DEL-001: bloquear exclusão de editoras com edições/séries, obras e criadores associados a edições; explicar como desvincular antes de excluir, sem cascata ou desvinculação automática.
- DEL-002: cadastros livres continuam excluíveis. Proprietário e CSRF obrigatórios; registros alheios continuam 404 sem revelar vínculos. Consultas de vínculos restritas ao proprietário.
- DEL-003: POST bloqueado usa PRG e flash errorMessage, renderizado com role=alert na listagem. Não mostrar SQL, constraints, UUIDs ou stacktrace ao usuário. Edições, séries, personagens/localizações/exemplares mantêm proteções existentes; tratar também conflito de integridade na exclusão concorrente.
- DEL-004: não alterar banco real, relações ou migrations; salvar/editar e navegação existentes preservados.

## Plan / tasks

- [x] DEL-T1 Consultas exists de referências nos repositórios; serviços verificam ownership antes de vínculo, lançam exceção de domínio/aplicação e executam flush na exclusão.
- [x] DEL-T2 Controllers convertem vínculo e conflito de integridade na fronteira transacional em mensagens pt-BR/PRG; listas exibem erro. Não capturar falhas de integridade globalmente em operações de cadastro/edição.
- [x] DEL-T3 JUnit/Mockito e MockMvc/PostgreSQL: vinculados permanecem intactos, desvinculados excluem, séries/edições e associações não são apagadas, CSRF/ownership, flash HTML e conflito simulado; clean verify.

## Analyze

FKs existentes preservam integridade, mesmo em corrida entre pré-checagem e delete. Capturar DataIntegrityViolationException fora do serviço transacional (controllers) permite rollback completo; não engolir dentro da transação. Flush antecipa o erro antes de retorno normal. Não criar dependência circular PublisherService/ComicSeriesService. Repositórios com SQL parametrizado exists, sem carregamento N+1. Mensagem de integridade na exclusão é conservadora, sem afirmar sucesso ou remover relacionados. Artefatos SDD equivalentes; Spec Kit não executado nesta revisão.

## Implement / verificação — 2026-10-09

- DEL-001/002: `PublisherRepository`, `WorkRepository`, `CreatorRepository` e respectivos serviços consultam referências; `ReferencedCatalogException` bloqueia exclusão. Nenhuma cascata ou alteração de vínculos adicionada.
- DEL-003: controllers de editoras/obras/criadores tratam bloqueio e conflito de integridade; templates das três listas renderizam flash com `role="alert"`. Controllers de edições/séries/personagens/localizações também tratam conflito de integridade na exclusão, mantendo regras anteriores.
- DEL-002/004: `MinhaGibitecaApplicationTest` reproduz editora Mythos com edição, obra/criador associados e editora vinculada somente a série em PostgreSQL isolado. Confirma preservação dos registros, 403 sem CSRF, 404 para outro proprietário, mensagens no HTML e exclusão após desvinculação explícita sem apagar edição/série.
- `CatalogDeletionTest`: Mockito verifica ordem ownership → referência → exclusão/flush e mensagens seguras para conflito simulado nos três controllers.
- Primeira execução específica falhou na compilação por ambiguidade AssertJ/FlashMap; corrigida nos testes. `./mvnw -q -Dtest=CatalogDeletionTest,MinhaGibitecaApplicationTest#explainsReferencedCatalogDeletionAndPreservesEditions+blocksPublisherReferencedOnlyBySeriesAndEnforcesDeletionSecurity test`: saída 0, 4 testes aprovados.
- `./mvnw clean verify -q`: saída 0; relatórios Surefire totalizam **148 testes, 0 falhas, 0 erros, 0 ignorados**, incluindo nova cobertura de CSRF/ownership nos três cadastros. `git diff --check`: saída 0.
- Sem migrations novas, sem escrita no banco real e sem alteração de `data/`. Testes usam PostgreSQL em Testcontainers; aplicação do IntelliJ não foi reiniciada.
- Pendência operacional: reiniciar a aplicação para carregar a correção. Mythos continuará protegida enquanto houver edição/série vinculada. Concorrência coberta por conflito simulado; não foi realizado teste de carga/corrida real nem validação visual em navegador nesta fase.
