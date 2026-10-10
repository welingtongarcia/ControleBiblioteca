# Botão de busca sem quebra — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Correção visual da busca compartilhada pelo Início e Visão geral.

- BUSCA-001: o texto “Buscar” ocupa uma única linha, com largura suficiente e sem compressão pelo campo de pesquisa.
- BUSCA-002: preservar envio GET ao catálogo, label acessível, foco e altura mínima de 44 px. Em 320, 390, 768, 1024 e 1440 px, manter campo e botão dentro da largura disponível, sem overflow horizontal.

## Plan → tasks → analyze

- [ ] BB-T1 Ajustar CSS compartilhado de `.dashboard-search`: impedir encolhimento/quebra do botão e permitir encolhimento do input.
- [ ] BB-T2 Conferir páginas renderizadas em desktop/móvel e teste MVC de navegação, registrando evidências.

O campo tem `width:100%` em um container flex; o botão da Visão geral pode encolher e herda `overflow-wrap:anywhere` do conteúdo. O Início possui uma proteção local de flex-shrink em home-showcase.css. Centralizar a garantia no dashboard.css, sem alterar templates, rotas ou a regra global para textos longos.
