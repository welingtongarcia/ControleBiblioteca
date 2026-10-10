# Revisão global de alinhamento — 2026-10-09

## Constitution / specify / clarify

Constituição 1.0.0 mantida. Escopo: apresentação CSS/HTML de todas as telas existentes; não redesenhar o produto, alterar regras, banco, integrações ou dados reais.

- UIA-001: bordas de campos, rótulos, ajuda e erros com espaçamento consistente. Grupos de duas colunas alinham os controles no topo, sem deslocá-los conforme a quantidade de texto de ajuda. Textarea continua redimensionável verticalmente; checkbox não recebe dimensões de input textual.
- UIA-002: cabeçalhos, ações, listas, detalhes, cartões e filtros respeitam a largura disponível; nomes longos quebram sem sobreposição. Ações primárias/secundárias e confirmação possuem área utilizável de pelo menos 44px, mantendo foco visível, sem remover semântica ou CSRF.
- UIA-003: revisão de início, catálogo, financeiro, cadastros novos/edição de edição, editora, série, obra, criador, personagem, localização, exemplar e checklist; detalhes, faltantes, confirmações, login e erros. Layout responsivo em 320/390/768/1024/1440px, com sidebar visível/oculta. Tabelas podem rolar dentro do próprio painel; página não deve ter overflow horizontal.
- UIA-004: manter identidade visual e termos pt-BR, relações, campos/name/id/action, validação, interrupor de leitura, menus e preferência da sidebar. Não ocultar conteúdo para mascarar overflow.

## Plan / tasks

- [ ] UIA-T1 Auditar templates/CSS e reproduzir com páginas renderizadas pelo MockMvc e PostgreSQL isolado, incluindo nomes longos e estados vazios/erros.
- [ ] UIA-T2 Corrigir regras comuns (campos, medidas, espaçamento, ações, largura/encolhimento), e exceções verificadas para filtros, detalhes, métricas, listas e formulários complexos.
- [ ] UIA-T3 Verificar páginas renderizadas em navegador nas larguras especificadas, alinhamento de controles, overflow, sidebar e foco. Executar regressão Maven e registrar resultados/limitações reais.

## Analyze

`.field` usa grid sem `align-content:start`, permitindo redistribuir espaço em linhas com ajudas diferentes. Inputs checkbox herdam altura mínima/padding de input textual; `.link-button` e `.button-link` têm dimensões diferentes. Formulários estreitos e página usam limites de largura/padding sobrepostos em dois CSS. Listas têm ações largas sem reservar espaço/encolhimento. Priorizar regras compartilhadas em `main.css` e ajustes específicos em `dashboard.css`, preservando checkboxes especiais de associações. Validar usando renderização Thymeleaf real, não HTML inventado; exportação opcional de fixtures somente em `target/`, sem credenciais/dados reais. Sem novo requisito Node/frontend; navegador apenas instrumento de QA.
