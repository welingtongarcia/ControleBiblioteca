# Função Escritor

Adicionar ESCRITOR (rótulo Escritor) às funções gerais dos criadores, preservando as anteriores. Disponível no cadastro/edição, seletores bibliográficos e filtro/descrição de relatórios. Formulário permite selecionar todas as dez funções. Flyway V24 amplia a restrição existente sem recriar registros. Backup mantém e restaura a nova função.

- [x] Enum, validação e migração.
- [x] Relatórios e testes de cadastro/filtros/backup.

Verificação: `./mvnw -q verify` em 2026-10-11, 239 testes, zero falhas/erros/ignorados. Cadastro de Escritor, persistência PostgreSQL, busca bibliográfica pela função, rótulo no CSV, seleção das dez funções e transporte/restauração pelo backup verificados. Migração não modifica funções anteriores; aplicada na próxima inicialização, sem alterar o banco em uso neste trabalho. `git diff --check` sem problemas.
