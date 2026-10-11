# Lista de desejos

- DES-001: área privada por usuário no grupo Minha coleção, para livros e quadrinhos desejados, independente do acervo.
- DES-002: título obrigatório; tipo Livro/Quadrinho, autor, editora, ISBN, preço estimado não negativo, prioridade e observações. Campos bibliográficos livres/opcionais, sem criar cadastros auxiliares.
- DES-003: busca por título/autor/ISBN, filtro pendentes/adquiridos/todos, paginação 20, prioridade alta primeiro e título; editar, excluir com confirmação, marcar adquirido e desfazer. Não criar edição ou gasto automaticamente.
- DES-004: CSRF e autorização por proprietário em todas as ações; validação e mensagens pt-BR; backup inclui desejos; preservar dados existentes.

Plano: V22 tabela própria, índice por proprietário/prioridade/título; domínio DTO/enums, repositório JDBC parametrizado, serviço transacional, formulário validado, controller MVC, telas com padrões atuais e navegação. Testar CRUD, valores, XSS, CSRF, isolamento, paginação/filtros e backup; verificar suíte e renderização.

- [x] Persistência, serviço e cadastro.
- [x] Lista, ações e navegação.
- [x] Backup, testes e documentação.

Verificação em 2026-10-11: `./mvnw -q verify` concluído com 233 testes, zero falhas/erros/ignorados. Cobertura adicional: CRUD, adquirido/pendente e preservação de situação ao editar, validação monetária e enums, conteúdo escapado, CSRF, acesso anônimo e isolamento entre proprietários, estimativa sem contabilização no acervo, prioridade antes da paginação e busca literal; exportação/restauração de desejos privados. Formulário, lista com textos longos e erros de validação renderizados e inspecionados em 320, 390, 768 e 1280 px, sem transbordamento horizontal. `git diff --check` sem problemas.

Banco em uso e processo do usuário não foram alterados. Reiniciar a aplicação aplica a V22 automaticamente. Backup continua exigindo compatibilidade de versão de esquema; gerar novo backup após atualização. Não há importação automática do desejo no catálogo, para evitar inventar dados da edição ou valor efetivamente pago.
