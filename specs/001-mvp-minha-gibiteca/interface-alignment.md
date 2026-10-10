# Revisão global de alinhamento — 2026-10-09

## Constitution / specify / clarify

Constituição 1.0.0 mantida. Escopo: apresentação CSS/HTML de todas as telas existentes; não redesenhar o produto, alterar regras, banco, integrações ou dados reais.

- UIA-001: bordas de campos, rótulos, ajuda e erros com espaçamento consistente. Grupos de duas colunas alinham os controles no topo, sem deslocá-los conforme a quantidade de texto de ajuda. Textarea continua redimensionável verticalmente; checkbox não recebe dimensões de input textual.
- UIA-002: cabeçalhos, ações, listas, detalhes, cartões e filtros respeitam a largura disponível; nomes longos quebram sem sobreposição. Ações primárias/secundárias e confirmação possuem área utilizável de pelo menos 44px, mantendo foco visível, sem remover semântica ou CSRF.
- UIA-003: revisão de início, catálogo, financeiro, cadastros novos/edição de edição, editora, série, obra, criador, personagem, localização, exemplar e checklist; detalhes, faltantes, confirmações, login e erros. Layout responsivo em 320/390/768/1024/1440px, com sidebar visível/oculta. Tabelas podem rolar dentro do próprio painel; página não deve ter overflow horizontal.
- UIA-004: manter identidade visual e termos pt-BR, relações, campos/name/id/action, validação, interrupor de leitura, menus e preferência da sidebar. Não ocultar conteúdo para mascarar overflow.

## Plan / tasks

- [x] UIA-T1 Auditar templates/CSS e reproduzir com páginas renderizadas pelo MockMvc e PostgreSQL isolado, incluindo nomes longos e estados vazios/erros.
- [x] UIA-T2 Corrigir regras comuns (campos, medidas, espaçamento, ações, largura/encolhimento), e exceções verificadas para filtros, detalhes, métricas, listas e formulários complexos.
- [x] UIA-T3 Verificar páginas renderizadas em navegador nas larguras especificadas, alinhamento de controles, overflow, sidebar e foco. Executar regressão Maven e registrar resultados/limitações reais.

## Analyze

`.field` usa grid sem `align-content:start`, permitindo redistribuir espaço em linhas com ajudas diferentes. Inputs checkbox herdam altura mínima/padding de input textual; `.link-button` e `.button-link` têm dimensões diferentes. Formulários estreitos e página usam limites de largura/padding sobrepostos em dois CSS. Listas têm ações largas sem reservar espaço/encolhimento. Priorizar regras compartilhadas em `main.css` e ajustes específicos em `dashboard.css`, preservando checkboxes especiais de associações. Validar usando renderização Thymeleaf real, não HTML inventado; exportação opcional de fixtures somente em `target/`, sem credenciais/dados reais. Sem novo requisito Node/frontend; navegador apenas instrumento de QA.

## Implement / verificação

### Arquivos e requisitos

- `src/main/resources/static/css/main.css` (UIA-001/002/004): campos alinhados no topo e com largura integral; ajuda/erros sem margens duplicadas; checkbox/radio com dimensões próprias e labels de confirmação flexíveis; botões e links de ação com altura mínima de 44px; cabeçalhos/listas quebram de forma controlada; pares de detalhes podem encolher; espaçamento da busca por título; ações dos formulários à direita no desktop e à esquerda no celular.
- `src/main/resources/static/css/dashboard.css` (UIA-002/003): padding/limite comum para páginas gerais e início, mantendo limites próprios de formulários/financeiro; rótulos de filtros financeiros reservam espaço uniforme; valores das tabelas alinhados à direita, células no topo; títulos dos cartões reservam altura uniforme e links do início ficam alinhados no rodapé.
- `src/test/java/br/com/minhagibiteca/MinhaGibitecaApplicationTest.java` (UIA-003/004): novo teste `rendersAllInterfaceLayoutsWithLongNamesAndValidationErrors`, 48 páginas/estados renderizados por controllers e Thymeleaf reais. Exportação opcional `-Dui.review.export=true` gera HTML/CSS/JS fictícios em `target/ui-review`, para inspeção local sem autenticar no banco real. Limpeza de personagens adicionada ao setup do PostgreSQL de teste para isolamento entre métodos.
- Nenhum template, endpoint, campo, integração, esquema, dependência de build ou dado real alterado. `data/` preservado.

### Evidência executada

- Antes: em 1440px, Valor pago com topo 1656,94px e Data da compra 1619,95px: **36,98px de desalinhamento**. Em 320px, login/login com erro tinham `scrollWidth=331` e `clientWidth=305`, transbordando 26px.
- Depois: controles de Valor pago/Data da compra com topo idêntico (1591,08px na reprodução inicial corrigida). Conferência final dos pares na identificação, valores, vínculos, busca de metadados, formulário com erros, anos da série e filtros financeiros: **diferença de topo 0px** entre controles na mesma linha.
- Navegador: 48 páginas × 320/390/768/1024/1440px × sidebar visível/oculta = **480 verificações de largura**, sem overflow horizontal. Com sidebar visível também foi verificado que inputs/selects/textareas não ultrapassam a viewport. A primeira tentativa de lote oculto excedeu o tempo do navegador; refeita em dois lotes completos, ambos aprovados.
- Inspeção por screenshot de formulário de edição, filtros/resumo financeiro, lista de séries, confirmação e login móvel. Checkbox de confirmação: 18,39 × 18,39px, label com altura de 44px. Enter no botão Mostrar menu expandiu a sidebar; Tab em Aplicar filtros focou Limpar filtros com outline branco sólido de 3px.
- `./mvnw -q -Dui.review.export=true -Dtest=MinhaGibitecaApplicationTest#rendersAllInterfaceLayoutsWithLongNamesAndValidationErrors test`: saída 0, 1 teste aprovado.
- Primeira suíte completa encontrou 24 erros de limpeza de fixtures: o novo personagem impedia remover usuários de teste. Corrigido setup de limpeza após exclusão das edições. `./mvnw verify -q -Dui.review.export=true`: saída 0.
- `./mvnw clean verify -q -Dui.review.export=true` final: saída 0; **149 testes, 0 falhas, 0 erros, 0 ignorados** nos relatórios Surefire. `git diff --check`: saída 0.

### Pendências e limites

Aplicação do IntelliJ não reiniciada; recarregar com Ctrl+F5 para evitar CSS em cache. Testes visuais feitos no navegador Chromium disponível, com fixtures e estados de validação; não constituem certificação WCAG completa nem testes em todos os navegadores/seletores nativos de cada sistema operacional. Sem novas pendências funcionais identificadas nesta revisão.
