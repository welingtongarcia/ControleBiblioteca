# Fotos dos criadores

- FOTO-001: foto opcional PNG/JPEG no formulário novo/editar; listar miniatura; substituir e remover em edição. Cadastros anteriores continuam sem foto.
- FOTO-002: proprietário autoriza toda leitura/mutação; CSRF; até 5 MB e 40 milhões de pixels; conferir formato real antes de decodificar; nomes UUID gerados, nunca nome original; armazenamento de arquivos já configurado, não BLOB.
- FOTO-003: cadastro/dados e foto na mesma transação; erro preserva campos e não altera criador/foto; limpar arquivo novo em rollback, antigo somente depois do commit. Exclusão bem-sucedida do criador remove foto; referências bloqueiam sem removê-la.
- FOTO-004: backup/restauração incluem arquivos e metadados de fotos, com integridade, isolamento e limites existentes. Interface pt-BR e responsiva.

Plano: V23 metadados em creators; serviço de foto e serviço de cadastro transacional; multipart e binder restrito; endpoint privado; prévia/listagem; ampliar transporte de imagens no backup. Testes reais de upload, substituição, remoção, rejeição, isolamento, rollback e backup.

- [x] Persistência, upload e transações.
- [x] Formulários, miniaturas e proteção.
- [x] Backup, testes e revisão.

Verificação em 2026-10-11: `./mvnw -q verify` concluído com 238 testes, zero falhas, erros ou ignorados. Testes novos exercitam criação multipart com foto, conteúdo servido ao proprietário, miniaturas, troca e remoção, CSRF, isolamento, rejeição de SVG/MIME incompatível/arquivo inválido/acima de 5 MB, rollback do cadastro e dos dados de edição mantendo foto anterior, remoção do arquivo na exclusão do criador e limpeza de arquivo novo em falha de commit. Backup/restauração transportam fotos com novo identificador e preservam privacidade. Cadastro, edição, listagem e erro inspecionados em 320, 390, 768 e 1280 px, sem transbordamento horizontal.

Fotos armazenadas no diretório já configurado por `app.cover-storage-path`, com metadados separados dos de capa e prefixo `fotos/` no ZIP. Remoção física pós-commit é best-effort, como no armazenamento de capas; falha de permissão/disco pode deixar um arquivo órfão não acessível pelo sistema. Banco em uso não alterado e processo do usuário não reiniciado; V23 será aplicada na próxima inicialização. Gerar novo backup após atualizar; a regra de compatibilidade por versão de esquema permanece.
