# Biblioteca digital — configurar o OneDrive

## Alternativa sem registro próprio

Para conectar sem criar conta Azure e registrar aplicativo próprio, utilize o [modo rclone](onedrive-rclone.md). As instruções OAuth direto abaixo continuam disponíveis para quem já tem registro Microsoft. O rclone requer instalação e autorização inicial por conta no computador onde o sistema roda.

A área **Minha coleção → Biblioteca digital** lista arquivos do seu OneDrive padrão. O recurso é opcional e permanece desativado até configurar a integração. Não é necessário divulgar suas pastas nem enviar sua senha ao sistema.

## O que esta versão faz

- Conecta uma conta Microsoft por usuário da Minha Biblioteca, independentemente do login local.
- Permite navegar no OneDrive padrão e selecionar até 20 pastas.
- Sincroniza manualmente as pastas e subpastas, listando PDF, CBR, CBZ, EPUB, MOBI, AZW e AZW3.
- Pesquisa nome/pasta, filtra por formato e pagina a listagem.
- Abre o link no OneDrive e permite vincular o arquivo a uma edição sua.

O sistema só consulta **metadados**; não baixa conteúdo, extrai capas, lê páginas, modifica, move ou exclui arquivos remotos. O formato é identificado pela extensão e não garante que o arquivo seja um quadrinho. Não há leitor embutido, Google Drive, reconhecimento de ISBN ou criação automática de edições/gastos. Arquivos com proteção DRM continuam sujeitos à proteção original. Abrir no OneDrive não garante pré-visualização de todos os formatos.

São atendidas contas pessoais e corporativas compatíveis com a configuração do aplicativo registrado. A política da organização pode exigir aprovação administrativa. Atalhos para itens compartilhados, outros drives e bibliotecas SharePoint não são percorridos nesta versão.

## 1. Registrar um aplicativo na Microsoft

