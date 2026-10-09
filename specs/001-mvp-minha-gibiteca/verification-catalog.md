# Verificação — catálogo e consulta

Data: 2026-10-09. Rastreio: T012, FR-024/025, AC-005, NFR-001/002/004.

## Especificação e análise

Especificação e plano detalhados antes do código. O filtro de tipo desta fase corresponde ao suporte físico/digital já entregue em FR-026. T012 cobre detalhe bibliográfico; exemplares no detalhe permanecem em T014. Todas as consultas filtram o proprietário no servidor. Ordenação é uma lista permitida, nunca um nome de propriedade fornecido pelo usuário.

## Arquivos e alterações

- CatalogFilter/CatalogOrder, EditionSpecifications, EditionRepository/EditionService: filtros combináveis, busca literal em título/ISBN/numeração, paginação de vinte registros e desempate por UUID.
- EditionController e EditionCatalogViewService: DTOs, opções próprias, nomes de editora/série e consultas com joins para obras/criadores em quantidade fixa, sem consulta por associação.
- V9 e dicionário: índices para formato, suporte e data de cadastro.
- Template de lista/CSS: controles pt-BR responsivos, limpar filtros, contagem, capas com carregamento adiado e navegação que mantém parâmetros.
- Detalhe e errors/400/MvcExceptionHandler: nomes bibliográficos e mensagem pt-BR para parâmetros malformados.

## Evidências executadas

- `./mvnw clean verify -q`: código 0 com 15 testes antes do cenário adicional de desempate.
- `./mvnw verify -q`: código 0 com 16 testes, zero falhas/erros; JAR empacotado.
- PostgreSQL real em Testcontainers: nove migrations aplicadas em schema limpo; também executada atualização de schema V7 com dados até V9.
- MockMvc/serviço: 25 edições correspondentes divididas em 20 + 5, combinação de todos os filtros, ordenação A–Z/Z–A, recentes/antigos, UUID para títulos iguais, query literal com `%`/`_`, ISBN/numeração, parâmetros de paginação, opções alheias ocultas, filtros alheios 404, entradas malformadas 400 e detalhe com associações.
- `git diff --check`: sem erros.

## Pendências

Não foi realizada aferição de p95, plano EXPLAIN com massa de 10.000 exemplares, contagem instrumentada de consultas nem auditoria visual/WCAG completa; continuam em T018/T020. A remoção de consultas por associação foi verificada pelo código de joins e renderização integrada. Aplicação local não reiniciada e banco da coleção não alterado manualmente; Flyway aplica V9 na próxima inicialização. Diretório data preexistente preservado. Próxima fase: localizações (T013) e exemplares (T014).
