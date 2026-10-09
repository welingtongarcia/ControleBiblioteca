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

- [ ] M1 DTO, serviço com cache/limites, validação e seleção segura de candidato.
- [ ] M2 Adaptadores Open Library/Google Books com opcionais seguros e erros sem segredos.
- [ ] M3 Integração MVC/Thymeleaf, preservação e seleção explícita com fonte/atribuição.
- [ ] M4 Testes de serviços/adaptadores/MVC, build completo e documentação de execução.

## Análise pré-implementação

Sem mudança de esquema ou migração; dados bibliográficos passam por DTO e campos existentes. Sem novos contratos pagos. Fontes e índices restritos no servidor, CSRF e autenticação herdados; nenhum valor ou data financeira inferido. Critérios cobertos por M1–M4. APIs não garantem cobertura brasileira: revisão manual obrigatória e erros externos não bloqueiam cadastro. Spec Kit não executado nesta revisão; artefatos SDD equivalentes.

Fontes primárias: https://openlibrary.org/dev/docs/api/search ; https://openlibrary.org/developers/api ; https://developers.google.com/books/docs/v1/using ; https://comicvine.gamespot.com/api/ .

## Validação do planejamento

AGENTS.md, prompt mestre, constituição e integração ISBN lidos; Java 21.0.12.1 e Docker 29.9.0 disponíveis. Git: somente data/ não rastreado preexistente, preservado. Implementação autorizada somente após esta análise.
