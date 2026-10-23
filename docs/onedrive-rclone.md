# OneDrive com rclone — sem aplicativo Azure próprio

O rclone é gratuito e usa, por padrão, seu próprio registro de aplicativo Microsoft. Não é necessário contratar Azure nem preencher `ONEDRIVE_CLIENT_ID`, `ONEDRIVE_CLIENT_SECRET` ou `ONEDRIVE_TOKEN_KEY` para este modo. Continuam valendo o espaço contratado no OneDrive, internet e limites Microsoft; o cliente compartilhado pode sofrer limitação de requisições.

O login Microsoft é feito **uma vez pelo rclone no computador do servidor**, não pelo formulário de login do Minha Biblioteca. Depois, na biblioteca digital, clique em verificar/conectar, escolha as pastas e sincronize. Esta entrega não inicia o assistente interativo pelo navegador do sistema.

## 1. Instalar e habilitar

Para IntelliJ/JAR, instale o rclone pelas [instruções oficiais](https://rclone.org/install/), disponíveis para Linux, Windows e macOS. Confira `rclone version`. No Windows pode ser necessário informar o caminho absoluto de `rclone.exe` em `RCLONE_EXECUTABLE`; o valor é um executável, não um comando com argumentos.

No `.env` local, configure sem publicar credenciais:

```dotenv
RCLONE_ENABLED=true
RCLONE_EXECUTABLE=rclone
RCLONE_CONFIG_DIRECTORY=./data/rclone
ONEDRIVE_ENABLED=false
```

Reinicie pelo IntelliJ e acesse **Minha coleção → Biblioteca digital**. Copie o identificador UUID exibido para **sua conta**. Não use o identificador de outro usuário. Cada pessoa terá um arquivo próprio; não se usa a configuração global do rclone.

## 2. Autorizar a conta Microsoft

Execute no diretório do projeto, substituindo `SEU-UUID` pelo identificador exibido. Use a mesma conta do sistema operacional que executa a aplicação.

Linux/macOS:

```bash
mkdir -p data/rclone
chmod 700 data/rclone
umask 077
rclone --config "data/rclone/SEU-UUID.conf" --onedrive-access-scopes "Files.Read offline_access" --onedrive-disable-site-permission config
```

Windows (PowerShell):

```powershell
New-Item -ItemType Directory -Force data/rclone
rclone --config "data/rclone/SEU-UUID.conf" --onedrive-access-scopes "Files.Read offline_access" --onedrive-disable-site-permission config
```

No assistente:

1. Escolha `n` (nova conexão). Se já existe o remote desta conta, edite-o em vez de criar outro com nome diferente.
2. Nome: **onedrive**, exatamente assim.
3. Tipo: **Microsoft OneDrive** (`onedrive`). Os números do menu variam por versão.
4. `client_id` e `client_secret`: deixe ambos vazios para usar o aplicativo compartilhado do rclone.
5. Se solicitar configuração avançada, os escopos devem ser `Files.Read offline_access`; não habilite permissões de escrita ou busca SharePoint.
6. Autorize no navegador Microsoft. Confira a conta e o consentimento de leitura. Se o assistente pedir escrita, cancele e revise os escopos antes de continuar.
7. Escolha OneDrive pessoal/corporativo e confirme a unidade correta. Não escolha SharePoint nesta integração.
8. Salve a configuração. Não use criptografia interativa por senha do rclone nesta versão: o processo web não solicita essa senha. Proteja o arquivo com permissões do sistema operacional.

Em Linux/macOS, após a criação:

```bash
chmod 600 "data/rclone/SEU-UUID.conf"
```

No Windows, restrinja as permissões do diretório e arquivo ao usuário que roda o aplicativo, pelas propriedades de segurança do sistema. Os tokens ficam nesse arquivo; não envie o conteúdo pelo chat e não publique nem copie para outro usuário.

## 3. Usar no sistema

Na biblioteca digital, clique **Verificar e conectar via rclone**. A aplicação verifica o remote e a raiz antes de gravar a conexão. Em seguida:

1. **Escolher pastas**; abra ou selecione a pasta desejada.
2. **Sincronizar agora** para listar arquivos de leitura nas pastas e subpastas.
3. Pesquise, filtre por formato e vincule a uma edição existente.

Não há cópia de arquivos nem criação automática de compras, edições, avaliações ou progresso. A identificação é pela extensão PDF/CBR/CBZ/EPUB/MOBI/AZW/AZW3. O sistema não usa `rclone link`: não cria links públicos e não oferece o botão de abertura Web para arquivos rclone. Abra os arquivos no próprio OneDrive.

Reconectar ou trocar entre OAuth direto e rclone **limpa seleção, índice e vínculos locais**. Edições e arquivos remotos não são apagados. Desconectar na interface remove somente a conexão no banco: a autorização rclone continua no computador. Para encerrá-la, revogue o consentimento nas configurações da conta Microsoft e peça ao administrador para remover o arquivo correspondente, sem apagar arquivos do OneDrive.

## Docker

A imagem inclui rclone. Ative `RCLONE_ENABLED=true` no `.env` e recrie a aplicação pelo Compose local conforme o README. O volume privado `rclone-data` armazena as configurações; não coloque tokens no Dockerfile ou na imagem.

Com a aplicação iniciada, execute no terminal do servidor, como o mesmo usuário da aplicação (padrão do container):

```bash
docker compose exec app rclone --config /app/data/rclone/SEU-UUID.conf --onedrive-access-scopes "Files.Read offline_access" --onedrive-disable-site-permission config
```

O container normalmente não tem navegador. Escolha **não** quando perguntar sobre abrir o navegador local e siga o procedimento de autorização remota mostrado pelo rclone. Faça o login em computador confiável com a mesma versão do rclone; a resposta de autorização contém tokens e deve ser colada somente no terminal do assistente, nunca no chat. Consulte o [guia de configuração remota](https://rclone.org/remote_setup/). Não exponha um servidor de controle rclone na rede.

O volume não é exportado no backup portátil da conta. Ao migrar de máquina, autorize novamente. Nunca use `docker compose down -v` para resolver erros: isso também pode remover o banco e as imagens.

## Segurança e limites

- Execução sem shell e somente comandos de metadados, com `--dry-run` como defesa adicional e escopos de leitura/renovação. Não se usa `--read-only`, pois não é uma flag global desses comandos. Não há upload, exclusão, movimentação, download de conteúdo ou criação de links. A renovação do token pode escrever **na configuração local**, que precisa ser gravável pelo usuário da aplicação.
- O programa não herda variáveis `RCLONE_*`; o caminho do executável/diretório é configuração confiável do servidor, nunca um campo do navegador. Não usar remotes de outro tipo, atalhos para configurações de outras contas ou configuração global compartilhada.
- Consulta individual: até 20 s e 16 MiB de saída. Sincronização: até 10.000 itens, 1.000 pastas, 200 consultas e orçamento de 90 s, podendo exceder em uma consulta de até 20 s. Erros preservam o índice anterior. Navegação exibe até 100 itens por página; arquivos indexados até 20 por página. A próxima página de pastas consulta novamente a listagem; alterações simultâneas no OneDrive podem mudar a posição dos itens.
- Caminho de pasta rclone: até 254 caracteres; nomes com `:`, barras invertidas ou caracteres de controle não são suportados. Pastas movidas precisam ser selecionadas novamente. IDs de arquivos são derivados do ID estável informado pelo backend, com caminho como alternativa quando ele estiver ausente.
- A seleção de pastas é uma restrição do Minha Biblioteca, não uma limitação do consentimento Microsoft a uma única pasta. Tokens existentes com permissões maiores precisam ser revogados e autorizados novamente com os escopos indicados.
- O arquivo `.conf` é sensível. `data/` fica fora do Git e do contexto de build; não mova credenciais para arquivos versionados. O administrador do servidor é responsável por associar o arquivo à pessoa certa e proteger backups privados da instalação.

Referências: [OneDrive no rclone](https://rclone.org/onedrive/), [lsjson](https://rclone.org/commands/rclone_lsjson/) e [licença](https://rclone.org/licence/).
