# OneDrive por rclone — SDD

## Constituição e escopo

Aplicam-se AGENTS.md e a constituição existente: Java 21/MVC/Thymeleaf, CSRF, proprietário por consulta e mutação, pt-BR, nenhuma alteração remota ou exposição de credenciais. Complementa `016-onedrive` sem remover OAuth direto. Não requer conta Azure própria; usa o cliente compartilhado do rclone. Consentimento Microsoft continua obrigatório.

## Especificação e esclarecimentos

- RC-01: habilitação opcional, sem impedir inicialização se o executável ou a autorização ainda não existir. Instalar rclone separadamente para execução nativa; imagem Docker incluirá o programa.
- RC-02: responsável configura no computador servidor, interativamente, um remote chamado `onedrive`, do tipo `onedrive`, por usuário. Arquivo exclusivo `<diretório configurado>/<UUID autenticado>.conf`. A interface mostra somente o UUID da própria conta e orientações; nunca aceita comandos, nomes de executável ou caminhos de configuração do navegador. Login Microsoft somente no navegador do rclone, não coletado pela aplicação.
- RC-03: botão POST com CSRF verifica configuração e acesso antes de ativar. Separação por proprietário, sem conexão compartilhada entre contas. Reconectar/trocar provedor limpa seleção/índice/vínculos com aviso prévio; desconectar remove apenas dados no banco, não revoga nem apaga a configuração rclone.
- RC-04: reutilizar seleção de até 20 pastas, navegação, sincronização manual recursiva, formatos, paginação, filtros e vínculos existentes. Comandos limitados a `listremotes --long` e `lsjson`/`--stat`, sem shell, sem downloads, uploads, links públicos ou operações de escrita. Configuração externa pode renovar seu token e deve ser gravável somente pelo usuário do serviço.
- RC-05: somente comandos de metadados em lista permitida, `--dry-run` como defesa adicional e escopos `Files.Read offline_access`, sem herdar variáveis RCLONE_*; stdin fechado; stderr descartado sem logs; JSON limitado a 16 MiB, processo a 20 segundos. Mesmos limites da sincronização existente de itens/pastas/consultas e orçamento de 90 s, podendo exceder por uma consulta de até 20 s. Falha não publica índice parcial. Rejeitar caminhos relativos perigosos/absolutos/remotes arbitrários, atalhos simbólicos de configuração e dados inválidos.
- RC-06: arquivo de configuração contém tokens e fica fora do Git, contexto Docker e backup portátil. Provedor rclone marcado explicitamente na conexão existente, sem tokens no banco; não alterar migrações aplicadas. `lsjson` não fornece link Web confiável: não gerar link de compartilhamento nem exibir botão de abertura para esses itens. Pastas movidas devem ser selecionadas novamente; IDs de arquivos preservados quando o backend fornece ID.
- RC-07: documentação de instalação Linux/Windows/macOS, consentimento somente leitura e configuração Docker; nenhuma cobrança pelo rclone, sujeitos a limites do cliente compartilhado/Microsoft e planos de armazenamento existentes. Não prometer conexão real sem consentimento executado.

## Plano e análise

Adicionar settings, executor seguro e adaptador rclone na infraestrutura; serviço existente escolhe adaptador pelo marcador de provedor da conexão, preservando transações de publicação e autorização. Controller adiciona ativação rclone; templates explicam configuração por conta e ambos os modos. Não há frontend separado nem dependência nova de build. Testes unitários de subprocesso/JSON e MockMvc+PostgreSQL com executor simulado; executar também rclone real contra dados locais sintéticos, sem conta pessoal, se o binário estiver disponível.

## Tarefas rastreáveis

- [x] RC-01/02/05: configuração e executor seguro, testes de limites e isolamento.
- [x] RC-03/04/06: adaptador, serviço, rotas e interface; regressão OAuth e testes de persistência.
- [x] RC-07: guias e configuração Docker; validar build/testes e registrar evidências/limitações.

Riscos: configuração rclone confiada ao administrador do servidor, tokens externos sem criptografia própria da aplicação (permissões restritas necessárias), expiração/revogação e throttling Microsoft, integração real dependente do usuário. Não habilitar ou autorizar conta real automaticamente.

