# Cadastro de séries e checklist — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Escopo: cadastro/edição de séries, checklist completo/faltantes e cadastro/edição de seus itens. Sem alterar regras de domínio, gerar sequências ou sincronizar posse/leitura automaticamente.

- SC-001: cadastro da série com seções Identificação, Publicação e Observações; campos obrigatórios/opcionais, ajuda, erros associados e ações claras.
- SC-002: checklist com cabeçalho da série, atalhos às edições/edição da série, filtros com indicação atual, quantidade da consulta paginada e estados de posse visualmente distintos com texto explícito. Não confundir quantidade filtrada com total da série, nem posse com leitura.
- SC-003: cadastro de item em painel próprio no checklist completo, campos agrupados, exemplos de números editoriais, estado manual explicado; manter fragmento compartilhado para edição. Faltantes oferece retorno ao cadastro no checklist completo. Estados vazios com orientação, erros preservam rascunho.
- SC-004: pt-BR, teclado/foco, largura responsiva a partir de 320 px, campos/ações utilizáveis. Preservar GET/POST, CSRF, PRG, returnTo, paginação, confirmação de exclusão e isolamento de proprietários. Textos cadastrados escapados.

## Plan → tasks → analyze

- [x] SC-T1 Reorganizar templates de série/checklist e CSS com escopo próprio, sem alterar serviços/controllers/banco.
- [x] SC-T2 Cobrir renderização de seções, filtros, formulário/erros e textos escapados com MockMvc; executar suíte Maven.
- [x] SC-T3 Inspecionar formulários/listas/faltantes/erros em desktop e móvel, incluindo teclado; registrar resultado real.

Reutilizar dados já disponíveis (`series`, `items` Page, `readingProgress`, formulários). Usar totalElements apenas como quantidade da consulta atual; não inferir percentual de completude a partir de uma página. Grid de lista/formulário no desktop, uma coluna em telas estreitas. Não introduzir controles sem backend.

## Implement → verificação

- Arquivos: `templates/series/form.html` (SC-001), `templates/checklist/list.html` e `templates/checklist/form.html` (SC-002/003), `static/css/dashboard.css` (SC-004), `MinhaGibitecaApplicationTest.java` (regressão e prévias). Sem mudanças de controllers/serviços/banco ou dependências.
- Formulários com fieldsets/legends, ajuda, ações Salvar série/Salvar item e erros associados por aria-describedby/aria-invalid. Corrigida apresentação do erro de período: `publicationPeriodValid` é erro de campo, não global; agora aparece junto aos anos. Rascunho preservado.
- Checklist: cabeçalho contextual, atalhos reais, leitura separada, etiquetas textuais de posse, quantidade da consulta e inclusão em painel lateral (uma coluna até 1100 px). Faltantes não exibe formulário e permite voltar à inclusão no checklist completo. Estados vazios não inferem completude da série.
- `./mvnw verify -q`, executado novamente após finalizar o texto da quantidade: saída 0; 162 testes, 0 falhas, 0 erros, 0 ignorados. Novo teste verifica seções, rascunhos/erros, posse, filtragem e conteúdo escapado. Testes existentes verificam PRG, CSRF, exclusão confirmada, contexto, paginação e isolamento com PostgreSQL Testcontainers.
- QA de oito prévias renderizadas por MockMvc em `target/ui-series-checklist`: nova série, editar série, erro de período, checklist vazio/completo/faltantes, editar item e erro de inclusão. 320/390/768/1024/1440 px: 40 combinações sem overflow horizontal ou campos fora do main; campos e botões com altura mínima de 44 px. Capturas desktop e móvel inspecionadas.
- Atalho Adicionar item chega ao título do painel (topo 23,91 px no móvel); Tab do número editorial chega à descrição com outline de 3 px. Erro de período aparece e mantém anos 2020/2019; filtro Faltantes marcado como atual com quantidade 1 e retorno à inclusão correto. Prévia estática não submete dados; fluxos HTTP cobertos por MockMvc.
- `git diff --check`: sem erros. Planejamento registrado no commit `29bde4b` antes da implementação. Servidor temporário encerrado, viewport restaurado. Dados reais/`data/` preservados; não houve deploy ou reinicialização do IntelliJ. Recarregar recursos/reiniciar a aplicação se necessário. Sem pendência funcional identificada no escopo; listagem geral de séries e cadastro de edições não redesenhados nesta alteração.
- Ajuste final apenas da exportação de prévias: incluir home-showcase.css referenciado pelo head compartilhado (sem alterações ao CSS de produção). Teste `presentsGroupedSeriesAndChecklistFormsWithClearPossessionAndValidation` reexecutado isoladamente: saída 0, 1 teste sem falhas/erros/ignorados. QA das 40 combinações repetido com as três folhas carregadas, sem overflow ou campos/botões abaixo de 44 px.
