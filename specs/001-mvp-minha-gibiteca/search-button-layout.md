# Botão de busca sem quebra — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Correção visual da busca compartilhada pelo Início e Visão geral.

- BUSCA-001: o texto “Buscar” ocupa uma única linha, com largura suficiente e sem compressão pelo campo de pesquisa.
- BUSCA-002: preservar envio GET ao catálogo, label acessível, foco e altura mínima de 44 px. Em 320, 390, 768, 1024 e 1440 px, manter campo e botão dentro da largura disponível, sem overflow horizontal.

## Plan → tasks → analyze

- [x] BB-T1 Ajustar CSS compartilhado de `.dashboard-search`: impedir encolhimento/quebra do botão e permitir encolhimento do input.
- [x] BB-T2 Conferir páginas renderizadas em desktop/móvel e teste MVC de navegação, registrando evidências.

O campo tem `width:100%` em um container flex; o botão da Visão geral pode encolher e herda `overflow-wrap:anywhere` do conteúdo. O Início possui uma proteção local de flex-shrink em home-showcase.css. Centralizar a garantia no dashboard.css, sem alterar templates, rotas ou a regra global para textos longos.

## Implement → verificação

- Arquivo alterado: `src/main/resources/static/css/dashboard.css`. Botão com flex fixo, white-space nowrap e overflow-wrap normal; input com flexível 1 1 0. BUSCA-001/002 atendidos sem alteração funcional da pesquisa.
- Reprodução no navegador antes: Visão geral em 1024 px, botão com 64,97 px de largura, flex-shrink 1 e texto em duas linhas (Range.getClientRects).
- Depois: Início e Visão geral em 320/390/768/1024/1440 px (dez combinações): texto em uma linha, botão 74,78 × 44 px, input com largura positiva, botão contido no formulário e nenhuma rolagem horizontal da página. Capturas das duas páginas inspecionadas. Tab do input foca Buscar com outline 3 px; formulário permanece GET /edicoes.
- `./mvnw -q -Dtest=MinhaGibitecaApplicationTest#sharesNavigationAcrossPrivatePagesAndLinksChildrenToTheirOwnedParents test`: saída 0, um teste integrado executado, sem falhas/erros/ignorados; renderiza 30 rotas com PostgreSQL Testcontainers. QA utiliza esses HTMLs em servidor estático temporário. Não foi repetida a suíte completa nesta correção CSS.
- `git diff --check`: sem erros. Planejamento registrado em `691b9ec` antes da implementação. Dados reais/`data/` preservados; não houve deploy ou reinício da aplicação do IntelliJ. Se CSS ficar em cache, recarregar com Ctrl+F5; pode ser necessário reiniciar o processo para copiar recursos atualizados. Sem pendência funcional identificada.
