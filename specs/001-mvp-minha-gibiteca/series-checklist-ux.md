# Cadastro de séries e checklist — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Escopo: cadastro/edição de séries, checklist completo/faltantes e cadastro/edição de seus itens. Sem alterar regras de domínio, gerar sequências ou sincronizar posse/leitura automaticamente.

- SC-001: cadastro da série com seções Identificação, Publicação e Observações; campos obrigatórios/opcionais, ajuda, erros associados e ações claras.
- SC-002: checklist com cabeçalho da série, atalhos às edições/edição da série, filtros com indicação atual, quantidade da consulta paginada e estados de posse visualmente distintos com texto explícito. Não confundir quantidade filtrada com total da série, nem posse com leitura.
- SC-003: cadastro de item em painel próprio no checklist completo, campos agrupados, exemplos de números editoriais, estado manual explicado; manter fragmento compartilhado para edição. Faltantes oferece retorno ao cadastro no checklist completo. Estados vazios com orientação, erros preservam rascunho.
- SC-004: pt-BR, teclado/foco, largura responsiva a partir de 320 px, campos/ações utilizáveis. Preservar GET/POST, CSRF, PRG, returnTo, paginação, confirmação de exclusão e isolamento de proprietários. Textos cadastrados escapados.

## Plan → tasks → analyze

- [ ] SC-T1 Reorganizar templates de série/checklist e CSS com escopo próprio, sem alterar serviços/controllers/banco.
- [ ] SC-T2 Cobrir renderização de seções, filtros, formulário/erros e textos escapados com MockMvc; executar suíte Maven.
- [ ] SC-T3 Inspecionar formulários/listas/faltantes/erros em desktop e móvel, incluindo teclado; registrar resultado real.

Reutilizar dados já disponíveis (`series`, `items` Page, `readingProgress`, formulários). Usar totalElements apenas como quantidade da consulta atual; não inferir percentual de completude a partir de uma página. Grid de lista/formulário no desktop, uma coluna em telas estreitas. Não introduzir controles sem backend.
