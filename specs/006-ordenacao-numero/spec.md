# Ordenação do catálogo pelo número da edição

## Constitution → specify → clarify

Constituição preservada. Acrescentar opções sem mudar o padrão A–Z nem dados do acervo.

- NUM-001: seletor existente oferece “Número da edição: crescente” e “Número da edição: decrescente”, além das ordens atuais.
- NUM-002: ordenar no PostgreSQL antes da paginação, pelo inteiro inicial (1, 2, 10; não 1, 10, 2). Sufixos são comparados sem distinção de caixa: 1, 1A, 1B, 2. Zeros à esquerda não mudam o valor. Decrescente inverte número e sufixo. Números somente textuais vêm depois dos numerados; ausência de número fica por último nos dois sentidos. Empates usam título A–Z e UUID para estabilidade. Não inferir decimais/volumes a partir de sufixos.
- NUM-003: preservar filtros, autorização por proprietário, paginação de 20 itens, links de retorno e comportamento para ordem inválida (A–Z).

## Plan → tasks → analyze

- [x] T1 Implementar opções e ordenação com Criteria/Specification, sem carregar o acervo inteiro em memória ou alterar esquema. Extração do inteiro inicial e sufixo com funções PostgreSQL; conversão para numeric suporta até 60 caracteres do campo. Usar índices existentes de proprietário/filtros; a nova ordenação exige sort SQL do recorte filtrado, sem alegar índice numérico específico.
- [x] T2 Testar com PostgreSQL/Testcontainers: 2/10, sufixos, zeros iniciais, texto, ausência, empates, filtros, isolamento e páginas; MockMvc do seletor/links.
- [x] T3 Executar suíte e registrar evidência/limitações.

Risco: campo editorial é textual; números com prefixo textual (ex. “Especial 2”) pertencem ao grupo textual. Nenhuma alteração/migração de valores existentes. A ordenação depende de PostgreSQL, já obrigatório no projeto.

## Implementação → verificação

- NUM-001: `CatalogOrder` inclui NUMBER_ASC/NUMBER_DESC; o seletor Thymeleaf existente usa o enum e exibe os dois rótulos pt-BR automaticamente. Ordens anteriores e fallback mantidos.
- NUM-002/003: `EditionSpecifications.orderedCatalog` ordena por grupo, prefixo numérico, sufixo, título e UUID. `regexp_substr`, `to_number` e `regexp_replace` executados no PostgreSQL; consulta de contagem não recebe ORDER BY. `EditionService.search` preserva validação de proprietário e filtros; usa Pageable sem Sort apenas para os dois novos modos, mantendo LIMIT/OFFSET no banco.
- Dois testes novos em `MinhaGibitecaApplicationTest`: ordenação natural nos dois sentidos, número de 60 dígitos, zeros iniciais/sufixos/texto/ausência; 25 edições em duas páginas, empate de título, nenhuma duplicação, proprietário isolado e filtros. MockMvc confere seletor, ordem selecionada e parâmetros nos links.
- Teste focado: `./mvnw -q '-Dtest=MinhaGibitecaApplicationTest#sortsCatalogByEditorialNumberNaturallyInBothDirections+numberOrderIsAppliedBeforePaginationAndPreservesCatalogFilters' test`, exit 0, 2 testes, 0 falhas/erros/ignorados. Acrescentado também empate de número 01/1 para a suíte final.
- Nenhuma operação no banco/acervo real ou reinicialização da aplicação. Sem novas dependências, alterações de CSS ou migração Flyway, pois não houve mudança de esquema. Não foi realizado benchmark de desempenho do novo sort.
- Verificação final: `./mvnw -q clean verify`, exit 0; 205 testes, 0 falhas, 0 erros, 0 ignorados, incluindo empate 01/1. `git diff --check`, exit 0. NUM-001/002/003 atendidos; sem pendências funcionais identificadas. Reativação da aplicação em execução permanece a cargo do usuário.
