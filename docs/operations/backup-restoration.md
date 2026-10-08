# Backup e restauração — plano do MVP

Ainda não executável: este procedimento será implementado e testado na T019.

1. Colocar a aplicação em modo de manutenção ou assegurar consistência entre banco e diretório de capas.
2. Gerar dump lógico PostgreSQL com `pg_dump` em formato custom, sem incluir segredos.
3. Copiar o diretório/volume de capas configurado preservando estrutura e metadados.
4. Armazenar dump e capas juntos, com data, versão de schema Flyway e checksum.
5. Para restaurar, subir PostgreSQL vazio na versão suportada, executar migrations, restaurar o dump e reidratar as capas no storage configurado.
6. Validar health, contagens de exemplares/edições e amostra de leitura de capas antes de liberar uso.

O teste deve registrar duração, versão de schema, checksums e resultado em documentação de Fase 6. Um dump PostgreSQL sem as capas não é considerado backup completo.
