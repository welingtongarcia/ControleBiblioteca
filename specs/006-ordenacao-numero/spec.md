# Ordenação do catálogo pelo número da edição

## Constitution → specify → clarify

Constituição preservada. Acrescentar opções sem mudar o padrão A–Z nem dados do acervo.

- NUM-001: seletor existente oferece “Número da edição: crescente” e “Número da edição: decrescente”, além das ordens atuais.
- NUM-002: ordenar no PostgreSQL antes da paginação, pelo inteiro inicial (1, 2, 10; não 1, 10, 2). Sufixos são comparados sem distinção de caixa: 1, 1A, 1B, 2. Zeros à esquerda não mudam o valor. Decrescente inverte número e sufixo. Números somente textuais vêm depois dos numerados; ausência de número fica por último nos dois sentidos. Empates usam título A–Z e UUID para estabilidade. Não inferir decimais/volumes a partir de sufixos.
- NUM-003: preservar filtros, autorização por proprietário, paginação de 20 itens, links de retorno e comportamento para ordem inválida (A–Z).

## Plan → tasks → analyze

- [ ] T1 Implementar opções e ordenação com Criteria/Specification, sem carregar o acervo inteiro em memória ou alterar esquema. Extração do inteiro inicial e sufixo com funções PostgreSQL; conversão para numeric suporta até 60 caracteres do campo. Usar índices existentes de proprietário/filtros; a nova ordenação exige sort SQL do recorte filtrado, sem alegar índice numérico específico.
- [ ] T2 Testar com PostgreSQL/Testcontainers: 2/10, sufixos, zeros iniciais, texto, ausência, empates, filtros, isolamento e páginas; MockMvc do seletor/links.
- [ ] T3 Executar suíte e registrar evidência/limitações.

Risco: campo editorial é textual; números com prefixo textual (ex. “Especial 2”) pertencem ao grupo textual. Nenhuma alteração/migração de valores existentes. A ordenação depende de PostgreSQL, já obrigatório no projeto.
