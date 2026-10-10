# Exclusões vinculadas — mensagens em vez de erro 500

2026-10-09. Constituição mantida. Diagnóstico: Mythos possui uma edição vinculada; PostgreSQL registrou violations de editions_publisher_id_fkey na tentativa de exclusão. Nenhum dado real excluído.

## Specify / clarify — DEL-001–004

- DEL-001: bloquear exclusão de editoras com edições/séries, obras e criadores associados a edições; explicar como desvincular antes de excluir, sem cascata ou desvinculação automática.
- DEL-002: cadastros livres continuam excluíveis. Proprietário e CSRF obrigatórios; registros alheios continuam 404 sem revelar vínculos. Consultas de vínculos restritas ao proprietário.
- DEL-003: POST bloqueado usa PRG e flash errorMessage, renderizado com role=alert na listagem. Não mostrar SQL, constraints, UUIDs ou stacktrace ao usuário. Edições, séries, personagens/localizações/exemplares mantêm proteções existentes; tratar também conflito de integridade na exclusão concorrente.
- DEL-004: não alterar banco real, relações ou migrations; salvar/editar e navegação existentes preservados.

## Plan / tasks

- [ ] DEL-T1 Consultas exists de referências nos repositórios; serviços verificam ownership antes de vínculo, lançam exceção de domínio/aplicação e executam flush na exclusão.
- [ ] DEL-T2 Controllers convertem vínculo e conflito de integridade na fronteira transacional em mensagens pt-BR/PRG; listas exibem erro. Não capturar falhas de integridade globalmente em operações de cadastro/edição.
- [ ] DEL-T3 JUnit/Mockito e MockMvc/PostgreSQL: vinculados permanecem intactos, desvinculados excluem, séries/edições e associações não são apagadas, CSRF/ownership, flash HTML e conflito simulado; clean verify.

## Analyze

FKs existentes preservam integridade, mesmo em corrida entre pré-checagem e delete. Capturar DataIntegrityViolationException fora do serviço transacional (controllers) permite rollback completo; não engolir dentro da transação. Flush antecipa o erro antes de retorno normal. Não criar dependência circular PublisherService/ComicSeriesService. Repositórios com SQL parametrizado exists, sem carregamento N+1. Mensagem de integridade na exclusão é conservadora, sem afirmar sucesso ou remover relacionados. Artefatos SDD equivalentes; Spec Kit não executado nesta revisão.
