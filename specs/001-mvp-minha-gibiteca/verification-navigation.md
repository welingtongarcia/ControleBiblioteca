# Verificação — navegação entre páginas

Data: 2026-10-09. T023, FR-052, AC-013, NFR-001/004.

## Escopo e arquivos

Especificação, plano, tarefas e análise atualizados antes da implementação. Sem mudanças de domínio, dependências, migrations ou dados privados.

- shared/presentation/NavigationContext.java, NavigationAdvice.java e NavigationConfiguration.java: contexto de apresentação, seção ativa, origens locais permitidas, parâmetros limitados e propagação em retornos/PRG. Sem confiança em Referer nem histórico arbitrário. A lista mais próxima tem prioridade; checklist/faltantes retêm modo e página da mesma série.
- shared/presentation/MvcExceptionHandler.java: contexto também nas páginas de erro; catalog/presentation/EditionController.java: identificador da edição no formulário para retorno ao detalhe.
- templates/fragments/navigation.html: menu compartilhado, trilhas, voltar e logout POST/CSRF; fragments/head.html: estilo compartilhado. Templates de dashboard, edições, exemplares, séries, checklist, localizações, editoras, obras, criadores e erros: menu, seção ativa, pais explícitos, cancelamento e contexto nos links/formulários. Login permanece separado.
- static/css/dashboard.css: integração das telas ao layout, trilhas flexíveis, abas selecionadas e formulários responsivos.
- MinhaGibitecaApplicationTest.java e shared/presentation/NavigationContextTest.java: testes de renderização, contexto, autorização e segurança dos destinos. Prévia descartável em target/ui-navigation com dados isolados de teste.

## Evidências executadas

`./mvnw clean verify -q`: código 0 e JAR gerado. **71 testes, zero falhas, erros ou ignorados**: 31 integrados PostgreSQL/MockMvc, 22 de contexto de navegação, 10 de checklist e 8 de exemplares. Testcontainers PostgreSQL 16; migrations V1–V12 e atualização de schema continuam passando.

Novos testes integrados renderizam 28 rotas privadas, verificam os sete destinos do menu, seção ativa única, trilhas, pular ao conteúdo, logout POST/CSRF e pais dos formulários. Verificam filtros e paginação após validação e gravação, retorno ao modo Faltantes, bloqueio de destinos externos e acesso negado a dados de outro proprietário. Testes unitários cobrem origens inválidas, limites, codificação, parâmetros permitidos, contextPath e ausência de confiança em Referer. Na primeira execução houve perda do atributo navigation nas páginas de erro; corrigida explicitamente no handler e validada na execução limpa final.

Prévia do HTML efetivamente renderizado conferida no navegador: catálogo em desktop padrão (1280 pixels), clique catálogo → detalhe → editar → cancelar → detalhe; menu Séries e checklist → Faltantes → editar item → cancelar → checklist. Capturas de página completa inspecionadas no catálogo e formulário. Em 320 × 780 pixels o menu reorganiza-se, trilhas quebram linhas e campos permanecem acessíveis; largura do documento 320/320, sem rolagem horizontal. Pular para o conteúdo acionado por teclado focou main-content. Tamanho do navegador restaurado e aba temporária fechada.

A prévia estática não processa returnTo: o formulário pré-renderizado do checklist retorna ao checklist padrão, enquanto os testes MockMvc confirmam retorno dinâmico a Faltantes/página de origem. Não foram enviados formulários POST pela prévia. A validação do comportamento de gravação/PRG foi executada no servidor de testes.

`git diff --check`: sem erros.

## Pendências e limites

Não equivale a auditoria WCAG completa ou E2E da sessão real; leitores de tela, demais breakpoints e fluxos de produção permanecem em T018/T020. Retornos são restritos à própria seção, sem pilha ilimitada de histórico. Aplicação do usuário não reiniciada; reiniciar pelo IntelliJ para carregar a mudança. data/ preservado e não incluído no commit. T016–T020 continuam pendentes conforme tasks.md.
