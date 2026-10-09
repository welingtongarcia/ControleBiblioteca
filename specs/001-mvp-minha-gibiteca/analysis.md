# Análise de consistência — pré-implementação

**Data:** 2026-10-08  
**Resultado:** apto a iniciar Fase 1, condicionado às premissas registradas.

| Verificação | Resultado | Tratamento |
| --- | --- | --- |
| Stack exige MVC/Thymeleaf e prompt proíbe SPA | Consistente | Plano não cria frontend separado nem API de negócio. |
| Catálogo futuro compartilhável x privacidade | Risco de modelagem | MVP usa ownership em todos os dados (CL-02); migração futura poderá extrair catálogo global. |
| ISBN opcional x unicidade | Consistente | Sem unique global de ISBN; alerta de duplicação no serviço. |
| Numeração irregular x checklist de faltantes | Consistente após CL-04 | Checklist explícito é fonte de lacunas; não há sequência inventada. |
| Upload de capa x controle de acesso | Cobertura necessária | T011 prevê armazenamento externo ao banco e serviço autorizado; URLs públicas diretas ficam vedadas. |
| CRUD x exclusão segura | Cobertura necessária | CL-05 e T008/T009/T013 preveem bloqueio por dependências; fluxos detalham mensagem. |
| Meta de 10 mil x listas | Cobertura planejada | Paginação, índices e aferição em T012/T018; meta ainda não medida. |
| Springdoc exigido x nenhuma API no MVP | Sem contradição | Contrato OpenAPI vazio explicita ausência; dependência/configuração entra na fundação para futuras integrações. |
| Recuperação de senha x e-mail ausente | Fora de escopo | Bootstrap documentado; não expor fluxo inoperante. |

## Cobertura de requisitos

Todos os FR possuem pelo menos uma tarefa: acesso T005–T007; editoras/séries T008–T009; catálogo T010–T012; inventário T013–T014; checklist/dashboard T015–T016; CSV T017. NFR são cobertos transversalmente por T001–T004, T011, T012, T018–T020.

## Riscos a controlar antes da fase correspondente

1. Escolher versão Spring Boot e Springdoc realmente compatíveis, por release notes, durante T001.
2. Definir limite de tamanho/formatos de capa por configuração antes de expor upload em T011.
3. Confirmar suporte de Testcontainers/Docker na máquina de CI antes de tornar T018 obrigatório em pipeline.
4. Validar a estratégia de backup do volume de capas junto do PostgreSQL em T019; `pg_dump` isolado não é backup completo.

Nenhuma contradição impede iniciar a fundação. Não há código funcional nesta entrega.

## Análise incremental — T014 (2026-10-09)

FR-030/031/033 e AC-003/011 detalhados na especificação e no plano. Exemplar continua distinto de edição: não duplica dados bibliográficos nem infere preço de aquisição. A possibilidade de suporte desconhecido preserva registros antigos, enquanto DIGITAL rejeita exemplar físico. FKs compostas protegem ownership e exclusões; o mesmo lock do proprietário serializa cadastro de cópia, edição de suporte, remoção de edição/localização e realocação. Consultas de detalhe/lista usam DTOs com joins e paginação, sem carregar entidades nas telas. Nenhum serviço injeta outro em ciclo (EditionService e LocationService consultam referências por JDBC).

Riscos restantes: opções de localização carregam todas as localizações próprias; aferição com volume real e concorrência fica em T018. Não se garante regra física/digital para SQL manual fora dos serviços; propriedade e referências são protegidas também pelo banco. Aquisição não modela empréstimos, vendas ou histórico financeiro nesta tarefa.

## Análise incremental — T015 (2026-10-09)

Checklist não é inventário: estado manual autoritativo, sem criar/atualizar itens por exemplares. Número editorial é texto (0, 1A, Especial), comparação normalizada sem ordenar numericamente. Ownership validado em serviço e FK composta; DTO de série evita novas entidades JPA na apresentação. Exclusão de série bloqueia checklist sem cascata. Lock comum serializa criação de item e exclusão de série; constraint única protege duplicados. Paginação e filtro SQL de faltantes evitam N+1. Evidência visual de exemplares é opcional em FR-041 e não será implementada nesta etapa. Não há bloqueio de planejamento.

## Análise incremental — página inicial T022 (2026-10-09)

