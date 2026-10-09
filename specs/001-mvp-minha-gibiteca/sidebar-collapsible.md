# Menu lateral recolhível

2026-10-09. Constituição mantida: apresentação MVC/Thymeleaf/CSS própria, sem mudança de dados, autorização ou rotas.

## Specify / clarify

SID-001: os grupos existentes **Sua coleção** e **Dados do catálogo** podem ser abertos/recolhidos individualmente ao clicar no título. Não é um recolhimento da largura de toda a barra lateral.

SID-002: o grupo da página atual começa aberto, mantendo seu link com aria-current; o outro começa recolhido. Cada página recalcula o grupo ativo, sem guardar preferência no navegador nesta entrega.

SID-003: controles nativos details/summary funcionam sem JavaScript, com Enter/Espaço, foco visível, indicador visual de estado e área mínima de 44 px. Links recolhidos não entram na navegação por Tab. Marca/início e Sair permanecem disponíveis fora dos grupos.

SID-004: compartilhar o comportamento em todas as páginas que usam o fragmento de navegação. Manter pt-BR e reflow móvel a 320 px, sem esconder os controles no CSS responsivo.

## Plan / tasks

- [x] SID-T001: envolver grupos do fragmento em details, summary e contêiner dos links, calculando open pelo contexto ativo. SID-001/002.
- [x] SID-T002: CSS de foco, marcador e layout desktop/móvel. SID-003/004.
- [x] SID-T003: testes MVC da abertura inicial em páginas de ambos os grupos, prévia visual/teclado em desktop/320 px, clean verify e evidência/commit. SID-001–004.

## Analyze

Os links são navegação comum, não um widget ARIA menu. details/summary fornece estado expandido nativo; não adicionar aria-expanded fixo que possa ficar desatualizado. Não remover links, relaxar autenticação, criar migrations ou dependências. O CSS móvel existente oculta os rótulos antigos; os novos controles precisam de classe própria e layout explícito para não herdar essa ocultação. data/ local preservado.

## Verificação — 2026-10-09

- Planejamento antes de código: commit `76b43c6`. Arquivos alterados: fragmento `navigation.html`, `dashboard.css`, teste integrado `MinhaGibitecaApplicationTest.java` e este registro.
- Teste MVC existente de navegação ampliado: páginas principais, formulários/detalhes de coleção e catálogo, Financeiro e Personagens renderizam ambos os grupos, somente o grupo ativo com open. Links, aria-current, autenticação e saída POST/CSRF preservados.
- Primeira rodada detectou uma asserção antiga que tratava o primeiro details da página como o bloco explicativo do resumo. Atualizada para identificar especificamente `home-calculation`, preservando o requisito original de ordem do conteúdo. Uma rodada intermediária foi interrompida para aplicar essa correção; não contabilizada como sucesso.
- Rodada final `./mvnw clean verify -q`: exit 0; **133 testes, 0 falhas, 0 erros, 0 ignorados** em Surefire, incluindo PostgreSQL/Testcontainers. `git diff --check`: exit 0.
- Prévia local de HTML realmente produzido por MockMvc: Enter recolhe Sua coleção; Tab vai diretamente ao próximo controle (Dados do catálogo), sem visitar os links ocultos; Espaço abre Dados do catálogo. Estado expandido/recolhido aparece na árvore de acessibilidade, foco visível, Sair continua disponível. Indicador final desenhado em CSS sem acrescentar caractere ao nome acessível.
- QA visual desktop e 320 px: grupos abertos/recolhidos e links responsivos; documento 305 px com viewport 320 px (barra de rolagem), sem overflow horizontal global. Prévia usa dados fictícios, não o banco do usuário; servidor temporário encerrado.
- Sem deploy ou reinício do processo IntelliJ; sem alterações de banco ou dependências. Não foi feita auditoria integral WCAG/leitores de tela. Preferência de abertura não persiste entre páginas: o grupo ativo abre conforme especificado. data/ não versionado preservado.
