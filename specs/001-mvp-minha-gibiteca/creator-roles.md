# Funções dos criadores — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. CR-001: cadastro/edição de criador permite zero ou várias funções: Roteirista, Desenhista, Arte-finalista, Colorista, Letrista, Capista, Editor, Tradutor e Outra função. Seleção opcional por checkboxes; sem seleção significa funções não informadas. Não inferir funções dos registros antigos nem duplicar criadores por função.

CR-002: mostrar funções na lista de criadores, nas opções do formulário de edição e no detalhe da HQ. São áreas de atuação gerais do criador, não créditos específicos de uma edição. Manter associações existentes e filtros financeiros por criador sem alteração.

CR-003: edição permite adicionar/remover todas as funções. Preservar seleções em erros de validação/duplicidade, rejeitar funções desconhecidas, manter proprietário, CSRF e PRG. Campo nome continua obrigatório; funções em pt-BR e utilizáveis por teclado/celular.

## Plan → tasks → analyze

- [x] CR-T1 Migrar com Flyway V18: tabela creator_functions com PK criador/função, FK cascade e valores restritos; dados antigos ficam sem funções. Enum e coleção de funções, carregar em lote/subselect sem consulta individual por criador na lista.
- [x] CR-T2 Form/command/controlador levam seleção ao serviço; checkboxes, mensagens acessíveis e exibição das funções. Não alterar edition_creators.creator_role, que representa outro conceito.
- [x] CR-T3 Testar domínio/serviço, PostgreSQL/MockMvc (persistência, remoção, erros, CSRF e isolamento), suíte completa e QA responsivo. Registrar evidências e commit.

Impacto: coleção JPA de enums em tabela própria; nove opções limitadas. Construtor antigo de Command preservado para chamadas existentes sem funções. Nenhum serviço externo, dependência nova ou alteração de valores/leitura. Banco real não será manipulado para testes; migração aplicada pelo Flyway na próxima inicialização.

## Verificação do planejamento

Lidos AGENTS.md, constituição, especificação existente e fluxo completo de criadores/associações. Esquema atual só possui nome/ordenação; papel em edition_creators é específico do vínculo e não deve ser confundido com atuação geral. Plano atende solicitação sem exigir função única. `git diff --check` antes do commit.

## Implement → verificação

- Planejamento registrado em `d3ed76c` antes do código. V18 cria somente a tabela de funções, sem modificar registros/vínculos antigos. `CreatorFunction`, `Creator`, `CreatorService`, `CreatorForm` e `CreatorController` transportam e persistem a seleção opcional. Coleção defensiva e enum restringem valores; Hibernate SUBSELECT evita N+1 na listagem.
- Templates de criadores, formulário/detalhe de edição e `EditionCatalogViewService` exibem rótulos pt-BR, com explicação de atuação geral. DTO `NamedOption` transporta descrição sem entidades novas no formulário de HQ. CSS usa grade responsiva e foco existente.
- `CreatorFunctionsTest` e três cenários novos em `MinhaGibitecaApplicationTest`: múltiplas funções, remoção completa, cópias defensivas, erros/duplicidade com rascunho, valor desconhecido, CSRF, proprietário, exibição e preservação de vínculos. Teste PostgreSQL com 25 criadores/página de 20 verifica no máximo três statements, check constraint e exclusão das funções com criador.
- Durante desenvolvimento, corrigidos construtores de fixtures, transporte de descrição pelo DTO e estrutura duplicada do template. Testes direcionados finais passaram (5); após a correção final do HTML, `./mvnw verify -q` terminou com saída 0: **167 testes, zero falhas/erros/ignorados**. Flyway validou/aplicou V18 no PostgreSQL Testcontainers.
- QA de `target/ui-creator-functions`: novo/edição/erros em 320/390/768/1024/1440 px (15 combinações), sem overflow ou campos fora da viewport; um formulário e nove checkboxes, duas seleções preservadas na edição/erros. Capturas móvel/desktop inspecionadas; Tab de Roteirista para Desenhista mantém foco com outline de 2 px. Sem POST na prévia estática. CSS/JS carregados; servidor/aba temporários encerrados, viewport restaurado.
- `git diff --check` sem erros. Nenhum deploy ou reinício do IntelliJ executado; banco real e `data/` preservados. Necessário reiniciar a aplicação para o Flyway aplicar V18 no ambiente do usuário. Créditos por edição e filtros por função ficam fora deste pedido; não foi realizada auditoria WCAG integral.
