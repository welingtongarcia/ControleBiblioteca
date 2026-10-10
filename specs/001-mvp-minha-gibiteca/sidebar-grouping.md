# Agrupamento lógico da barra lateral — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Reorganizar os destinos existentes por finalidade, sem criar páginas ou alterar rotas/regras de domínio.

- MENU-001: três grupos, nesta ordem: **Painéis e análises** (Início, Visão geral, Financeiro); **Minha coleção** (Catálogo de edições, Séries e checklist, Localizações); **Cadastros de apoio** (Obras, Criadores, Personagens, Editoras). Separar exploração/indicadores, gestão do acervo e dados vinculáveis às HQs. Sem duplicar links.
- MENU-002: manter grupos recolhíveis com details/summary nativos. Somente o grupo da página atual inicia aberto, inclusive detalhes/formulários/exemplares/checklist. Exatamente um link atual com aria-current=page. Manter ocultar/mostrar menu e Sair independente dos grupos.
- MENU-003: pt-BR, teclado/foco, responsividade e autenticação preservados. Não persistir novos dados nem interferir no retorno contextual.

## Plan → tasks → analyze

- [ ] MG-T1 Reorganizar fragmento compartilhado, preservando URLs/estados ativos e IDs dos dois grupos existentes; acrescentar nav-overview.
- [ ] MG-T2 Atualizar teste de navegação para checar agrupamento, ordem, unicidade e abertura em todos os módulos; executar Maven e QA desktop/móvel/teclado.

O fragmento já recebe `navigation.key`, incluindo aliases exemplares→edições e checklist→séries. Reutilizar essas regras, sem alterar controllers/NavigationContext. CSS atual atende três grupos e menus responsivos. Não criar “Leitura” ou “Exemplares” como destinos sem página própria; seus fluxos continuam dentro da edição/série.
