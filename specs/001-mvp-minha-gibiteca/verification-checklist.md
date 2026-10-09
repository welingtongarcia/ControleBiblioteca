# Verificação — checklist e quadrinhos faltantes

Data: 2026-10-09. T015, FR-040/041/033, AC-006, NFR-001/002/004.

## Escopo e arquivos

Especificação, plano, tarefas e análise atualizados antes da implementação. Checklist manual por série, sem números inferidos e sem sincronização com exemplares. Número editorial obrigatório até 40 caracteres; descrição opcional até 180; observações até 2000. Estado inicial Não possuo, editável. Números são únicos por proprietário/série após trim e Locale.ROOT; 0 e 00 continuam distintos. Ordem de cadastro com UUID de desempate; vinte itens por página, faltantes filtrados no banco.

- Módulo collection: domain/ChecklistItem; application/ChecklistService e ChecklistView; infrastructure/ChecklistRepository e ChecklistQueries; presentation/ChecklistForm e ChecklistController. Views somente DTOs; binding limitado aos campos editáveis.
- V12 cria series_checklist_items, FK composta de série/proprietário, unicidade, checks, auditoria e índices de lista/faltantes, sem cascata.
- ComicSeriesService expõe DTO id/nome e bloqueia exclusão com checklist ou edições; mutações do checklist e remoção da série compartilham lock do proprietário. Controller apresenta mensagem de referência.
- Templates checklist/list, form e delete: cadastro, edição, confirmação, estado manual, faltantes e paginação pt-BR; links em series/list. CSS protege campos e textos longos; mensagem de conversão de estado localizada.
- Dicionário, ER e rastreabilidade atualizados; ChecklistServiceTest e MinhaGibitecaApplicationTest ampliados.

## Evidências executadas

`./mvnw clean verify -q`: código 0, JAR gerado; 45 testes, zero falhas/erros/ignorados. São 10 JUnit/Mockito de checklist, 8 de exemplares e 27 integrados MockMvc/PostgreSQL. Testcontainers/PostgreSQL 16 aplicou V1–V12 em banco vazio; atualização de schema V7 até V12 também passou.

AC-006: entradas 0, 1A e Especial; somente 1A aparece na consulta de faltantes. Checklist vazio não cria itens. Cadastro/remoção de exemplar 1A não modifica o estado manual; marcar Possuo retira dos faltantes e desmarcar o devolve. Editar não troca a série, mesmo com parâmetro extra. Números irregulares preservados pelo domínio; duplicado com espaços/caixa diferente rejeitado na mesma série, aceito em outra. Campos obrigatórios/limites/estado inválido geram erros renderizados. Texto HTML é escapado. Rotas e mutações alheias retornam 404; POST de criação/edição/exclusão sem CSRF é rejeitado. Exclusão exige confirmação, preserva série e catálogo. Série com checklist/edições não é removida; liberada após remoção explícita. Paginação de faltantes 20 + 5, lista completa 20 + 6, links preservam o modo; página inválida retorna 400. SQL direto rejeita série alheia, número duplicado e exclusão de série referenciada.

`git diff --check`: sem erros.

## Pendências e riscos

Próxima tarefa: T016, painel com totais e agregações reais. Evidência visual de exemplares no checklist não incluída (opcional em FR-041). Teste de concorrência dos locks, aferição de desempenho em massa, E2E em navegador e auditoria visual/WCAG completa continuam em T018/T020. Mantém-se o aviso de autocarregamento do agente Mockito no Java 21, sem falha; revisar antes de atualização de JDK.

Nesta etapa, a aplicação em uso não foi reiniciada e V12 não foi executada manualmente no banco da coleção. Flyway aplicará a migração na próxima inicialização. data/ preexistente preservado e não incluído no commit.
