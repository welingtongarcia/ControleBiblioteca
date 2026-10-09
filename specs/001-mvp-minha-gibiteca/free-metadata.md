# Metadados gratuitos — busca assistida no cadastro

Data: 2026-10-09. Constituição 1.0.0 mantida; monólito MVC, DTOs, pt-BR, CSRF e PRG.

## Especificação e clarificação — FR-030-M / AC-017-M

- Ampliar o cadastro de nova edição com busca por título nas APIs gratuitas Open Library e Google Books. ISBN existente continua disponível e não é obrigatório.
- Exibir até cinco candidatos por fonte com título, ISBN, ano, editora, autores e link de atribuição; usuário escolhe explicitamente. Nunca gravar edição durante busca/aplicação.
- Aplicar somente título, ISBN, ano e associações auxiliares vazias com correspondência única nos cadastros do proprietário. Não alterar valores, compra, suporte, leitura, observações ou número editorial. Não criar auxiliares ou baixar capas.
- Preservar todo rascunho e erros de conversão nos POSTs. Semântica de obra/edição: Open Library fornece candidatos de edição, nunca usar ano original da obra como ano da edição brasileira.
- Somente consulta humana pontual, sem scraping, cobrança, assinatura ou importação em massa. Google exige chave externa e cota; chave ausente é aviso, não ausência de resultados.
- Metabooks/GCD excluídos por gratuidade/condições não confirmadas. Comic Vine excluído desta entrega: gratuidade não resolve restrições de reprodução/manipulação para importação; requer esclarecimento dos termos, não contornar limitações.

## Plano

DTO de candidato e serviço de pesquisa com fontes fixas. Adaptadores HTTP JSON por fonte reutilizam clientes HTTPS com redirects desabilitados, timeouts 3s/5s e limite 256 KiB. Resultados em cache por consulta/fonte (10 min, 200 entradas); uma nova consulta por segundo/processo. Aplicação usa resultado no cache e índice validado, sem confiar em campos bibliográficos enviados pelo navegador, sem nova chamada externa ou alteração de banco. Cache expirado exige buscar novamente.

POST /edicoes/metadados/buscar e /edicoes/metadados/aplicar → GET /edicoes/nova. Seleção de fonte explícita evita chamadas desnecessárias; a opção Google é identificada como dependente de configuração. Links construídos com IDs validados, nunca URLs arbitrárias da resposta. Dados particulares não são enviados; somente termo digitado para busca.

## Tarefas — T030-M

- [x] M1 DTO, serviço com cache/limites, validação e seleção segura de candidato.
- [x] M2 Adaptadores Open Library/Google Books com opcionais seguros e erros sem segredos.
- [x] M3 Integração MVC/Thymeleaf, preservação e seleção explícita com fonte/atribuição.
- [x] M4 Testes de serviços/adaptadores/MVC, build completo e documentação de execução.

## Análise pré-implementação

Sem mudança de esquema ou migração; dados bibliográficos passam por DTO e campos existentes. Sem novos contratos pagos. Fontes e índices restritos no servidor, CSRF e autenticação herdados; nenhum valor ou data financeira inferido. Critérios cobertos por M1–M4. APIs não garantem cobertura brasileira: revisão manual obrigatória e erros externos não bloqueiam cadastro. Spec Kit não executado nesta revisão; artefatos SDD equivalentes.

Fontes primárias: https://openlibrary.org/dev/docs/api/search ; https://openlibrary.org/developers/api ; https://developers.google.com/books/docs/v1/using ; https://comicvine.gamespot.com/api/ .

## Validação do planejamento

AGENTS.md, prompt mestre, constituição e integração ISBN lidos; Java 21.0.12.1 e Docker 29.9.0 disponíveis. Git: somente data/ não rastreado preexistente, preservado. Implementação autorizada somente após esta análise.

## Verificação — 2026-10-09

- Planejamento anterior ao código: commit 342922b. Arquivos: DTO MetadataCandidate, contrato MetadataSearchProvider, serviço MetadataSearchService, adaptador FreeMetadataSearchProvider, EditionController, template edicoes/form.html, testes MetadataSearchTest/FreeMetadataSearchProviderTest/MinhaGibitecaApplicationTest, docs/installation.md e rastreabilidade spec/tasks.
- Serviço: token aleatório de seleção restrito ao dono, cache limitado/temporário, validação de fonte/termo/índice, falhas não cacheadas. Nenhuma requisição externa na aplicação do candidato. A limitação de uma nova pesquisa por segundo aplica-se à pesquisa por título; ISBN mantém seu limitador próprio. Não é rate limiting distribuído/agregado entre fluxos.
- Adaptador: consultas pontuais em hosts fixos; busca Open Library com q e dados da edição em editions.docs, nunca first_publish_year da obra; Google com intitle e langRestrict=pt. Sem redirects, URLs remotas ou imagens consumidas. IDs validados geram links de atribuição; metadados externos escapados no HTML.
- Primeira rodada dos novos testes não compilou por ambiguidade do overload assertThat(FlashMap). Corrigida asserção para containsKey booleano. Rodada seguinte `./mvnw test -q -Dtest=MetadataSearchTest,FreeMetadataSearchProviderTest,MinhaGibitecaApplicationTest`: exit 0, 56 testes sem falhas.
- Smoke externo Open Library, via GET curl com timeout 15s: uso inicial de title sem chave/title da obra retornava editions.docs vazio. Corrigido para q + key,title,author_name,editions e campos da edição; GET real retornou Batman - Ano Um - Volume 1, Panini Books, OL45096364M, ISBN 9788573517941/8573517948. Ano e autores ausentes não são inventados. Esta evidência é do endpoint público, não de E2E no banco do usuário. Google testado deterministicamente, sem chave real/cota externa validada.
- Rodada final `./mvnw clean verify -q`: exit 0, 144 testes, 0 falhas, 0 erros, 0 ignorados; PostgreSQL 16/Testcontainers, migrações/upgrade anteriores preservados e JAR gerado. `docker compose --env-file .env.example config --quiet` e `git diff --check`: exit 0.
- Cobertura nova: normalização/validação, cache e seleção sem nova rede, expiração e proprietário, limite de consultas/resultados, falha não cacheada, IDs/URLs inválidos, dados opcionais/ISBN ausente, ano da edição, chave ausente sem rede, quota/timeout/JSON malformado/oversized sem segredo. MVC verifica autenticação/CSRF, PRG, HTML escapado/atribuição, associações somente próprias, preservação de campos/valores/compra/suporte/notas, conversões inválidas e fonte/índice adulterados sem gravação durante consulta/aplicação.
- Prévia do HTML real renderizado por MockMvc em target/ui-metadata, com dados simulados: desktop e 320×780 conferidos. A 320 px documento mede 305 px, sem overflow horizontal; Tab do título chega ao seletor da fonte, com foco visível. Sem POST na prévia. Viewport restaurado, aba de conferência fechada, servidor temporário encerrado. Não é auditoria integral WCAG ou E2E da sessão real.
- Pendências: reiniciar IntelliJ para carregar a versão; Google depende de GOOGLE_BOOKS_API_KEY e cota. Não foi criada conta, assinatura ou chave, alterado .env, reiniciado app real ou modificado banco/data/. Capas, série/número/personagens, suporte e dados de aquisição continuam manuais; cobertura brasileira não garantida. Comic Vine exige esclarecimento de reutilização antes de eventual integração.
