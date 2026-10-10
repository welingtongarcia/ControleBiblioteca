# Seletores pesquisáveis de vínculos

## Constitution → specify → clarify

MVC/Thymeleaf/PostgreSQL preservados; JavaScript próprio como melhoria progressiva, sem frontend separado.

- SEL-001: busca independente de obras, criadores e personagens no banco, sem distinção de caixa/acentos portugueses, com até 20 resultados por página. Pesquisa literal (%, _ e barra não são curingas), tamanho máximo 180; páginas limitadas a 0–500. Ordem estável por nome e UUID. Criadores filtráveis por função cadastrada, sem inferência.
- SEL-002: escolhidos acima da busca e dos resultados, contador, remoção individual, sem duplicação e sem perder escolhas ao pesquisar, paginar, editar ou receber erro do formulário. A renderização inicial traz somente primeira página mais todos os IDs selecionados autorizados, nunca todos os cadastros. Outros campos da edição não são alterados pela busca.
- SEL-003: endpoints GET autenticados com proprietário da sessão, DTOs, no-store, parâmetros validados e SQL parametrizado. Persistência permanece no POST atual com CSRF e autorização de vínculos. Sem criação automática de registros.
- SEL-004: labels explícitos, botões nativos, resultados e status anunciados, uso por teclado, mensagem vazia/erro/repetição de pesquisa, debounce e cancelamento de buscas antigas. Dados exibidos com textContent, não HTML. Sem JS, primeira página e seleções existentes continuam com checkboxes; aviso explícito para habilitar busca completa. Não remover seleções no erro de rede.

## Plan → tasks → analyze

- [x] T1 Implementar repositório paginado e serviço de opções/seleções; controlador JSON e integração ao formulário.
- [x] T2 Fragmento reutilizável, JS próprio, filtro de criadores e CSS responsivo; fallback honesto.
- [x] T3 Testcontainers/MockMvc: limites, acentos, proprietário, função, paginação, seleção fora da primeira página, erros/edição. QA navegador e clean verify.

Análise: listas atuais carregam todo o acervo; opções de criadores podem provocar consultas adicionais de funções. Usar JDBC com agregação de funções em uma consulta por conjunto, índices existentes de proprietário e creator_functions; busca por substring normalizada fará scan do recorte do proprietário, sem extensão unaccent ou alteração de esquema. Não alegar benchmark. ISBN/metadados mantêm métodos de correspondência existentes fora deste escopo.

## Implementação → verificação

- SEL-001/003: `AssociationOptionsRepository`, `AssociationOptionsService` e `AssociationOptionsController` entregam GET `/edicoes/vinculos/{WORK|CREATOR|CHARACTER}` autenticado e no-store. SQL parametrizado, tabela/colunas determinadas exclusivamente pelo enum, LIMIT 21 (20 retornados + indicador de próxima página), normalização de acentos via translate/lower, escapes literais e filtro de função com EXISTS. Funções agregadas numa consulta, sem N+1 de leitura de entidades.
- SEL-002: `EditionController.form` carrega somente primeira página mais seleções autorizadas por ID. Fragmento `association-picker.html`, `association-picker.js` e CSS compartilhado exibem contador, chips com remoção e inputs ocultos com nomes originais; marcadores Spring preservados. POST/CSRF/validações de proprietário e transação existentes não foram modificados. Edição e erros de validação reconstroem escolhas mesmo fora da primeira página.
- SEL-004: botões nativos, labels, status ao vivo por seção, foco explícito no status após paginação e na busca após adicionar/remover, debounce de 300 ms, AbortController/geração para evitar resposta antiga. Sem innerHTML com dados. Seleções mantidas em vazio/falha. CSS conferido em 320/390/768/1440 px com nomes longos, sem overflow de página; chips empilhados no celular. Sem JS, checkboxes da página inicial e vínculos selecionados permanecem disponíveis, com aviso do limite da busca.
- Testes: três casos Mockito de serviço (limites, paginação, merge/deduplicação); três casos novos em `InterfaceIntegrationTest` com PostgreSQL real via Testcontainers e MockMvc (todos os tipos, acentos/caixa, curingas literais, isolamento, função, autenticação/parâmetros, seleção fora da página inicial e persistência/validação dos três vínculos). Auditoria HTML existente cobre labels, IDs e ARIA.
- Primeiro ciclo focado encontrou duas regressões de apresentação: ordem das funções e mensagens da conta vazia. Restauradas e confirmadas: execução focada `-Dtest=InterfaceIntegrationTest,MinhaGibitecaApplicationTest test`, exit 0, 68 testes. Uma tentativa de suíte completa foi interrompida com exit 143; não considerada aprovação.
- `./mvnw -q clean verify`, exit 0, 212 testes, 0 falhas/erros/ignorados. Depois do ajuste final de foco por teclado: `./mvnw -q -Dtest=InterfaceIntegrationTest verify`, exit 0, 8 testes, e pacote recomposto com recursos atuais. `git diff --check`, exit 0.
- QA navegador com HTML e DTOs sintéticos gerados por MockMvc, servidos por prévia local (respostas de pesquisa simuladas apenas nessa prévia): adicionar nos três campos, pesquisa sem perda, botão já selecionado, remoção, paginação, filtro de desenhista, vazio e falha de rede, foco com Enter/Tab, edição/validação com escolhas fora da página inicial e quatro larguras. O comportamento real dos endpoints/persistência foi verificado pelos testes acima, não por sessão no banco do usuário. Servidor/tab temporários encerrados e viewport restaurado.

Limites e riscos: sem benchmark, teste de leitor de tela ou certificação WCAG completa. Busca cobre acentos portugueses comuns; não promete transliteração universal. Paginação até 501 páginas (refine a busca para bases maiores). Sem JS não há pesquisa completa; o fallback é explícito. Nenhuma alteração de esquema, novos requisitos de build, registros reais, serviços externos ou reinicialização da aplicação. SEL-001/002/003/004 atendidos, sem pendência funcional identificada.
