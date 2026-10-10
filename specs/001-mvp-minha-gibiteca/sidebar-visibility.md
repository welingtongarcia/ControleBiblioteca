# Mostrar ou ocultar a barra lateral inteira

2026-10-09. Constituição mantida; sem mudanças de domínio, banco, autenticação ou navegação existente.

## Specify / clarify — SID-V001–004

- SID-V001: botão independente da barra lateral permite **Ocultar menu** e **Mostrar menu** em todas as páginas privadas que compartilham o fragmento. O conteúdo aproveita o espaço disponível quando a barra está oculta.
- SID-V002: preferência de visibilidade persiste neste navegador entre páginas/recarregamentos por localStorage, sem dados particulares ou requisições ao servidor. Navegadores sem armazenamento continuam funcionando; padrão inicial visível.
- SID-V003: botão acessível por teclado, nome em pt-BR, aria-controls/aria-expanded coerentes, foco visível e alvo mínimo 44 px. A barra oculta sai da árvore de acessibilidade e da ordem de Tab; botão continua disponível para reabri-la. Não é modal/overlay.
- SID-V004: desktop/celular, grupos details/summary preservados. Sem JavaScript, barra permanece visível e o botão inoperante não é exibido. Não ocultar conteúdo ou sobrepor controles da página.

## Plan / tasks

- [x] SID-VT1: fragmento compartilhado com controle fora do aside e IDs únicos; JS vanilla defer, hidden, estado ARIA e armazenamento tolerante a falha.
- [x] SID-VT2: CSS de layout com/sem barra, toolbar e responsividade.
- [x] SID-VT3: testes MockMvc de controle/atributos/asset nas rotas existentes, build completo e teste real de mostrar/ocultar/persistência/teclado em desktop e 320 px.

## Analyze

Fragmento deve renderizar controle + aside como irmãos: a toolbar ocupa a linha inteira do grid, aside/conteúdo permanecem na linha seguinte. hidden precisa prevalecer sobre display:flex da sidebar. No celular layout já é block; mesma toolbar fica visível e o aside oculto não deixa espaço. Script externo compartilhado não faz chamadas HTTP de mutação, usa textContent e não precisa de nova dependência/build. IDs não duplicados, nenhuma alteração de links, CSRF, PRG ou propriedade. data/ não rastreado preservado. Spec Kit não executado; revisão SDD com artefatos equivalentes.

## Validação do planejamento

Fragmentos navigation/head, CSS e testes de navegação inspecionados; Git sem mudanças rastreadas antes desta tarefa. Planejamento registrado antes do código.

## Verificação

- Planejamento commit 105d085. Arquivos: navigation.html, head.html, dashboard/index.html, dashboard.css, novo js/sidebar.js, MinhaGibitecaApplicationTest, docs/installation.md e este registro.
- JS vanilla externo defer; toolbar independente do aside, preferência booleana local sem informações privadas, falha de armazenamento tolerada, sidebar.hidden e aria-expanded coerentes. Sem JS controle permanece hidden e barra visível. Layout desktop libera coluna; mobile mantém conteúdo em fluxo normal, sem overlay.
- Primeira rodada clean verify falhou (144 testes, uma falha): inicial não usava fragmento head e não carregava JS. Página inicial migrada para head compartilhado. Asserção de defer também tornada independente da ordem dos atributos. Rodada intermediária interrompida para corrigir a inicial; não contabilizada como sucesso.
- Rodada final `./mvnw clean verify -q`: exit 0; 144 testes, 0 falhas/erros/ignorados, PostgreSQL 16/Testcontainers e JAR. Teste de navegação cobre IDs únicos, controle fora do aside, estado inicial visível, script defer, asset público e grupos/links/CSRF existentes em todas as rotas enumeradas. `git diff --check`: exit 0.
- Prévia estática do HTML real MockMvc em target/ui-navigation: desktop mostrar/ocultar comprovados; aside sai da árvore AX, mainLeft=0 quando oculto; Tab do botão chega ao dashboard-query sem visitar links ocultos; recarregamento mantém oculto. Em 320×780, navegação da inicial para catálogo mantém oculto; Enter mostra o menu com foco visível e estado expandido. Documento 305 px/viewport 320 px sem overflow. Nenhum POST de negócio, somente preferência no origin descartável da prévia. Sem auditoria completa WCAG ou teste de armazenamento bloqueado no navegador (fallback implementado).
- Sem banco/deploy/reinício da aplicação IntelliJ; data/ preservado. Reiniciar aplicação para carregar templates/asset. Preferência de grupos continua baseada na página ativa, independente da visibilidade inteira.