No [Microsoft Entra](https://entra.microsoft.com/), acesse **App registrations / Registros de aplicativo → New registration**. Se sua conta não puder registrar aplicativos, será necessário um administrador com acesso ao registro; não é possível substituir essa etapa por usuário/senha.

1. Nome: `Minha Biblioteca`.
2. Para atender contas pessoais e corporativas, escolha o tipo de conta que admite diretórios organizacionais e contas Microsoft pessoais. O projeto usa o endpoint `common`.
3. Adicione uma plataforma **Web**, não SPA, com o endereço de retorno exato da sua instalação. Para execução local padrão:

   ```text
   http://localhost:8080/biblioteca-digital/onedrive/retorno
   ```

4. Em produção, registre o endereço público **HTTPS**, incluindo um eventual prefixo de contexto. Não dependa de um endereço gerado a partir do cabeçalho Host.
5. Copie o **Application (client) ID**.
6. Em **Certificates & secrets**, crie um segredo de cliente e guarde o **valor**, não apenas o identificador. Anote sua validade para renová-lo antes de expirar. Não envie o segredo pelo chat, nem o publique no GitHub.
7. Em permissões da API, use Microsoft Graph com permissões **delegadas** `Files.Read` e `offline_access`. Não são necessárias permissões de escrita nem de aplicativo/app-only. O código solicita somente esses dois escopos; não exige `User.Read` ou login OIDC.

O fluxo utiliza authorization code com PKCE, state de uso único vinculado à sessão/usuário e prazo de dez minutos. O provedor realiza login e consentimento. Registre somente endereços controlados por você.

Referências oficiais: [fluxo authorization code](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow) e [permissões para listar pastas](https://learn.microsoft.com/en-us/graph/api/driveitem-list-children?view=graph-rest-1.0).

## 2. Configurar a instalação

Acrescente os valores ao seu `.env` **local**:

```properties
ONEDRIVE_ENABLED=true
ONEDRIVE_CLIENT_ID=seu-id-de-aplicativo
ONEDRIVE_CLIENT_SECRET=valor-do-segredo
ONEDRIVE_REDIRECT_URI=http://localhost:8080/biblioteca-digital/onedrive/retorno
ONEDRIVE_TOKEN_KEY=sua-chave-base64-de-32-bytes
```

Esses textos são marcadores; não são credenciais válidas. Se o segredo contiver caracteres especiais, respeite a sintaxe do `.env` usada pelo Spring/Docker. Não coloque senhas ou tokens no código ou em URLs.

Gere uma chave própria de 32 bytes, por exemplo em terminal com OpenSSL instalado:

```bash
openssl rand -base64 32
```

Guarde a chave com segurança: ela protege tokens em AES-256-GCM e não deve ser compartilhada entre instalações independentes. Não a troque ou remova sem planejar a reconexão das contas. Quem tem acesso simultâneo ao banco e à chave pode acessar os tokens; mantenha ambos protegidos.

Reinicie a aplicação no IntelliJ/terminal, ou reconstrua o serviço Docker com a configuração de execução apropriada. O Compose já repassa essas variáveis. Para o Docker local:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build app
```

O Flyway aplica a V25 automaticamente, sem modificar os cadastros existentes. Quando `ONEDRIVE_ENABLED=false`, não há chamadas Microsoft e a tela exibe orientação de configuração. Se habilitar com valores incompletos, a aplicação recusa a configuração com mensagem sem expor segredos.

## 3. Conectar e sincronizar

1. Entre na sua conta da Minha Biblioteca e abra **Biblioteca digital → Conectar OneDrive**.
2. Na página da Microsoft, escolha a conta correta e confira as permissões antes de autorizar.
3. De volta ao aplicativo, abra **Escolher pastas**. Navegue até a pasta de leitura e clique em **Selecionar esta pasta**. É possível selecionar a raiz, mas pastas específicas evitam indexar documentos pessoais desnecessariamente.
4. Clique em **Sincronizar agora** e aguarde. A sincronização é manual, não um monitoramento contínuo.
5. Use busca/formato para localizar arquivos. **Abrir no OneDrive** utiliza a conta e os recursos de visualização do provedor.
6. Em **Vincular edição**, pesquise uma edição sua e confirme o vínculo. Isso não muda preço, data, suporte ou estado de leitura. Cadastre a edição no catálogo se ela ainda não existir.

**Importante:** `Files.Read` autoriza leitura dos arquivos da conta, não somente de uma pasta. A seleção de pastas é uma restrição aplicada pelo sistema, não uma limitação do consentimento Microsoft.

## Limites e comportamento de atualização

- Por execução: 10.000 itens visitados (incluindo arquivos não suportados), 1.000 pastas, 200 consultas e orçamento de 90 segundos. Uma requisição em andamento pode levar até mais 15 segundos para terminar. Cada resposta tem limite de 1 MB; respostas maiores são recusadas.
- Há somente uma sincronização ativa por usuário; alterações na seleção aguardam sua conclusão. Uma execução interrompida libera o bloqueio após cinco minutos.
- Subpastas e arquivos repetidos em seleções sobrepostas são deduplicados por identificador remoto.
- O índice só é publicado após a leitura completa. Falha, indisponibilidade, limite ou permissão negada mantêm a listagem anterior. Uma execução incompleta nunca marca tudo como ausente.
- Após sincronização completa, arquivos antes indexados que não foram encontrados recebem a indicação **Não encontrado na última sincronização**, preservando o vínculo com a edição. Eles podem ter sido removidos, movidos para fora da seleção ou excluídos da seleção de pastas; a mensagem não afirma exclusão definitiva.
- Arquivos encontrados novamente ficam disponíveis sem criar duplicata. Renomear atualiza o índice pelo ID; não é identificação por hash/conteúdo.
- Ao remover todas as pastas, ainda é possível sincronizar: os resultados anteriores ficam como não encontrados na seleção, preservando edições e vínculos. Nenhum arquivo remoto é excluído.
- Não há abertura de links de download temporários nem compartilhamento público dos arquivos. A paginação só aceita o mesmo endpoint HTTPS Microsoft Graph, evitando enviar tokens a endereços arbitrários.

## Desconectar, reconectar e backup

**Desconectar** apaga tokens, seleção e índice **locais**, mas não modifica o OneDrive nem exclui edições do catálogo. Para revogar o consentimento também no provedor, utilize as configurações de aplicativos autorizados da conta Microsoft/organização.

**Reconectar** cria uma conexão nova e limpa a seleção e o índice anteriores, inclusive vínculos de arquivos. Isso evita misturar arquivos de contas Microsoft diferentes. Selecione as pastas e sincronize novamente. A renovação automática de tokens, enquanto válida, mantém a seleção e os vínculos.

O backup portátil **não transporta tokens, seleção de pastas, índice de nuvem ou vínculos desses arquivos**. Depois de restaurar uma coleção em outra instalação, configure a integração, conecte a conta, escolha pastas e refaça os vínculos. Os dados normais do catálogo continuam no backup. Isso evita transportar permissões de nuvem entre usuários/máquinas. Não copie a chave ou o banco para resolver problemas de autorização sem avaliar a segurança.

## Problemas comuns

| Situação | Ação |
| --- | --- |
| Tela informa que falta configurar | Confira variáveis e reinicie; no Docker, recrie o serviço app para atualizar seu ambiente |
| Retorno inválido / redirect URI mismatch | Use plataforma Web e o endereço de retorno exato, com protocolo, porta e caminho corretos |
| Autorização expirada ou usuário errado | Conecte novamente a partir da sessão local atual, sem reaproveitar uma URL de retorno |
| Consentimento negado ou bloqueio empresarial | Confira permissões delegadas e solicite aprovação ao administrador quando exigida |
| Chave alterada ou segredo expirado | Corrija a configuração; segredo do cliente e chave de criptografia têm funções diferentes; reconecte conforme necessário |
| Pasta muito grande | Selecione subpastas menores; a listagem anterior é preservada |
| OneDrive limita consultas | Aguarde alguns minutos; o sistema não faz tentativas ilimitadas nem ignora `429` |
| Mudança não aparece | Clique em Sincronizar; o recurso não faz atualização em tempo real |
| EPUB/CBR não abre no navegador | Listagem e vínculo não incluem um leitor interno; use um leitor compatível autorizado |

Não habilite logs HTTP detalhados com headers/corpos OAuth: podem conter credenciais. Use HTTPS em produção, proteja `.env`, chave e banco e configure o proxy encaminhado apenas em infraestrutura confiável.
