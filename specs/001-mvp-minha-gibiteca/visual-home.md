# Início visual e Visão geral — 2026-10-09

## Constitution → specify → clarify

Constituição mantida. Pedido: transferir o painel atual para a aba **Visão geral** e fazer do **Início** uma vitrine visual inspirada em serviços de streaming. Manter marca própria, pt-BR, monólito MVC/Thymeleaf e dados reais do proprietário; sem frontend separado ou fontes externas.

- VHOME-001: `/` e `/dashboard` exibem o novo Início. `/visao-geral` preserva o painel atual, seus valores, percentuais, explicações e atalhos. Sidebar oferece Início e Visão geral com estados ativos distintos. Nenhuma informação do painel é perdida.
- VHOME-002: Início apresenta uma edição em destaque (não lida mais recente; na ausência, edição mais recente), busca no catálogo e acesso a cadastrar/Visão geral. Fileiras horizontais: Adicionados recentemente, Sua próxima leitura (não lidos), Na sua estante (físicos), Biblioteca digital e Já lidos para revisitar. No máximo 12 edições por fileira; determinismo por data de cadastro/id, sem deduplicar entre categorias diferentes. Não afirmar recomendação personalizada, leitura parcial ou compra recente.
- VHOME-003: usar capas já cadastradas pelo endpoint protegido, lazy loading nas fileiras, proporção de capa consistente e placeholder gráfico CSS com título quando não houver capa. Sem importar imagens nem inventar exemplares. Estado vazio oferece cadastro e catálogo; categorias vazias não criam fileiras inúteis.
- VHOME-004: responsivo (320/390/768/1024/1440px), com menu visível/oculto; fileiras rolam somente dentro da área da vitrine por toque/teclado, sem autoplay. Links com foco visível, títulos acessíveis, status de leitura textual, movimento reduzido e sem página com overflow horizontal. Detalhes/edição/cadastro podem voltar ao Início ou Visão geral por `returnTo` local validado, sem aceitar destinos externos.
- VHOME-005: toda consulta/URL de capa/detalhe exige proprietário e autenticação. Consultas limitadas, sem JPA nos controllers ou N+1, com índices Flyway para filtros/ordenação. Visão geral continua atualizando na próxima consulta após cadastro, edição, exclusão e leitura.

## Plan → tasks

- [ ] VH-T1 DTOs e serviço de vitrine, consultas JDBC parametrizadas com joins restritos ao proprietário e LIMIT 12; índices Flyway de proprietário/suporte/leitura/data.
- [ ] VH-T2 Separar rotas/template do painel e Início; adicionar Visão geral à navegação e retornos locais seguros; criar destaque, fileiras e estados vazios com CSS próprio.
- [ ] VH-T3 JUnit/Mockito, MockMvc e PostgreSQL: seleção/limites/isolamento, rotas privadas, estatísticas preservadas, links de capa/detalhe, retornos seguros, atualização e XSS. Atualizar testes existentes para a nova rota do painel.
- [ ] VH-T4 Renderizar fixtures com/sem capas e dados, verificar responsividade/rolagem/foco/menu no navegador, executar `clean verify` e registrar evidências, pendências e riscos.

## Analyze

O atual `DashboardController` atende ambas as rotas iniciais e agrega métricas. Separar handler da visão geral, mantendo o serviço existente; adicionar serviço de vitrine/DTOs no módulo dashboard. Cinco consultas constantes e limitadas com joins não causam N+1 e não carregam coleções inteiras. Preferir SQL com predicados de enum interno (não texto recebido do usuário). Índices de mídia existentes ordenam título; criar índices específicos para fileiras por data de cadastro. Ordem descendente também por UUID elimina empates. Placeholder e composição do destaque serão CSS, pois os visuais representam capas do próprio catálogo. Roll horizontal nativo acessível dispensa biblioteca/carrossel e JavaScript de build. Não mudar semântica de leitura ou financeiro, não manipular banco real para gerar a vitrine.
