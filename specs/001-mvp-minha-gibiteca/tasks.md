# Tarefas ordenadas — MVP 1.0

Cada tarefa só começa após suas dependências e os requisitos associados estarem estáveis. `AC` referencia a especificação.

## Fase 1 — Fundação

- [x] T001 Criar `pom.xml`, Maven Wrapper e estrutura modular mínima; configurar Java 21 e dependências MVC/Thymeleaf/JPA/Validation/Security/Flyway/Actuator/testes. **Dep.:** planejamento. **Aceite:** `./mvnw clean verify` gera JAR. (NFR-006)
- [x] T002 Criar perfis de configuração e `docker-compose.yml` com PostgreSQL, volume e health checks. **Dep.:** T001. **Aceite:** serviços iniciam e health é verificável. (NFR-005)
- [x] T003 Criar migration Flyway inicial, página de login/base e fragments/CSS acessíveis. **Dep.:** T001. **Aceite:** migration limpa e MockMvc renderiza página. (NFR-001)
- [x] T004 Registrar versão do framework e decisões de infraestrutura. **Dep.:** T001–T003. **Aceite:** ADR atualizado, sem segredos versionados.

## Fase 2 — Autenticação

- [ ] T005 Modelar usuário e bootstrap documentado; criar migration e repositório. **Dep.:** T003. **Aceite:** usuário inicial criado sem segredo no Git. (FR-004)
- [ ] T006 Configurar login/logout, sessão, CSRF e política de acesso. **Dep.:** T005. **Aceite:** testes de acesso anônimo, login e logout. (FR-001/002)
- [ ] T007 Criar mecanismo reutilizável de resolução/autorização por dono. **Dep.:** T006. **Aceite:** teste de recurso alheio retorna 404. (FR-003, AC-001)

## Fase 3 — Catálogo

- [ ] T008 Implementar migrations/modelo e CRUD de editoras. **Dep.:** T007. **Aceite:** validação, PRG, busca e exclusão segura testadas. (FR-010/012)
- [ ] T009 Implementar migrations/modelo e CRUD de séries. **Dep.:** T008. **Aceite:** relação opcional com editora e dados alheios isolados. (FR-011/012)
- [ ] T010 Implementar modelo de edição, obra/criador e formulário DTO. **Dep.:** T008–T009. **Aceite:** ISBN vazio e numeração textual persistem. (FR-020–022, AC-002)
- [ ] T011 Implementar upload/serviço autorizado de capa. **Dep.:** T010. **Aceite:** imagem válida renderiza; arquivo inválido é rejeitado. (FR-023, AC-004, NFR-004)
- [ ] T012 Implementar catálogo paginado, busca, filtro, ordenação e detalhe. **Dep.:** T010. **Aceite:** MockMvc/integrado cobre parâmetros e volume. (FR-024/025, AC-005)

## Fase 4 — Inventário

- [ ] T013 Implementar localizações hierárquicas e regras de exclusão. **Dep.:** T007. **Aceite:** níveis válidos e bloqueio de localização referenciada. (FR-032/033)
- [ ] T014 Implementar exemplares, conservação/aquisição simplificada e alerta de duplicado. **Dep.:** T010/T013. **Aceite:** duas cópias persistem e alerta não bloqueia. (FR-030/031, AC-003)

## Fase 5 — Consulta

- [ ] T015 Implementar checklist explícito e faltantes. **Dep.:** T009/T014. **Aceite:** nenhuma lacuna é inferida. (FR-040/041, AC-006)
- [ ] T016 Implementar consultas e tela de dashboard. **Dep.:** T012/T014. **Aceite:** totais e agregações conferem com banco. (FR-050, AC-007)

## Fase 6 — Entrega

- [ ] T017 Implementar exportação CSV segura e testes de conteúdo/proprietário. **Dep.:** T012/T014. **Aceite:** UTF-8, cabeçalho e isolamento. (FR-060, AC-008)
- [ ] T018 Executar Testcontainers, E2E disponível e análise de N+1/índices em massa. **Dep.:** T001–T017. **Aceite:** evidências reais registradas; falhas viram pendências. (NFR-002/003)
- [ ] T019 Documentar instalação, produção, backup/restauração e roadmap; testar restauração. **Dep.:** T002/T011/T017. **Aceite:** procedimento reproduzível em ambiente limpo. (NFR-005)
- [ ] T020 Verificação final de especificação, acessibilidade, segurança e critérios. **Dep.:** T018/T019. **Aceite:** matriz de rastreabilidade com evidências e pendências.
