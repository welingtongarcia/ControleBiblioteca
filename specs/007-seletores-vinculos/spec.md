# Seletores pesquisáveis de vínculos

## Constitution → specify → clarify

MVC/Thymeleaf/PostgreSQL preservados; JavaScript próprio como melhoria progressiva, sem frontend separado.

- SEL-001: busca independente de obras, criadores e personagens no banco, sem distinção de caixa/acentos portugueses, com até 20 resultados por página. Pesquisa literal (%, _ e barra não são curingas), tamanho máximo 180; páginas limitadas a 0–500. Ordem estável por nome e UUID. Criadores filtráveis por função cadastrada, sem inferência.
- SEL-002: escolhidos acima dos resultados, contador, remoção individual, sem duplicação e sem perder escolhas ao pesquisar, paginar, editar ou receber erro do formulário. A renderização inicial traz somente primeira página mais todos os IDs selecionados autorizados, nunca todos os cadastros. Outros campos da edição não são alterados pela busca.
- SEL-003: endpoints GET autenticados com proprietário da sessão, DTOs, no-store, parâmetros validados e SQL parametrizado. Persistência permanece no POST atual com CSRF e autorização de vínculos. Sem criação automática de registros.
- SEL-004: labels explícitos, botões nativos, resultados e status anunciados, uso por teclado, mensagem vazia/erro/repetição de pesquisa, debounce e cancelamento de buscas antigas. Dados exibidos com textContent, não HTML. Sem JS, primeira página e seleções existentes continuam com checkboxes; aviso explícito para habilitar busca completa. Não remover seleções no erro de rede.

## Plan → tasks → analyze

- [ ] T1 Implementar repositório paginado e serviço de opções/seleções; controlador JSON e integração ao formulário.
- [ ] T2 Fragmento reutilizável, JS próprio, filtro de criadores e CSS responsivo; fallback honesto.
- [ ] T3 Testcontainers/MockMvc: limites, acentos, proprietário, função, paginação, seleção fora da primeira página, erros/edição. QA navegador e clean verify.

Análise: listas atuais carregam todo o acervo; opções de criadores podem provocar consultas adicionais de funções. Usar JDBC com agregação de funções em uma consulta por conjunto, índices existentes de proprietário e creator_functions; busca por substring normalizada fará scan do recorte do proprietário, sem extensão unaccent ou alteração de esquema. Não alegar benchmark. ISBN/metadados mantêm métodos de correspondência existentes fora deste escopo.
