# Plano técnico — MVP 1.0

## Arquitetura

Monólito modular Spring Boot 3.x compatível com Java 21, Maven e PostgreSQL. A versão exata do Spring Boot será selecionada na Fase 1 dentre versões estáveis disponíveis e registrada no ADR correspondente. A aplicação terá módulos `auth`, `catalog`, `collection`, `inventory`, `dashboard` e `shared`; cada módulo separa `presentation` (MVC/form/view model), `application` (casos de uso), `domain` (regras) e `infrastructure` (JPA/arquivo/configuração).

Fluxo padrão: controller MVC → form DTO validado → serviço de aplicação → repositório/domain → DTO de view → template Thymeleaf. Controllers não recebem entidades JPA nem retornam entidades a telas. A aplicação usa sessão Spring Security, POST/Redirect/GET, flash messages e CSRF para todas as mutações.

## Modelo e persistência

### T025 — correção do resumo segundo o cadastro (2026-10-09)

Estender DTO/consulta agregada de dashboard com count filtrado por media_type, count reference_paid_price e sum reference_paid_price. Agregados independentes de edições e exemplares evitam multiplicação; SQL parametrizado por dono, uma consulta, índices existentes. Indicador principal usa referências das edições; aquisição física fica em linha secundária e seu valor não é somado às referências. Cartões físicos/digitais explicitam que são edições, não unidades. Suporte desconhecido e cobertura informados. Sem migration/backfill/cache. Testes de PostgreSQL/MockMvc com cadastro sem exemplares, físico/digital/desconhecido, zero/nulo, edição por POST e isolamento; unitários de formatação e prévia desktop/celular.

### T024 — resumo financeiro e quantitativo (2026-10-09)

DashboardController resolve CurrentAuthenticatedUser e usa DashboardService read-only → DashboardQueries JDBC → DTO CollectionSummary. Uma consulta com subconsultas agregadas independentes por owner_user_id evita multiplicação: count de edições e count/count distinct edition_id/count acquisition_price/sum acquisition_price de physical_copies. Índices existentes iniciados por dono atendem o acesso, sem migration. BigDecimal e long; soma nula permanece desconhecida quando há exemplares sem qualquer preço; vazio tem zero. Formatação NumberFormat pt-BR no DTO sem instância mutável compartilhada. Cartões semânticos na inicial, antes do destaque, com explicação de cobertura e links aos cadastros. Testes unitários e PostgreSQL/MockMvc verificam isolamento, atualização e limites; prévia responsiva com dados de teste. É subconjunto de T016, não encerra a tarefa ampla.

### T015 — checklist explícito (2026-10-09)

Módulo collection com entidade ChecklistItem, serviço e DTOs, repositório JPA para escrita e consultas JDBC paginadas para leitura. V12 cria series_checklist_items e FK composta série/proprietário, número editorial/normalizado (40), rótulo (180), owned boolean, notas (2000), auditoria; unicidade dono/série/número normalizado e índices dono/série/data/id e dono/série/owned/data/id. Normalização Locale.ROOT com trim, sem conversão numérica. Mutações e remoção de série usam lock do dono; série referenciada bloqueada com mensagem. GET/POST checklist cria um item por vez; GET/POST /checklist/itens/{id}/editar e /excluir; GET faltantes filtra owned=false, sem consultas a exemplares nem sincronização automática. Form DTO/Bean Validation/PRG/CSRF e confirmação. Views recebem somente DTOs; série autorizada expõe id/nome. Testes JUnit/Mockito e MockMvc/PostgreSQL cobrem AC-006 e integridade.

### T014 — exemplares físicos (2026-10-09)

V11 cria physical_copies: UUID/dono/edição, localização opcional, conservação e observações, origem/data/preço opcionais, timestamps UTC; numeric(12,2) BRL e FKs compostas por proprietário. Índices dono/edição/data/id e dono/localização. Módulo inventory retorna DTOs via consulta paginada com joins do caminho de localização e consulta separada da edição autorizada. Mutações usam o mesmo lock do proprietário de T013, incluindo mudança de suporte/exclusão de edição para não disputar referências. Detalhe bibliográfico integra contagem/avisos e paginação de exemplares. Formulários usam decimal pt-BR e confirmação server-side. Edições digitais rejeitadas para exemplares físicos; preços de referência não migram/inferem aquisições. Testar regras, proprietário, valores distintos, bloqueios de referência, CSRF e renderização.

### T013 — localizações (2026-10-09)

