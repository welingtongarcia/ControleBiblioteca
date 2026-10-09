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
