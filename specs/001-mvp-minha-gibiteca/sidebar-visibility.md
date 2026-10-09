# Mostrar ou ocultar a barra lateral inteira

2026-10-09. Constituição mantida; sem mudanças de domínio, banco, autenticação ou navegação existente.

## Specify / clarify — SID-V001–004

- SID-V001: botão independente da barra lateral permite **Ocultar menu** e **Mostrar menu** em todas as páginas privadas que compartilham o fragmento. O conteúdo aproveita o espaço disponível quando a barra está oculta.
- SID-V002: preferência de visibilidade persiste neste navegador entre páginas/recarregamentos por localStorage, sem dados particulares ou requisições ao servidor. Navegadores sem armazenamento continuam funcionando; padrão inicial visível.
- SID-V003: botão acessível por teclado, nome em pt-BR, aria-controls/aria-expanded coerentes, foco visível e alvo mínimo 44 px. A barra oculta sai da árvore de acessibilidade e da ordem de Tab; botão continua disponível para reabri-la. Não é modal/overlay.
- SID-V004: desktop/celular, grupos details/summary preservados. Sem JavaScript, barra permanece visível e o botão inoperante não é exibido. Não ocultar conteúdo ou sobrepor controles da página.

## Plan / tasks

- [ ] SID-VT1: fragmento compartilhado com controle fora do aside e IDs únicos; JS vanilla defer, hidden, estado ARIA e armazenamento tolerante a falha.
- [ ] SID-VT2: CSS de layout com/sem barra, toolbar e responsividade.
- [ ] SID-VT3: testes MockMvc de controle/atributos/asset nas rotas existentes, build completo e teste real de mostrar/ocultar/persistência/teclado em desktop e 320 px.

## Analyze

Fragmento deve renderizar controle + aside como irmãos: a toolbar ocupa a linha inteira do grid, aside/conteúdo permanecem na linha seguinte. hidden precisa prevalecer sobre display:flex da sidebar. No celular layout já é block; mesma toolbar fica visível e o aside oculto não deixa espaço. Script externo compartilhado não faz chamadas HTTP de mutação, usa textContent e não precisa de nova dependência/build. IDs não duplicados, nenhuma alteração de links, CSRF, PRG ou propriedade. data/ não rastreado preservado. Spec Kit não executado; revisão SDD com artefatos equivalentes.

## Validação do planejamento

Fragmentos navigation/head, CSS e testes de navegação inspecionados; Git sem mudanças rastreadas antes desta tarefa. Planejamento registrado antes do código.
