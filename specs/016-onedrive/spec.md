# Biblioteca digital — OneDrive

Primeira integração: somente leitura via Microsoft Graph, sem download, exclusão ou movimentação de arquivos remotos. Suporta OneDrive padrão de conta pessoal ou corporativa autorizada; acesso empresarial pode depender de política do administrador. Google Drive e leitor interno ficam fora desta entrega.

Fluxos: conectar por OAuth authorization code + PKCE e state por sessão/usuário, escolher pastas, sincronizar manualmente incluindo subpastas, listar/pesquisar/paginar PDF/CBR/CBZ/EPUB/MOBI/AZW/AZW3 e vincular a edição existente. Não inferir título/ISBN, não criar gastos ou edições automaticamente. Vincular não modifica o suporte da edição.

Segurança: consentimento delegado Files.Read e offline_access; senha Microsoft nunca passa pelo aplicativo. Tokens AES-256-GCM com chave externa, separados por proprietário e fora do backup portátil. Callback fixo configurado, código/state de uso único e expiração de dez minutos; POST com CSRF para iniciar autorização e todas as mutações. URLs Graph fixas, paginação validada sem SSRF, timeouts/tamanho/quantidade/tempo limitados, links externos HTTPS Microsoft, sem logs de segredos. Reconectar limpa índice/pastas locais, desconectar apaga credenciais e índice locais sem afetar o OneDrive ou edições.

Sincronização: uma por proprietário, snapshot só publicado após leitura completa; falha mantém índice anterior. Arquivos ausentes no snapshot concluído ficam indisponíveis, preservando vínculos. Limites: 10.000 itens visitados, 1.000 pastas, 200 requisições e 90 segundos; erro por limite não publica snapshot parcial. Mudança de pastas durante sincronização bloqueada; desconexão/reconexão invalidam execução anterior.

Plano: V25 tabelas de conexão/pastas/índice e índices/FKs; configuração opcional desativada por padrão; clientes OAuth Spring Security e HTTP Graph; criptografia; serviço e repositório; interface pt-BR; documentação de registro Microsoft e variáveis Docker. Testar sem credenciais reais com respostas simuladas e PostgreSQL isolado. Não modificar .env nem registrar/autorizar conta Microsoft pelo usuário.

- [x] Persistência, configuração e OAuth.
- [x] Graph, pastas, sincronização e vínculos.
- [x] Interface, documentação e testes.

Verificação em 20/10/2026: `./mvnw -q clean verify` e, após os ajustes finais, `./mvnw -q verify` concluídos com 254 testes, zero falhas/erros/ignorados. Casos adicionais cobrem AES-GCM/AAD por proprietário, PKCE/state, callback de uso único/expirado/usuário divergente, CSRF, escopos, paginação Graph sem SSRF, erro HTTP sem exposição de tokens, facetas nulas, renovação de credenciais, recursão/deduplicação, paginação do índice, falha/limite sem publicação parcial, pasta vazia/seleção vazia, bloqueio concorrente, geração obsoleta, vínculos com edição própria, exclusão de edição e isolamento do backup.

Configuração Compose validada com `.env.example`; `git diff --check` sem problemas. Quatro telas renderizadas com dados sintéticos e inspecionadas em 320/390/768/1280 px, sem transbordamento horizontal. O código, a migração e os templates foram testados com PostgreSQL isolado e clientes Microsoft simulados. Não houve consentimento real, acesso a arquivos pessoais, edição do `.env`, migração no banco em uso, reinício da aplicação do usuário ou publicação no GitHub nesta entrega.

Próxima etapa externa: proprietário deve registrar aplicativo Web no Microsoft Entra, configurar valores locais conforme `docs/onedrive.md`, reiniciar e autorizar a conta. Não se afirma validação ponta a ponta com o provedor real. A integração permanece desativada por padrão. A seleção de pastas é uma restrição do aplicativo, não um escopo Microsoft limitado à pasta. Orçamento de 90 s pode exceder em até uma requisição em andamento de 15 s. Tokens, índice e vínculos de nuvem não são transportados pelo backup portátil.
