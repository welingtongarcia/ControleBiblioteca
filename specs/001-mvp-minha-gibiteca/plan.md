# Plano técnico — MVP 1.0

## Arquitetura

Monólito modular Spring Boot 3.x compatível com Java 21, Maven e PostgreSQL. A versão exata do Spring Boot será selecionada na Fase 1 dentre versões estáveis disponíveis e registrada no ADR correspondente. A aplicação terá módulos `auth`, `catalog`, `collection`, `inventory`, `dashboard` e `shared`; cada módulo separa `presentation` (MVC/form/view model), `application` (casos de uso), `domain` (regras) e `infrastructure` (JPA/arquivo/configuração).

Fluxo padrão: controller MVC → form DTO validado → serviço de aplicação → repositório/domain → DTO de view → template Thymeleaf. Controllers não recebem entidades JPA nem retornam entidades a telas. A aplicação usa sessão Spring Security, POST/Redirect/GET, flash messages e CSRF para todas as mutações.

## Modelo e persistência

### Extensão solicitada em 2026-10-09 — valores e suporte (FR-026)

Migration V8 adiciona `media_type` opcional (PHYSICAL/DIGITAL), `cover_price` e `reference_paid_price` como `numeric(12,2)` com constraints não negativas. Valores monetários em Java usam BigDecimal, validados antes da persistência. Formulário aceita vírgula ou ponto decimal e apresenta erros em pt-BR. DTO de leitura de edição impede exposição de entidades JPA pelo controller alterado. Sem backfill de preços ou suporte; novo formulário oferece físico como padrão. O valor pago é uma referência privada da edição até a implementação de aquisições por exemplar; não agrega nem substitui esse modelo. Verificar migration limpa e compatibilidade de dados anteriores, cadastro, edição, validação, formatação e isolamento com MockMvc/PostgreSQL.

O modelo lógico está em [ER](../../docs/er-model.md) e o detalhamento físico em [dicionário](../../docs/data-dictionary.md). Todas as tabelas proprietárias carregam `owner_user_id`; relações entre dados do usuário exigem o mesmo proprietário no serviço e, onde possível, por constraints. UUIDs são preferidos para IDs externos/URLs. Flyway cria schema e índices em migrations imutáveis.

Consultas de catálogo usam `Pageable`, projeções/fetch planejado e especificações JPA. Índices essenciais incluem proprietário + título/ordenação, proprietário + FK de série/editora e proprietário + edição em exemplares. Antes da Fase 5, aferir plano de consulta em uma massa representativa.

## Rotas MVC propostas

| Área | Rotas | Observações |
| --- | --- | --- |
| Autenticação | `GET/POST /login`, `POST /logout` | POST é conduzido por Spring Security; login customizado Thymeleaf. |
| Dashboard | `GET /dashboard` | Página inicial autenticada. |
| Edições | `GET /edicoes`, `GET /edicoes/{id}`, `GET /edicoes/nova`, `POST /edicoes`, `GET/POST /edicoes/{id}/editar`, `POST /edicoes/{id}/excluir`, `POST /edicoes/{id}/capa` | Filtros em query string; exclusão com confirmação e CSRF. |
| Exemplares | `GET /edicoes/{edicaoId}/exemplares/novo`, `POST /edicoes/{edicaoId}/exemplares`, `GET/POST /exemplares/{id}/editar`, `POST /exemplares/{id}/excluir` | Sempre valida dono da edição/exemplar. |
| Editoras | `GET /editoras`, `GET/POST /editoras/nova`, `GET/POST /editoras/{id}/editar`, `POST /editoras/{id}/excluir` | Exclusão segura. |
| Séries/checklist | `GET /series`, `GET/POST /series/nova`, `GET/POST /series/{id}/editar`, `POST /series/{id}/excluir`, `GET/POST /series/{id}/checklist`, `GET /series/{id}/faltantes` | Lacunas explícitas. |
| Localizações | `GET /localizacoes`, `GET/POST /localizacoes/nova`, `GET/POST /localizacoes/{id}/editar`, `POST /localizacoes/{id}/excluir` | Árvore limitada a quatro níveis. |
| Exportação | `GET /exportacao/csv` | `text/csv; charset=UTF-8`, autenticada. |

Não há API REST de negócio no MVP. O contrato OpenAPI permanece explícito e vazio até uma integração justificada; Actuator é interno/protegido conforme perfil.

## Interface

Templates em `src/main/resources/templates`, com fragments de layout, cabeçalho, navegação, feedback e formulários. CSS autoral em `static/css`; JavaScript vanilla só para pré-visualização de capa/confirmação progressiva, com comportamento básico disponível sem JavaScript. Tema escuro, biblioteca digital, cards de capas, menu lateral em desktop e navegação compacta em celular.

## Segurança e operações

- Spring Security: senha com bcrypt/argon2 suportado, sessão, CSRF, fixation protection, autenticação obrigatória e 404 para recurso alheio.
- Upload: allowlist MIME e assinatura de conteúdo, limite configurável, nomes gerados, caminho não controlado pelo usuário, arquivo servido apenas após autorização (ou URL assinada em futura storage), sem SVG no MVP.
- Configuração: `application.yml` sem segredo; perfis local/test/prod e variáveis de ambiente. Produção exige HTTPS no proxy e cookies `Secure`.
- Saúde: Spring Boot Actuator com health liveness/readiness; Docker Compose com health checks de aplicação e PostgreSQL.
- Backup: `pg_dump` + cópia consistente do volume de capas; restauração em ambiente limpo documentada e exercitada na Fase 6.

## Estratégia de teste

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