Melhoria de interface não antecipa totais da T016 nem cria dados de demonstração. Pesquisa reutiliza o catálogo e atalhos apontam apenas para funcionalidades existentes. Logout mantém POST/CSRF. Template usa HTML semântico; CSS próprio separado evita regressões nas telas de cadastro. Ilustração de capas é decorativa, aria-hidden, sem imagens externas. Sem mudança de modelo, migration ou consulta privada nesta etapa.

## Análise incremental — navegação T023 (2026-10-09)

Marca volta sempre ao início; volta contextual é um controle distinto. Origem mais próxima de lista é transportada explicitamente, validada e limitada à mesma seção; não usar histórico do navegador nem confiar em Referer. Allowlist impede retorno externo e rotas de mutação. Autorização permanece nos controllers/serviços, e trilhas de entidades usam apenas DTOs já autorizados. Fragment evita menu divergente entre telas. Interceptor só altera redirects MVC de apresentação previstos, com contexto presente, preservando fluxos sem parâmetro. Não altera regras de domínio, esquema nem dependências. Histórico de múltiplas origens aninhadas fica fora desta fase.
# Análise T024 — resumo da coleção (2026-10-09)

Constituição preservada: DTO, proprietário da sessão, agregação no banco, sem entidades na apresentação nem schema novo. Clarificação adotada: valor é gasto registrado nos exemplares físicos, não avaliação de mercado; referências bibliográficas e digitais não são aquisições e não entram na soma. Ausência de preço fica explícita, zero permanece conhecido. Consulta sem joins evita duplicação e tem quantidade fixa; agregação BigDecimal suporta total acima do limite individual. Testar isolamento, vazio, desconhecidos, gratuitos e atualização. T016 permanece aberta para demais indicadores; performance em massa e auditoria WCAG completa seguem pendentes.
# Reforço T024 — atualização após formulários (2026-10-09)

Inspeção confirma DashboardService sem cache e consulta SQL a cada GET; CopyService grava/flush em transação concluída antes do redirect. Não é necessário armazenar/recalcular totais na escrita, o que criaria risco de divergência. Verificação adicional cobre os controllers, não só chamadas diretas ao serviço. Limite explícito: abas já abertas não recebem push; novo acesso/recarregamento mostra dados atuais. Sem mudança de comportamento, schema ou dados privados.
# Análise T025 — correção aprovada do resumo (2026-10-09)

Usuário confirmou somar Valor pago dos cadastros e mostrar físicos/digitais; diagnóstico mostrou indicador anterior restrito ao inventário. Constitução permanece: distinguir edição/exemplar, não criar cópias fictícias. Total principal é referência cadastrada, não gasto de múltiplas cópias nem avaliação. Duas fontes independentes e rótulos explícitos evitam dupla contagem. Ausências não viram zero em coleção não vazia sem valores. Sem schema novo nem dados reais alterados; testar sem exemplares e alterações de suporte/preço. Atualização permanece na próxima leitura, não push entre abas. T016 amplo segue pendente.
# Análise T026 — leitura (2026-10-09)

Constituição compatível: leitura privada por edição, sem entidades novas nos controllers; Flyway explícito. Clarificação adotada e comunicada: coleção específica é série, denominador somente edições cadastradas, não checklist nem exemplares. Inicial geral inclui edições sem série. Estado binário inicialmente Não li, reversível/idempotente e não inferido de posse. Lote de séries limitado à página evita N+1; consultas obrigatoriamente por dono. Riscos: percentual não cobre números ainda não cadastrados; explicado na interface. Testar upgrade sem apagar dados, CSRF/owner, arredondamento e alterações. Sem auditoria WCAG completa, performance em massa ou atualização push nesta etapa.
# Análise T027 — ISBN (2026-10-09)

Constituição mantida: assistência ao formulário, DTO, persistência só após Salvar, consultas públicas sem dados privados, ISBN manual opcional. Clarificação de segurança: não importar capa ou criar editora/criador automaticamente; sugerir/link de fonte e associar apenas registros próprios únicos. Número/série/suporte/valores/leitura não inferidos. Open Library Books API é documentada porém legado: cobertura incompleta e eventual retirada são riscos; fallback manual obrigatório. Timeout/JSON/cache/taxa limitados e URLs fixas evitam espera ilimitada, SSRF e rajadas. Sem migração/schema. Integração externa não será considerada validada apenas por mocks; smoke de ISBN público separado, sujeito à conectividade. Google Books/fallback múltiplo e importação de capa fora desta entrega.
