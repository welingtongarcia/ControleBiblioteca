# Avaliação pessoal de HQs

- AVA-001: cada edição do usuário recebe nota inteira de 0 a 5; sem avaliação (null) é diferente de nota zero. Registros existentes começam sem avaliação.
- AVA-002: detalhes permitem escolher uma nota por botões de estrelas, atribuir zero e remover avaliação. Catálogo exibe a nota atual. Avaliação independe da leitura, suporte ou número de exemplares.
- AVA-003: persistir por Flyway com constraint no PostgreSQL; autorizar proprietário antes de toda alteração, inclusive entrada inválida. Manter CSRF, PRG, navegação contextual, mensagens pt-BR e controles nativos acessíveis sem JavaScript.
- AVA-004: preservar avaliação ao editar outros dados e no backup/restauração existente.

Plano: adicionar rating nullable na edição/DTO, serviço transacional e formulário validado; renderizar controles nos detalhes e resumo no catálogo. Sem mudanças na ordenação, relatórios ou metadados externos.

Tarefas:
- [x] Persistência/domínio/serviço e endpoints protegidos.
- [x] Interface de estrelas e catálogo.
- [x] Testes de limites, zero/ausência/remoção, proprietário, CSRF, persistência, edição e backup; executar verificação.

## Implementação e evidências

V20 adiciona rating integer nullable com constraint 0–5; Edition.rate valida o domínio. EditionView expõe nota/rótulo/estrelas. EditionRatingService autoriza por proprietário e persiste; EditionRatingController usa formulário com @NotNull/@Min/@Max, campos permitidos e PRG, incluindo remoção explícita.

Fragmento rating.html nos detalhes: botões nativos com nomes acessíveis e estado aria-pressed, nota zero e remoção; catálogo exibe resumo textual e estrelas. CSS com foco visível, alvos mínimos de 44px e quebra de linha para telas pequenas. Sem JavaScript ou novas dependências.

EditionRatingTest: 2 testes de domínio/serviço. MinhaGibitecaApplicationTest: novo fluxo MVC/PostgreSQL cobrindo todas as notas, null, persistência, edição preservada, entradas inválidas, CSRF, proprietário, contexto e constraint SQL. BackupIntegrationTest confirma restauração de nota 4 e nota zero; backup usa colunas do esquema e inclui rating automaticamente.

Primeira tentativa de clean verify falhou na validação de esquema (smallint versus Integer); migração corrigida para integer antes de qualquer implantação real. Verificação final: `./mvnw -q clean verify`, exit 0; 217 testes, 0 falhas, 0 erros, 0 ignorados. `git diff --check`: exit 0.

Sem acesso/mutação do acervo real, data/ preservado; aplicação não reiniciada. HTML renderizado validado por MockMvc, sem inspeção visual em navegador nesta entrega. O backup existente exige versão de esquema correspondente: backups anteriores à V20 continuam exigindo a versão em que foram gerados; novos backups preservam avaliações. Reiniciar aplicação para executar a migração Flyway e gerar novos backups na versão atual.
