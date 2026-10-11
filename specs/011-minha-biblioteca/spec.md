# Minha Biblioteca: livros e quadrinhos

- BIB-001: nome visível Minha Biblioteca, incluindo login, menu, páginas, relatórios e instruções. Preservar identificadores técnicos, banco, diretórios e contas existentes.
- BIB-002: edição possui tipo de publicação QUADRINHO (COMIC) ou LIVRO (BOOK), independente de suporte físico/digital. Migrar existentes para COMIC; escolher tipo no cadastro/edição. ISBN e número editorial continuam opcionais.
- BIB-003: catálogo exibe tipo e permite filtrar/paginar por ele, preservando contexto de navegação. Detalhes mostram tipo. Livros reutilizam capas, páginas/leitura, estrelas, valores, exemplares, criadores e obras; vínculos específicos de quadrinhos continuam opcionais.
- BIB-004: resumo/financeiro/relatórios existentes incluem ambas as publicações, sem nova contabilidade. Backup atual transporta tipo. Sem inferir tipo a partir de ISBN/metadados. Dados existentes e propriedade preservados.

Plano: Flyway V21 tipo obrigatório com default COMIC e índice proprietário/tipo/título; enum/domínio/DTO/form/serviço; filtros e links; textos genéricos de publicações e marca. Testes de migração, cadastro/edição, filtros/isolamento, finanças/leitura e backup; suíte e interface.

- [x] Persistência e fluxos de livros.
- [x] Nome/interface/filtros/contexto.
- [x] Verificação e documentação.

Verificação em 2026-10-11: `./mvnw -q clean verify` concluído com 229 testes, sem falhas, erros ou testes ignorados. Testes adicionais cobrem cadastro/edição de livro digital e físico, validação do tipo, valores/páginas no resumo, filtro antes da paginação, isolamento por usuário, contexto de retorno, migração de dados anteriores e backup/restauração de ambos os tipos. Prévia HTML gerada com dados sintéticos: cadastro, edição, detalhes e catálogo de livros verificados nas larguras 320, 390, 768 e 1280 px, sem transbordamento horizontal.

Não foi alterado o banco em uso nem reiniciada a aplicação do usuário. A migração é aplicada automaticamente no próximo início. Backup mantém a política existente de compatibilidade por versão de esquema; gerar um novo backup depois de atualizar. ISBN/metadados não mudam automaticamente o tipo escolhido.
