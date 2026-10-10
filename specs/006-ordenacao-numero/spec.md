# Ordenação do catálogo pelo número da edição

## Constitution → specify → clarify

Constituição preservada. Acrescentar opções sem alterar dados do acervo; o padrão passa a ser título A–Z com número crescente conforme NUM-005.

- NUM-001: seletor existente oferece “Número da edição: crescente” e “Número da edição: decrescente”, além das ordens atuais.
- NUM-002: ordenar no PostgreSQL antes da paginação, pelo inteiro inicial (1, 2, 10; não 1, 10, 2). Sufixos são comparados sem distinção de caixa: 1, 1A, 1B, 2. Zeros à esquerda não mudam o valor. Decrescente inverte número e sufixo. Números somente textuais vêm depois dos numerados; ausência de número fica por último nos dois sentidos. Empates usam título A–Z e UUID para estabilidade. Não inferir decimais/volumes a partir de sufixos.
- NUM-003: preservar filtros, autorização por proprietário, paginação de 20 itens e links de retorno. Ordem inválida usa o padrão definido em NUM-005.
- NUM-004 (extensão): acrescentar “Título A–Z + número crescente” e “Título A–Z + número decrescente”. Agrupar por título normalizado (sem distinção de caixa), primeiro títulos A–Z e depois ordenação natural dentro de cada título. Não agrupar por série, remover números do título, fundir títulos diferentes nem alterar os cadastros. Trata-se de manter cartões contíguos na lista existente, não seções/contagens novas. Um grupo pode continuar na página seguinte. Ausência de número por último dentro do próprio título.
- NUM-005: abrir o catálogo por padrão em “Título A–Z + número crescente” (TITLE_NUMBER_ASC), inclusive quando o parâmetro de ordem estiver ausente, vazio ou inválido. Preservar escolhas explícitas das demais ordens e propagar a ordem selecionada nos links de paginação.

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

## Extensão: agrupamento por título → plano e tarefas

- [x] T4 Reutilizar ordenação numérica no banco, acrescentando título como primeiro critério apenas nas duas novas opções. Preservar opções anteriores e desempate estável.
- [x] T5 Cobrir títulos intercalados, caixa, número ausente, ordem nos dois sentidos, filtros e continuidade de grupos/paginação. Conferir seletor e links via MockMvc; executar clean verify e registrar resultados.

Análise: o agrupamento deve ocorrer antes do LIMIT/OFFSET para não intercalar títulos entre páginas. Não exige esquema novo, listas em memória ou nova camada de apresentação. Se títulos foram cadastrados com números embutidos, são títulos diferentes e não serão deduzidos como iguais.

Implementação NUM-004: `CatalogOrder` inclui TITLE_NUMBER_ASC/TITLE_NUMBER_DESC e identifica agrupamento por título; `EditionSpecifications` antepõe normalizedTitle aos critérios numéricos somente nesses modos. Seletor existente lista os rótulos automaticamente. Novo teste `groupsCatalogByTitleAndOrdersNumbersWithinEachGroupAcrossPages` cobre Batman/BATMAN e Superman intercalados, 25 edições em duas páginas, ausência de número dentro do título, filtros de pesquisa/editora, registros de outro proprietário excluídos, ordem selecionada e parâmetros de paginação via MockMvc. Sem mudanças de templates, CSS, esquema ou dados reais.

Verificação da extensão: `./mvnw -q clean verify`, exit 0, 206 testes, 0 falhas, 0 erros, 0 ignorados. `git diff --check`, exit 0. NUM-004 atendido, ordens NUM-001/002/003 preservadas. Não houve reinicialização da aplicação real ou benchmark do sort; grupos são definidos pelo título cadastrado, não por identificação automática de coleção.

## Extensão: ordenação padrão → plano e tarefas

- [ ] T6 Centralizar o fallback em CatalogOrder.parse e remover o padrão antigo do controller; reutilizar a ordenação SQL existente, sem mudanças de esquema ou dados.
- [ ] T7 Testar via MockMvc ordem ausente, vazia e inválida, títulos A–Z e números 1/2/10 crescentes, escolha explícita preservada e paginação; executar a suíte e registrar resultados.

Análise NUM-005: apenas a seleção inicial muda. Os critérios naturais e o agrupamento já implementados continuam aplicados antes da paginação no PostgreSQL. O seletor existente acompanha selectedOrder automaticamente; não requer mudanças de interface, dependências ou migração.
