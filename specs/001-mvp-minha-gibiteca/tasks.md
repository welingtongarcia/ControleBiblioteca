# Tarefas ordenadas — MVP 1.0

Cada tarefa só começa após suas dependências e os requisitos associados estarem estáveis. `AC` referencia a especificação.

## Fase 1 — Fundação

- [x] T001 Criar `pom.xml`, Maven Wrapper e estrutura modular mínima; configurar Java 21 e dependências MVC/Thymeleaf/JPA/Validation/Security/Flyway/Actuator/testes. **Dep.:** planejamento. **Aceite:** `./mvnw clean verify` gera JAR. (NFR-006)
- [x] T002 Criar perfis de configuração e `docker-compose.yml` com PostgreSQL, volume e health checks. **Dep.:** T001. **Aceite:** serviços iniciam e health é verificável. (NFR-005)
- [x] T003 Criar migration Flyway inicial, página de login/base e fragments/CSS acessíveis. **Dep.:** T001. **Aceite:** migration limpa e MockMvc renderiza página. (NFR-001)
- [x] T004 Registrar versão do framework e decisões de infraestrutura. **Dep.:** T001–T003. **Aceite:** ADR atualizado, sem segredos versionados.

## Fase 2 — Autenticação

- [x] T005 Modelar usuário e bootstrap documentado; criar migration e repositório. **Dep.:** T003. **Aceite:** usuário inicial criado sem segredo no Git. (FR-004)
- [x] T006 Configurar login/logout, sessão, CSRF e política de acesso. **Dep.:** T005. **Aceite:** testes de acesso anônimo, login e logout. (FR-001/002)
- [x] T007 Criar mecanismo reutilizável de resolução/autorização por dono. **Dep.:** T006. **Aceite:** teste de recurso alheio retorna 404. (FR-003, AC-001)

## Fase 3 — Catálogo

- [x] T032 Trocar controle de leitura por interruptor acessível no catálogo/detalhe, preservando POST/CSRF/retorno/percentuais; testar estados e conferir desktop/móvel. **Dep.:** T026/T030. **Aceite:** FR-048, AC-020; verification-reading-switch.md.

- [x] T031 Reorganizar formulário de edição, grupos semânticos, erros navegáveis e associações por checkboxes; preservar ISBN/dados/CSRF/retorno e verificar desktop/móvel. **Dep.:** T010/T021/T023/T027. **Aceite:** FR-047, AC-019; verification-edition-form.md.

- [x] T030 Reorganizar catálogo com cartões, filtros expansíveis, valores/leitura, faixa de resultados e vazio contextual; preservar consultas/retornos/CSRF e conferir desktop/móvel. **Dep.:** T012/T021/T023/T026. **Aceite:** FR-046, AC-018; verification-catalog-layout.md.

- [x] T029 Reorganizar início com ações/visão geral/acervo/apoio, disclosure de metodologia e vazio contextual; manter cálculos/links/segurança e validar desktop/móvel. **Dep.:** T026. **Aceite:** FR-045, AC-017; evidências em verification-home-layout.md.

- [x] T027 Buscar metadados por ISBN no cadastro novo, preencher vazios/revisar sugestões, preservar rascunho e fallback manual; testar checksum/HTTP/CSRF/isolamento/PRG. **Dep.:** T026. **Aceite:** AC-015, FR-027/028; evidências e limites em verification-isbn.md; sem importação automática de capa.
- [x] T028 Google Books como segunda fonte após ausência na Open Library; chave servidor, ISBN equivalente, fonte segura, erros distintos e cache composto. **Dep.:** T027. **Aceite:** FR-029, AC-016; verification-google-isbn.md registra testes e configuração/cota pendente para validação externa real.
- [x] T030-M Busca assistida por título em fontes gratuitas, candidatos de edição, seleção restrita ao dono e preenchimento somente de vazios; testes e evidências em `free-metadata.md`. **Dep.:** T028. **Aceite:** FR-030-M / AC-017-M; 144 testes sem falhas; Google externo depende de chave/cota.

- [x] T021 Adicionar suporte físico/digital, preço de capa e valor pago de referência no cadastro, edição, lista e detalhe; migration preserva registros antigos; testes de decimal, validação e isolamento. **Dep.:** T010. (FR-026, AC-009, NFR-001/004)

