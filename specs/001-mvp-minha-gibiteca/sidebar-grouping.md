# Agrupamento lógico da barra lateral — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Reorganizar os destinos existentes por finalidade, sem criar páginas ou alterar rotas/regras de domínio.

- MENU-001: três grupos, nesta ordem: **Painéis e análises** (Início, Visão geral, Financeiro); **Minha coleção** (Catálogo de edições, Séries e checklist, Localizações); **Cadastros de apoio** (Obras, Criadores, Personagens, Editoras). Separar exploração/indicadores, gestão do acervo e dados vinculáveis às HQs. Sem duplicar links.
- MENU-002: manter grupos recolhíveis com details/summary nativos. Somente o grupo da página atual inicia aberto, inclusive detalhes/formulários/exemplares/checklist. Exatamente um link atual com aria-current=page. Manter ocultar/mostrar menu e Sair independente dos grupos.
- MENU-003: pt-BR, teclado/foco, responsividade e autenticação preservados. Não persistir novos dados nem interferir no retorno contextual.

## Plan → tasks → analyze

- [x] MG-T1 Reorganizar fragmento compartilhado, preservando URLs/estados ativos e IDs dos dois grupos existentes; acrescentar nav-overview.
- [x] MG-T2 Atualizar teste de navegação para checar agrupamento, ordem, unicidade e abertura em todos os módulos; executar Maven e QA desktop/móvel/teclado.

O fragmento já recebe `navigation.key`, incluindo aliases exemplares→edições e checklist→séries. Reutilizar essas regras, sem alterar controllers/NavigationContext. CSS atual atende três grupos e menus responsivos. Não criar “Leitura” ou “Exemplares” como destinos sem página própria; seus fluxos continuam dentro da edição/série.

## Implement → verificação

- Arquivos: `templates/fragments/navigation.html` (MENU-001/002/003), `MinhaGibitecaApplicationTest.java` (MG-T2), esta especificação (rastreabilidade). Sem alterações de CSS, JavaScript, domínio ou banco.
- `./mvnw verify -q`: código de saída 0; relatórios Surefire: 160 testes, 0 falhas, 0 erros, 0 ignorados. Teste compartilhado verifica 30 rotas autenticadas, incluindo formulários, detalhes, exemplares, checklist e Visão geral; confere ordem dos grupos/links, links sem duplicação e grupo atual aberto.
- QA no navegador com HTML renderizado pelos controllers em `target/ui-navigation`, dados de teste e servidor estático temporário: dez módulos em 1440, 390 e 320 px (30 combinações), sem overflow horizontal, exatamente um link atual e apenas seu grupo inicialmente aberto. Capturas desktop/móvel inspecionadas.
- Enter e Espaço abrem grupos; Enter também recolhe. Link do catálogo navega para sua página e abre Minha coleção. Ocultar/mostrar menu mantém funcionamento e estado acessível. Sair permanece independente dos grupos.
- `git diff --check`: sem erros. Planejamento registrado no commit `d1dcdf4` antes da implementação.
- Pendência operacional: não houve reinicialização da aplicação do IntelliJ nem deploy. Se o processo não recarregar templates, reiniciar e atualizar a página para carregar os novos menus. Dados reais e diretório `data/` preservados. Sem riscos novos de persistência/autorização identificados; QA estática não testa submissão de formulários, coberta por MockMvc.
