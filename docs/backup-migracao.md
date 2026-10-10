# Backup e mudança de máquina

Na instalação de origem, entre na sua conta e abra **Painéis e análises → Backup e migração → Baixar backup completo**. Guarde o ZIP em um local privado e uma cópia da mesma versão do projeto/JAR. O ZIP é pessoal, não criptografado, e não contém contas, senhas, `.env`, API keys nem o programa. Não é um dump de todo o servidor.

Na nova máquina (Linux, Windows ou macOS), instale a versão preservada conforme [installation.md](installation.md), usando Java 21/PostgreSQL ou Docker Compose. Configure o banco e as credenciais locais. Entre em uma conta vazia e use **Restaurar coleção**, escolhendo o ZIP e marcando a confirmação. Não descompacte diretamente na aplicação.

São preservados editoras, obras, séries, criadores e funções, personagens, locais e hierarquia, edições e créditos, capas PNG/JPEG, suporte físico/digital, preços, datas, leitura e páginas, exemplares, checklist e registros financeiros legados. IDs são recriados, vínculos remapeados e o proprietário passa a ser a conta de destino. Nenhum cadastro existente é excluído ou mesclado.

Confira valores, datas, checklist, capas, leitura e relatórios na nova instalação antes de desativar a antiga. Mantenha o ZIP original e a instalação antiga até essa conferência. Se a restauração falhar, o banco retorna ao estado anterior e novas capas são removidas; corrija o problema e tente novamente. Versão de formato e esquema Flyway diferentes exigem uma instalação compatível; não edite o manifesto para contornar a validação.

Limites atuais: ZIP 250 MB, expansão 512 MB, dados JSON 25 MB, capa 5 MB / 40 milhões de pixels, 20.000 entradas e 100.000 registros. Coleções maiores exigem uma solução administrativa de backup integral. Arquivos corrompidos, caminhos não permitidos, vínculos externos e capas ausentes são rejeitados. SHA-256 verifica integridade, não autenticidade: importe apenas backups confiáveis. A geração inclui todo o acervo, sem truncamento; alteração simultânea de uma capa pode exigir gerar o backup novamente.

## Armazenamento de capas no Docker

Novas instalações usam `postgres-data` para o banco e `covers-data` para `/app/data/capas`. Os dois volumes precisam ser preservados em backups administrativos. Nunca execute `docker compose down -v` se quiser manter seus dados.

**Instalações Docker anteriores:** não recrie um contêiner antigo antes de salvar as capas que ainda estejam no seu sistema de arquivos. Primeiro faça o backup portátil na versão antiga/atualizada com acesso às capas, ou copie a pasta de capas do contêiner para local seguro. A adição do volume não transporta automaticamente arquivos de um contêiner antigo. Instalações Java/IntelliJ continuam usando `COVER_STORAGE_PATH` (padrão `./data/capas`); mantenha essa pasta intacta.
