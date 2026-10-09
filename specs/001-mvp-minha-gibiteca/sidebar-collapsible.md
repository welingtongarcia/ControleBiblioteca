# Menu lateral recolhível

2026-10-09. Constituição mantida: apresentação MVC/Thymeleaf/CSS própria, sem mudança de dados, autorização ou rotas.

## Specify / clarify

SID-001: os grupos existentes **Sua coleção** e **Dados do catálogo** podem ser abertos/recolhidos individualmente ao clicar no título. Não é um recolhimento da largura de toda a barra lateral.

SID-002: o grupo da página atual começa aberto, mantendo seu link com aria-current; o outro começa recolhido. Cada página recalcula o grupo ativo, sem guardar preferência no navegador nesta entrega.

SID-003: controles nativos details/summary funcionam sem JavaScript, com Enter/Espaço, foco visível, indicador visual de estado e área mínima de 44 px. Links recolhidos não entram na navegação por Tab. Marca/início e Sair permanecem disponíveis fora dos grupos.

SID-004: compartilhar o comportamento em todas as páginas que usam o fragmento de navegação. Manter pt-BR e reflow móvel a 320 px, sem esconder os controles no CSS responsivo.

## Plan / tasks

- [ ] SID-T001: envolver grupos do fragmento em details, summary e contêiner dos links, calculando open pelo contexto ativo. SID-001/002.
- [ ] SID-T002: CSS de foco, marcador e layout desktop/móvel. SID-003/004.
- [ ] SID-T003: testes MVC da abertura inicial em páginas de ambos os grupos, prévia visual/teclado em desktop/320 px, clean verify e evidência/commit. SID-001–004.

## Analyze

Os links são navegação comum, não um widget ARIA menu. details/summary fornece estado expandido nativo; não adicionar aria-expanded fixo que possa ficar desatualizado. Não remover links, relaxar autenticação, criar migrations ou dependências. O CSS móvel existente oculta os rótulos antigos; os novos controles precisam de classe própria e layout explícito para não herdar essa ocultação. data/ local preservado.