Módulo inventory com domain/application/infrastructure/presentation; nenhum objeto JPA nas telas. V10 cria storage_locations, FK composta de pai/proprietário, check de tipo e auto-referência, nomes únicos entre irmãos com NULLS NOT DISTINCT (PostgreSQL 16). Enum estabelece quatro posições; cada ligação deve aumentar a posição, permitindo omitir níveis. Serviço verifica pai/ancestrais e filhos antes de editar, usa lock transacional da linha do proprietário para serializar alterações da árvore e evita ciclos concorrentes. Listagem e seleção de pai usam consulta com três joins ancestrais e DTO de caminho, sem consultas por nó. Seleção de pai apresenta caminhos e tipos do proprietário e exclui o próprio registro; servidor valida compatibilidade e ciclos ao salvar, sem JavaScript obrigatório. Exclusão usa tela de confirmação e POST/CSRF; bloqueio por filhos no serviço e FK. T014 acrescentará bloqueio por exemplares. Testes de domínio/serviço e MockMvc/PostgreSQL cobrem regras e isolamento.

### Extensão solicitada em 2026-10-09 — valores e suporte (FR-026)

Migration V8 adiciona `media_type` opcional (PHYSICAL/DIGITAL), `cover_price` e `reference_paid_price` como `numeric(12,2)` com constraints não negativas. Valores monetários em Java usam BigDecimal, validados antes da persistência. Formulário aceita vírgula ou ponto decimal e apresenta erros em pt-BR. DTO de leitura de edição impede exposição de entidades JPA pelo controller alterado. Sem backfill de preços ou suporte; novo formulário oferece físico como padrão. O valor pago é uma referência privada da edição até a implementação de aquisições por exemplar; não agrega nem substitui esse modelo. Verificar migration limpa e compatibilidade de dados anteriores, cadastro, edição, validação, formatação e isolamento com MockMvc/PostgreSQL.

O modelo lógico está em [ER](../../docs/er-model.md) e o detalhamento físico em [dicionário](../../docs/data-dictionary.md). Todas as tabelas proprietárias carregam `owner_user_id`; relações entre dados do usuário exigem o mesmo proprietário no serviço e, onde possível, por constraints. UUIDs são preferidos para IDs externos/URLs. Flyway cria schema e índices em migrations imutáveis.

Consultas de catálogo usam `Pageable`, projeções/fetch planejado e especificações JPA. Índices essenciais incluem proprietário + título/ordenação, proprietário + FK de série/editora e proprietário + edição em exemplares. Antes da Fase 5, aferir plano de consulta em uma massa representativa.

## Rotas MVC propostas

T012 (2026-10-09): GET `/edicoes` aceita `q`, `publisherId`, `seriesId`, `format`, `mediaType`, `order`, `page`. Specification JPA compõe predicados com proprietário obrigatório; paginação e ordenação executam no PostgreSQL. Busca escapa caracteres LIKE; ordenação vem de enum allowlist e termina com id. Opções de formato usam DISTINCT por proprietário; detalhe usa joins/projeções DTO para obras/criadores, evitando consultas por associação. V9 adiciona índices dono/formato, dono/suporte/título/id e dono/data/id para os novos caminhos. Parâmetros malformados renderizam erro 400 pt-BR. Cobrir MockMvc, PostgreSQL/Testcontainers e regras de consulta; inventário/dados de exemplares continuam em T014.

| Área | Rotas | Observações |
| --- | --- | --- |
| Autenticação | `GET/POST /login`, `POST /logout` | POST é conduzido por Spring Security; login customizado Thymeleaf. |
| Dashboard | `GET /dashboard` | Página inicial autenticada. |
| Edições | `GET /edicoes`, `GET /edicoes/{id}`, `GET /edicoes/nova`, `POST /edicoes`, `GET/POST /edicoes/{id}/editar`, `POST /edicoes/{id}/excluir`, `POST /edicoes/{id}/capa` | Filtros em query string; exclusão com confirmação e CSRF. |
| Exemplares | `GET /edicoes/{editionId}/exemplares/novo`, `POST /edicoes/{editionId}/exemplares`, `GET/POST /exemplares/{id}/editar`, `GET/POST /exemplares/{id}/excluir` | Sempre valida dono da edição/exemplar; GET confirma exclusão, POST exige confirmação/CSRF. |
| Editoras | `GET /editoras`, `GET/POST /editoras/nova`, `GET/POST /editoras/{id}/editar`, `POST /editoras/{id}/excluir` | Exclusão segura. |
| Séries/checklist | `GET /series`, `GET/POST /series/nova`, `GET/POST /series/{id}/editar`, `POST /series/{id}/excluir`, `GET/POST /series/{id}/checklist`, `GET /series/{id}/faltantes` | Lacunas explícitas. |
| Itens do checklist | `GET/POST /checklist/itens/{id}/editar`, `GET/POST /checklist/itens/{id}/excluir` | Série não é trocada; GET confirma remoção, POST exige confirmação e CSRF. |
| Localizações | `GET /localizacoes`, `GET/POST /localizacoes/nova`, `GET/POST /localizacoes/{id}/editar`, `POST /localizacoes/{id}/excluir` | Árvore limitada a quatro níveis. |
| Exportação | `GET /exportacao/csv` | `text/csv; charset=UTF-8`, autenticada. |

