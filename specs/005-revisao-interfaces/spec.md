# Revisão transversal das interfaces — 2026-10-10

## Constitution → specify → clarify

Constituição e AGENTS.md preservados: MVC/Thymeleaf/CSS próprio, pt-BR, autorização por proprietário, CSRF e PRG. Revisar todas as telas existentes; ajustar problemas concretos sem trocar a identidade visual escura/dourada, introduzir framework ou alterar dados/regras financeiras e de leitura. Nenhuma operação em cadastros reais.

- UI-001: consistência de ações primárias/secundárias/destrutivas, campos, painéis, buscas e paginação; alvos de interação confortáveis, texto dos botões sem quebra dentro de palavras. Manter semântica nativa e teclado, foco visível, navegação/sidebar existentes. Diferenciar exclusão de salvar e pedir confirmação visual antes dos POSTs de exclusão direta nas listas de apoio/séries, usando disclosure nativo sem depender de JS. Endpoints e bloqueios de vínculos permanecem iguais.
- UI-002: formulários de editoras, obras, criadores, personagens, locais, exemplares, séries e checklist com resumo de validação e associação correta das mensagens aos campos. Não referenciar IDs de mensagens inexistentes por aria-describedby; sinalizar aria-invalid somente no erro. Não pintar campos obrigatórios vazios como erro antes da interação. Não enfraquecer validação do servidor. Preservar valores após erro e manter campos obrigatórios explícitos.
- UI-003: hierarquia e contexto nos cadastros/listas, descrições curtas quando faltam; detalhes da edição com seções legíveis e formulários de leitura/capa sem largura excessiva; ajuda de upload associada. Listas vazias/filtradas não devem confundir ausência de resultado com ausência de cadastros.
- UI-004: responsividade e acessibilidade básica em todas as famílias: login; Início/banner; Visão geral; catálogo/nova/editar/detalhe; editoras/séries/obras/criadores/personagens; locais/exemplares; checklist/faltantes; financeiro; relatórios; backup; confirmações e erros. Conferir 320/390/768/1440 px, textos longos, formulários inválidos e conta vazia. Evitar overflow de página; tabela financeira pode ter rolagem própria acessível por teclado. Respeitar prefers-reduced-motion. Auditoria básica não equivale a certificação WCAG completa.
- UI-005: regressão MockMvc/PostgreSQL Testcontainers e HTML renderizado: rotas/templates, pt-BR, títulos, labels, IDs únicos, aria-describedby existente, CSRF, erros e confirmações. QA no navegador com dados sintéticos e suíte Maven. Não declarar validação visual de estados que não foram conferidos.

## Plan → tasks → analyze

- [ ] T1 Auditar templates/CSS e renderizar todas as rotas/estados existentes com fixtures isoladas; registrar achados.
- [ ] T2 Melhorar componentes compartilhados e telas/formulários com problemas; preservar regras e segurança.
- [ ] T3 Testes estruturais/validação, QA responsivo no navegador, clean verify e relatório/commit.

Achados iniciais: vários formulários de apoio não têm resumo nem aria-invalid; IDs de erros condicionais são referenciados mesmo quando ausentes. CSS usa `:invalid:not(:placeholder-shown)` e marca campos novos sem placeholder em vermelho. Ações similares alternam entre links sublinhados, links sem moldura e botões; exclusões diretas de editoras/obras/criadores/séries podem ocorrer com um clique. Formulários simples não têm organização visual dos formulários de edição/série. Detalhe de edição concentra blocos de leitura e upload sem painel/contexto equivalente. Revisão de renderização e responsividade determinará ajustes adicionais, sem redesenho arbitrário.