Esclarecimento após execução com rclone 1.60.1 do repositório Ubuntu: `--read-only` não é flag global para `lsjson`/`listremotes`. Utilizar somente os comandos de metadados permitidos, consentimento de leitura e `--dry-run`, compatível com essa versão. Isso não desativa a renovação local de tokens.

## Verificação — 23/10/2026

Correção durante a configuração real no Linux: rclone 1.60.1 substitui `access_scopes` por permissões amplas quando `disable_site_permission=true`. Exigir `--onedrive-disable-site-permission=false` e `Files.Read offline_access` explicitamente, inclusive no consentimento inicial. Não aprovar solicitações com edição/exclusão. Registrar regressão no executor e conferir o consentimento no navegador antes da autorização pelo usuário.

Verificação dessa correção: `./mvnw -q -Dtest=RcloneClientTest test`, saída 0, 9 testes sem falhas/erros/ignorados. Documentação e executor ajustados. Rclone instalado no diretório `.local/bin` do usuário Linux a partir de pacote Ubuntu cujo SHA-256 foi conferido contra os metadados APT. Configuração local habilitada a pedido do usuário; diretório privado 700, arquivo de autorização 600, ambos e `.env` ignorados pelo Git. A primeira tela Microsoft exibiu permissões maiores e o assistente de autorização foi interrompido; foi constatada autorização salva. Usuário concordou em revogar e refazer o consentimento, e a página de gerenciamento foi aberta para ação dele. Configuração real permanece **pendente da revogação e nova autorização somente leitura**; não houve indexação ou modificação de arquivos pessoais. A aplicação existente não foi reiniciada. Nenhum segredo foi incluído no commit.

`./mvnw -q clean verify`: saída 0; 31 suítes, 267 testes, zero falhas/erros/ignorados. Inclui 9 testes do adaptador/executor rclone (subprocessos Java reais, entrada fechada, ambiente sem RCLONE_*, saída limitada e encerramento após 20 s), 13 testes de integração da biblioteca digital com PostgreSQL isolado/MockMvc (incluindo OAuth prévio e rclone simulado), além das regressões da aplicação. O modo rclone foi exercitado com OAuth direto desativado, sem chave, ID ou segredo Azure.

JAR resultante iniciado em `127.0.0.1:18081` com `--app.rclone.enabled=true --app.onedrive.enabled=false`: inicialização em 8,059 s, readiness UP e login HTTP 200; processo de verificação encerrado normalmente, sem interromper a instância do usuário em 8080. Nenhuma migração adicional foi necessária além da V25 já existente.

Rclone 1.60.1 obtido do repositório Ubuntu e extraído em diretório temporário: `version`, `listremotes --long` com configuração sintética e `lsjson --stat` de diretório temporário executados com as flags finais, saída 0. Não houve consulta a arquivos Microsoft nem autorização real. O programa temporário não foi instalado no PATH do usuário.

Interface renderizada por MockMvc com dados sintéticos e conferida no navegador em 320/390/768/1280 px: sem transbordamento horizontal, botões e instruções presentes, alternativa OAuth recolhível. Servidor de prévia e aba de teste encerrados após a revisão.

`docker compose --env-file .env.example config --quiet` e `git diff --check`: saída 0. Dockerfile inclui rclone e Compose inclui volume privado; build completo de imagem foi interrompido durante o download lento das imagens base, portanto a imagem Docker final não foi validada nesta sessão. Uma execução intermediária de testes sobreposta à limpeza de `target/` gerou erros de templates ausentes; não foi aceita como evidência. A rodada limpa final acima foi concluída depois, sem outra execução Maven concorrente.

Arquivos: infraestrutura `RcloneSettings`, `RcloneCommandRunner`, `RcloneClient`; serviço/controller/template existentes; configuração YAML/exemplo de ambiente/Docker; testes `RcloneClientTest` e `OneDriveIntegrationTest`; `docs/onedrive-rclone.md`, README e referência no guia OAuth. Evidências temporárias: `/tmp/rclone-release-verify.log`, `/tmp/rclone-jar-smoke.log`, relatórios em `target/surefire-reports/`.

Pendências externas: instalar rclone na execução nativa, habilitar o modo na configuração local, autorizar cada usuário na Microsoft e verificar a sincronização real. Nenhum `.env`, token ou cadastro pessoal foi alterado/excluído; nenhuma credencial foi solicitada no chat. Tokens e configurações continuam fora do Git/backup portátil. Não houve publicação no GitHub.