Não há API REST de negócio no MVP. O contrato OpenAPI permanece explícito e vazio até uma integração justificada; Actuator é interno/protegido conforme perfil.

## Interface

T023 — navegação consistente (2026-10-09): extrair sidebar para fragment reutilizável com seção ativa e logout; aplicar shell responsivo e breadcrumbs às telas privadas. NavigationContext DTO/advice deriva módulo de rotas conhecidas; allowlist de destinos e parâmetros normaliza returnTo, com tamanho limitado, sem Referer. Links de registros/formulários transportam contexto e forms POST carregam campo oculto. Interceptor de apresentação preserva retornos PRG a listas e propaga contexto a detalhes/confirmações, sem substituir autorização dos serviços nem afetar logout/login/download. Origem mais próxima de checklist/faltantes tem prioridade; não reproduzir histórico arbitrário de navegação. Cancelar edição existente retorna ao detalhe; entidades filhas aos respectivos pais. CSS compartilhado mantém tema da inicial, largura de formulários, menu móvel e foco. Testes unitários do contexto/redirect, MockMvc para todas as telas/fluxos/validação/ownership e prévia visual desktop/celular.

T022 — melhoria da página inicial (2026-10-09): template dashboard/index com menu lateral em desktop e compacto em celular, cabeçalho com pesquisa do catálogo e logout, destaque editorial com chamada para cadastrar edição e ilustração decorativa em HTML/CSS, cartões de acesso aos módulos e orientação breve para organizar coleção. CSS isolado em dashboard.css, sem bibliotecas/fontes externas/JavaScript obrigatório, sem tocar estilos das demais telas. Página continua sem consultas adicionais nem métricas fictícias; T016 entrega agregações reais posteriormente. Teste MockMvc de rotas/HTML/CSRF, build completo e inspeção visual do HTML renderizado em prévia local, documentando limites de acessibilidade.

Templates em `src/main/resources/templates`, com fragments de layout, cabeçalho, navegação, feedback e formulários. CSS autoral em `static/css`; JavaScript vanilla só para pré-visualização de capa/confirmação progressiva, com comportamento básico disponível sem JavaScript. Tema escuro, biblioteca digital, cards de capas, menu lateral em desktop e navegação compacta em celular.

## Segurança e operações

- Spring Security: senha com bcrypt/argon2 suportado, sessão, CSRF, fixation protection, autenticação obrigatória e 404 para recurso alheio.
- Upload: allowlist MIME e assinatura de conteúdo, limite configurável, nomes gerados, caminho não controlado pelo usuário, arquivo servido apenas após autorização (ou URL assinada em futura storage), sem SVG no MVP.
- Configuração: `application.yml` sem segredo; perfis local/test/prod e variáveis de ambiente. Produção exige HTTPS no proxy e cookies `Secure`.
- Saúde: Spring Boot Actuator com health liveness/readiness; Docker Compose com health checks de aplicação e PostgreSQL.
- Backup: `pg_dump` + cópia consistente do volume de capas; restauração em ambiente limpo documentada e exercitada na Fase 6.

## Estratégia de teste

Reforço T024 (2026-10-09): teste PostgreSQL/MockMvc exercita POST real com CSRF para cadastro e edição, seguido por GET da inicial; cobre preço alterado, preço removido e tentativa inválida. Agregação já é por leitura, sem cache/contador persistido para invalidar; não alterar PRG de retorno à edição nem introduzir polling.

| Camada | Evidência |
| --- | --- |
| Domínio/aplicação | JUnit 5 e Mockito: proprietário, validações, duplicados, checklist e CSV. |
| Persistência | `@DataJpaTest`/integração com Testcontainers PostgreSQL e Flyway em banco vazio. |
| MVC | MockMvc: login, CSRF, validação, redirects, 404 entre usuários, views Thymeleaf e download CSV. |
| Arquivos | testes temporários para tipo/tamanho/nomes e autorização de capa. |
| E2E | jornada login → edição → exemplar → pesquisa, com ferramenta compatível disponível; registrar impedimento se não instalada. |
| Operação | `./mvnw clean verify`, `docker compose up`, health endpoint, backup/restauração em banco de teste. |

## Marcos de entrega

1. Fundação: wrapper Maven, Boot MVC/Thymeleaf, config, Flyway inicial, Compose/health, esqueleto visual e testes de fumaça.
2. Acesso: usuário/bootstrap, login/logout, isolamento proprietário testado.
3. Catálogo: editoras/séries/edições, capa e listagem pesquisável.
4. Inventário: exemplares/localizações e duplicados.
5. Consulta: checklist e dashboard real.
6. Entrega: CSV, endurecimento, testes integrados, backup/restauração, documentação e deploy guide.