- [x] T008 Implementar migrations/modelo e CRUD de editoras. **Dep.:** T007. **Aceite:** validação, PRG, busca e exclusão segura testadas. (FR-010/012)
- [x] T009 Implementar migrations/modelo e CRUD de séries. **Dep.:** T008. **Aceite:** relação opcional com editora e dados alheios isolados. (FR-011/012)
- [x] T010 Implementar modelo de edição, obra/criador e formulário DTO. **Dep.:** T008–T009. **Aceite:** ISBN vazio e numeração textual persistem. (FR-020–022, AC-002)
- [x] T011 Implementar upload/serviço autorizado de capa. **Dep.:** T010. **Aceite:** imagem válida renderiza; arquivo inválido é rejeitado. (FR-023, AC-004, NFR-004)
- [x] T012 Implementar catálogo paginado, busca, filtro, ordenação e detalhe bibliográfico (exemplares em T014). **Dep.:** T010. **Aceite:** MockMvc/integrado cobre filtros combinados, busca literal, ordenação, preservação de parâmetros e volume acima de uma página. (FR-024/025, AC-005)

## Fase 4 — Inventário

- [x] T013 Implementar localizações hierárquicas e regras de exclusão. **Dep.:** T007. **Aceite:** níveis/saltos válidos, ciclos e duplicados rejeitados, bloqueio por filhos e confirmação; bloqueio por exemplares integrado em T014. (FR-032/033, AC-010)
- [x] T014 Implementar exemplares, conservação/aquisição simplificada e alerta de duplicado, integrar proteção de referências de edição/localização. **Dep.:** T010/T013. **Aceite:** duas cópias persistem e alerta não bloqueia; aquisição decimal, autorização e bloqueios testados. (FR-030/031/033, AC-003/011; evidências em verification-copies.md)

## Fase 5 — Consulta

- [x] T026 Marcar/desmarcar edição lida, progresso por série e geral, migration conservadora e testes de percentuais/isolamento/PRG. **Dep.:** T025/T015. **Aceite:** AC-014, FR-042–044; 83 testes e QA visual em verification-reading.md.

- [x] T025 Corrigir total principal para valores pagos das edições, contar físicas/digitais e manter inventário/aquisições separados. **Dep.:** T024. **Aceite:** AC-007/T025; sem duplicação, isolamento e atualização verificados; 77 testes e prévia visual em verification-summary-correction.md. (FR-050)

- [x] T024 Resumo na inicial: exemplares físicos, edições cadastradas/com exemplares, total pago conhecido e cobertura de preços; decimal e isolamento testados. **Dep.:** T014/T022. **Aceite:** AC-007/T024; 75 testes e prévia desktop/celular em verification-summary.md. (FR-050; subconjunto de T016)
  - [x] Reforço T024: validar atualização dos totais após POST de cadastro/edição de exemplar, incluindo rejeição e retirada do preço, sem recálculo manual. Evidência: verification-summary.md; 76 testes passaram.

- [x] T023 Padronizar menu, trilhas, retornos e cancelamento nas telas privadas; preservar contexto permitido e impedir redirecionamento externo. **Dep.:** T022/módulos entregues. **Aceite:** AC-013, segurança e layout verificados; 71 testes e conferência visual registrados em verification-navigation.md. (FR-052, NFR-001/004)

- [x] T022 Melhorar visual/navegação da página inicial, com pesquisa, atalhos e logout seguro, sem métricas fictícias. **Dep.:** módulos já entregues. **Aceite:** HTML/rotas/CSRF e prévia visual verificados. (FR-051, AC-012, NFR-001/004; evidências em verification-home.md)

- [x] T015 Implementar CRUD de checklist explícito, estado manual e faltantes paginados; proteger referências de séries e isolamento por dono. **Dep.:** T009/T014. **Aceite:** nenhuma lacuna é inferida, AC-006/duplicados/limites/CSRF/paginação/referências validados. (FR-040/041/033, AC-006; evidências em verification-checklist.md)
- [ ] T016 Implementar consultas e tela de dashboard. **Dep.:** T012/T014. **Aceite:** totais e agregações conferem com banco. (FR-050, AC-007)

## Fase 6 — Entrega

- [ ] T017 Implementar exportação CSV segura e testes de conteúdo/proprietário. **Dep.:** T012/T014. **Aceite:** UTF-8, cabeçalho e isolamento. (FR-060, AC-008)
- [ ] T018 Executar Testcontainers, E2E disponível e análise de N+1/índices em massa. **Dep.:** T001–T017. **Aceite:** evidências reais registradas; falhas viram pendências. (NFR-002/003)
- [ ] T019 Documentar instalação, produção, backup/restauração e roadmap; testar restauração. **Dep.:** T002/T011/T017. **Aceite:** procedimento reproduzível em ambiente limpo. (NFR-005)
- [ ] T020 Verificação final de especificação, acessibilidade, segurança e critérios. **Dep.:** T018/T019. **Aceite:** matriz de rastreabilidade com evidências e pendências.
