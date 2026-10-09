# Verificação — valores e suporte

Data: 2026-10-09. Rastreio: T021, FR-026, AC-009, NFR-001/004.

## Decisão e análise

A solicitação amplia o escopo original de preço manual e suporte digital. Adotados dois valores opcionais em BRL: preço de capa e valor pago de referência da edição. A pergunta sobre quais valores foi apresentada; sem resposta durante esta implementação, a premissa de ambos foi comunicada. Não há agregação de preços de cópias. Aquisições por exemplar permanecem previstas em T014. Cadastros antigos não têm suporte presumido.

## Arquivos e comportamento

- Spec, plano, tarefas e dicionário atualizados antes de implementar FR-026.
- V8: suporte opcional e preços numeric(12,2), constraints de suporte e valores não negativos.
- Edition, EditionService e EditionForm: BigDecimal, validações e persistência em cadastro/alteração; casas excedentes são rejeitadas, sem arredondamento silencioso.
- EditionView/EditionCatalogViewService e controller: DTOs nas telas de edições; entrada decimal com vírgula ou ponto; erros traduzidos em messages.properties.
- Templates de cadastro/edição, lista e detalhe: físico/digital, campos opcionais, formatação em reais; zero permanece diferente de valor desconhecido.
- Testes de capas direcionados a target/test-covers para não usar arquivos da coleção.

## Evidências executadas

`./mvnw clean verify -q` terminou com código 0; 13 testes integrados, zero falhas/erros, PostgreSQL em Testcontainers. Cobertura adicionada: cadastro digital, valores exatos, detalhe/lista e formulário, edição físico/zero/nulo, negativos, precisão excedente, valor fora do limite, texto inválido, isolamento e migração real de V7 para V8 com registro anterior preservado. JAR gerado em target.

`git diff --check` terminou sem erros.

## Pendências e limites

Aplicação local não reiniciada nem banco da coleção migrado nesta verificação; a migration será aplicada pelo Flyway na próxima inicialização. Verificação visual manual em navegador e auditoria completa WCAG não foram realizadas. Valores por exemplar aguardam T014. Demais tarefas de catálogo/inventário seguem com seu estado anterior. O diretório data preexistente foi preservado.
