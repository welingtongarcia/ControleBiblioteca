# Funções dos criadores — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. CR-001: cadastro/edição de criador permite zero ou várias funções: Roteirista, Desenhista, Arte-finalista, Colorista, Letrista, Capista, Editor, Tradutor e Outra função. Seleção opcional por checkboxes; sem seleção significa funções não informadas. Não inferir funções dos registros antigos nem duplicar criadores por função.

CR-002: mostrar funções na lista de criadores, nas opções do formulário de edição e no detalhe da HQ. São áreas de atuação gerais do criador, não créditos específicos de uma edição. Manter associações existentes e filtros financeiros por criador sem alteração.

CR-003: edição permite adicionar/remover todas as funções. Preservar seleções em erros de validação/duplicidade, rejeitar funções desconhecidas, manter proprietário, CSRF e PRG. Campo nome continua obrigatório; funções em pt-BR e utilizáveis por teclado/celular.

## Plan → tasks → analyze

- [ ] CR-T1 Migrar com Flyway V18: tabela creator_functions com PK criador/função, FK cascade e valores restritos; dados antigos ficam sem funções. Enum e coleção de funções, carregar em lote/subselect sem consulta individual por criador na lista.
- [ ] CR-T2 Form/command/controlador levam seleção ao serviço; checkboxes, mensagens acessíveis e exibição das funções. Não alterar edition_creators.creator_role, que representa outro conceito.
- [ ] CR-T3 Testar domínio/serviço, PostgreSQL/MockMvc (persistência, remoção, erros, CSRF e isolamento), suíte completa e QA responsivo. Registrar evidências e commit.

Impacto: coleção JPA de enums em tabela própria; nove opções limitadas. Construtor antigo de Command preservado para chamadas existentes sem funções. Nenhum serviço externo, dependência nova ou alteração de valores/leitura. Banco real não será manipulado para testes; migração aplicada pelo Flyway na próxima inicialização.

## Verificação do planejamento

Lidos AGENTS.md, constituição, especificação existente e fluxo completo de criadores/associações. Esquema atual só possui nome/ordenação; papel em edition_creators é específico do vínculo e não deve ser confundido com atuação geral. Plano atende solicitação sem exigir função única. `git diff --check` antes do commit.
