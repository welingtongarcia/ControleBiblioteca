# Ações das séries como botões — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Escopo restrito às cinco ações de cada série na listagem.

- SAB-001: Edições e leituras, Checklist, Faltantes e Editar têm aparência de botão secundário, mas mantêm semântica de link e URLs atuais. Excluir permanece botão POST com CSRF e tem destaque visual de perigo.
- SAB-002: alinhamento, quebra de linha entre botões (não dentro dos rótulos), foco visível e alvo mínimo 44 px; sem overflow horizontal em 320/390/768/1024/1440 px. Não alterar regras de exclusão, leitura ou checklist.

## Plan → tasks → analyze

- [x] SAB-T1 Aplicar classes específicas no template e CSS compartilhado, sem modificar ações/controllers.
- [x] SAB-T2 Validar HTML, navegação e exclusão em testes existentes; conferir aparência e responsividade no navegador.

Preservar anchors para navegação, sem role=button ou JavaScript artificial. Manter formulário de exclusão e retorno contextual. Estilos limitados às ações da listagem de séries.

## Implement → verificação

- `templates/series/list.html`: quatro links com classe de botão e botão POST Excluir com classe de perigo. URLs, returnTo e CSRF preservados (SAB-001).
- `static/css/dashboard.css`: fundo, borda, padding, hover e destaque de perigo limitados a `.series-action-buttons`; rótulos não quebram e grupo pode distribuir ações em linhas (SAB-002). `MinhaGibitecaApplicationTest.java`: asserções das quatro classes de link e do botão/formulário de exclusão.
- Maven: executados `sharesNavigationAcrossPrivatePagesAndLinksChildrenToTheirOwnedParents`, `managesSeriesOnlyWithPublishersFromTheSameOwner`, `keepsChecklistMissingContextAndRejectsUnsafeReturnsWithoutBypassingOwnership` e `blocksPublisherReferencedOnlyBySeriesAndEnforcesDeletionSecurity` via `./mvnw -q -Dtest=MinhaGibitecaApplicationTest#sharesNavigationAcrossPrivatePagesAndLinksChildrenToTheirOwnedParents+managesSeriesOnlyWithPublishersFromTheSameOwner+keepsChecklistMissingContextAndRejectsUnsafeReturnsWithoutBypassingOwnership+blocksPublisherReferencedOnlyBySeriesAndEnforcesDeletionSecurity test`: saída 0, quatro testes sem falhas/erros/ignorados, PostgreSQL Testcontainers. Suíte completa não repetida neste ajuste visual.
- QA em prévia renderizada pelo MockMvc: cinco larguras 320/390/768/1024/1440 px; cinco ações com 44 px de altura, borda de 1 px, texto em linha única e sem overflow horizontal. Captura desktop inspecionada; Tab de Edições e leituras para Checklist conserva outline de 3 px. Nenhuma exclusão realizada na prévia/dados reais.
- `git diff --check`: sem erros. Planejamento registrado em `6cf8aa1` antes da implementação. Servidor temporário encerrado e viewport restaurado. Sem deploy/reinicialização da aplicação; Ctrl+F5 ou reinício se necessário para recarregar recursos. `data/` preservado, sem mudanças de domínio/banco ou pendências funcionais identificadas.
