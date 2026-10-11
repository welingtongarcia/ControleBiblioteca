# Minha Biblioteca

Sistema web para organizar coleções particulares de **livros e quadrinhos**, desenvolvido em Java 21, Spring Boot MVC, Thymeleaf e PostgreSQL. Cada usuário tem seu próprio acervo.

## Funcionalidades

- Cadastro de usuários e login; dados separados por proprietário.
- Livros e quadrinhos físicos ou digitais, ISBN opcional, capas e ordenação por título e número.
- Editoras, obras, séries, personagens e criadores com fotos e múltiplas funções, incluindo Escritor.
- Exemplares físicos, conservação e localizações.
- Lista de desejos com prioridade, preço estimado e situação de aquisição.
- Leitura por edição e por páginas, progresso das séries e notas de 0 a 5 estrelas.
- Financeiro com filtros, gráficos e comparações por período; relatórios em PDF e CSV.
- Backup portátil dos dados e imagens da conta.
- Consulta opcional de metadados por ISBN/título; cadastro manual funciona sem integrações.

## Requisitos

| Modo | Requisitos |
| --- | --- |
| Docker local | Git, Docker e Compose 2.24.4 ou superior; não precisa instalar Java no computador |
| IntelliJ ou terminal | Git, JDK 21 e Docker/Compose para o banco |
| Servidor | Docker/Compose ou JDK 21 + PostgreSQL 16, HTTPS e configuração de segurança |

O Maven Wrapper (`mvnw` / `mvnw.cmd`) está incluído. A primeira execução precisa de internet para baixar dependências ou imagens. Execute os comandos **dentro da pasta clonada**, onde estão `.env.example`, `pom.xml` e `docker-compose.yml`.

## 1. Baixar e configurar

```bash
git clone https://github.com/welingtongarcia/ControleBiblioteca.git
cd ControleBiblioteca
```

Crie a configuração local, sem sobrescrever um `.env` que já utiliza.

Linux/macOS:

```bash
cp .env.example .env
```

Windows — PowerShell:

```powershell
Copy-Item .env.example .env
```

Abra `.env` em seu editor e configure:

| Variável | O que informar |
| --- | --- |
| `POSTGRES_DB` | Nome do banco; pode manter `minha_gibiteca` |
| `POSTGRES_USER` | Usuário do banco; pode manter `minha_gibiteca` |
| `POSTGRES_PASSWORD` | Senha forte para uma instalação nova |
| `DB_PASSWORD` | A **mesma senha** de `POSTGRES_PASSWORD` |
| `BOOTSTRAP_ADMIN_EMAIL` | Seu e-mail para o primeiro administrador |
| `BOOTSTRAP_ADMIN_PASSWORD` | Senha própria de pelo menos 12 caracteres, diferente da senha do banco |
| `GOOGLE_BOOKS_API_KEY` | Opcional; deixe vazio para iniciar sem Google Books |

Não use as senhas de exemplo. Nunca publique `.env`, credenciais, backups ou imagens pessoais. Alterar as variáveis do PostgreSQL **não troca a senha de um banco já criado**; para instalação existente, mantenha suas credenciais reais.

## 2. Executar com Docker no computador

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

O complemento **local** limita a aplicação a `127.0.0.1:8080` e permite a sessão no HTTP local. Não o utilize em servidor público. Ele usa `!override`, disponível no [Docker Compose 2.24.4 ou superior](https://docs.docker.com/reference/compose-file/merge/#replace-value).

Aguarde o banco e a aplicação iniciarem. Na primeira execução, o build pode levar alguns minutos.

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml ps
docker compose -f docker-compose.yml -f docker-compose.local.yml logs --tail=100 app
```

Abra [http://localhost:8080](http://localhost:8080). Consulte a disponibilidade em [http://localhost:8080/actuator/health/readiness](http://localhost:8080/actuator/health/readiness); o resultado esperado é `UP`.

Para parar sem remover os volumes:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

O banco e as imagens ficam nos volumes `postgres-data` e `covers-data`. **Não acrescente `-v` ao comando `down`**: essa opção remove os volumes e pode apagar o acervo. Recriar containers não substitui uma estratégia de backup.

## 3. Alternativa: IntelliJ IDEA

1. Instale JDK 21 e configure **Project SDK** e o JDK do Maven como 21.
2. Abra a pasta clonada ou importe `pom.xml` como projeto Maven.
3. Prepare `.env` como descrito acima.
4. No terminal do projeto, inicie somente o banco:

   ```bash
   docker compose up -d postgres
   ```

5. Se a aplicação do Docker estiver rodando, pare-a para liberar a porta:

   ```bash
   docker compose stop app
   ```

6. Execute `br.com.minhagibiteca.MinhaGibitecaApplication`, com a pasta do projeto como diretório de trabalho. Para uso local, não ative o perfil `prod`.
7. Abra [http://localhost:8080](http://localhost:8080).

O `.env` é carregado automaticamente. Fora do container, o endereço padrão do banco é `localhost:5432`. Se alterou `POSTGRES_DB` ou `POSTGRES_USER`, acrescente `DB_NAME` e `DB_USERNAME` com os mesmos valores ao `.env`. Se alterou a porta publicada por `POSTGRES_PORT`, informe também `DB_PORT` para execução fora do Docker.

Os nomes técnicos `minhagibiteca` e `MinhaGibitecaApplication` foram mantidos para compatibilidade; a interface se chama Minha Biblioteca.

## 4. Alternativa: terminal ou JAR

Com o banco iniciado e JDK 21 instalado, execute:

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows — PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Se o wrapper não tiver permissão no Linux/macOS, use `chmod +x mvnw`.

Para gerar um JAR e executá-lo, permanecendo na pasta que contém `.env`:

```bash
./mvnw -DskipTests package
java -jar target/minha-gibiteca-0.1.0-SNAPSHOT.jar
```

No Windows, troque `./mvnw` por `.\mvnw.cmd`. Esse empacotamento pula testes; para validar o projeto, use a seção de testes.

## Primeiro acesso

O administrador é criado por `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` **somente quando ainda não há usuários no banco**. Entre com esses dados; não há senha universal distribuída com o projeto.

Outras pessoas podem escolher **Criar conta** no login. Contas criadas pelo formulário são usuários comuns e começam com biblioteca vazia. A senha de cadastro aceita de 12 a 64 caracteres, limitada também a 72 bytes UTF-8. Não há confirmação de e-mail ou recuperação de senha implementadas.

Alterar o bootstrap depois de existir um usuário não redefine a senha de nenhuma conta. Não apague o banco para resolver um problema de login.

## Testes e migrações

```bash
./mvnw clean verify
```

No Windows: `.\mvnw.cmd clean verify`. Os testes exigem JDK 21 e Docker acessível para iniciar PostgreSQL isolado pelo Testcontainers. A última verificação da implementação, em 11/10/2026, passou com **239 testes**.

O Flyway aplica migrações na inicialização. Não use `ddl-auto=update` e não edite migrações já aplicadas em um banco existente.

## Atualizar sem perder o acervo

Gere backup antes de atualizar. Se houver modificações próprias no código, revise-as antes de atualizar a branch.

```bash
git pull --ff-only
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

Para IntelliJ/terminal, reinicie após atualizar. Os cadastros **não ficam no GitHub**: ficam no banco e no armazenamento de imagens da instalação.

Em **Painéis e análises → Backup e migração**, gere um ZIP pessoal para transportar cadastros, capas e fotos. Guarde-o em local privado: não é criptografado e não inclui a aplicação nem contas/senhas. A restauração exige conta vazia e versão de esquema compatível. Veja o [guia de backup](docs/backup-migracao.md).

## Problemas comuns

| Sintoma | Verifique |
| --- | --- |
| `.env.example` não encontrado | Execute na pasta `ControleBiblioteca`, não na pasta pessoal do sistema |
| Porta 8080 ocupada | Não execute no Docker e no IntelliJ ao mesmo tempo |
| Porta 5432 ocupada | Verifique se há outro PostgreSQL; ajuste a porta e `DB_PORT` na execução nativa |
| Falha de autenticação no banco | `DB_PASSWORD` deve corresponder à senha real do banco; alterar `.env` não troca a senha de volume antigo |
| Login volta para a tela de entrada | Confira HTTP local/HTTPS, cookies e perfil; use o complemento local somente no computador |
| Testcontainers não inicia | Docker deve estar ativo e acessível ao usuário; não publique o socket nem dê permissões irrestritas |
| ISBN sem resultado | Nem todas as publicações estão nos provedores; o cadastro manual continua disponível |
| Imagem recusada | Use PNG/JPEG válido de até 5 MB; fotos aceitam até 40 milhões de pixels; mantenha o armazenamento gravável |

## Implantação pública

Um repositório público **não significa que o sistema está hospedado na internet**. O `docker-compose.yml` base ativa `prod`, com cookies seguros. Para servidor, use HTTPS com proxy reverso, configure os cabeçalhos encaminhados e o acesso à porta da aplicação, limite acesso ao banco, mantenha segredos fora do Git e tenha backups regulares.

Não use `docker-compose.local.yml` nesse ambiente. Antes de permitir cadastro público, configure proteção contra abuso/rate limiting. O projeto não inclui provisionamento completo de infraestrutura ou proxy HTTPS.

## Documentação

- [Instalação detalhada e integrações opcionais](docs/installation.md)
- [Financeiro e regras de cálculo](docs/financeiro.md)
- [Backup e migração](docs/backup-migracao.md)
- [Especificações e decisões](specs)
- [Contrato HTTP](docs/contracts/openapi.yaml)

## Licença

Ainda não foi definida uma licença geral para o projeto. A visibilidade pública não substitui licença de uso ou redistribuição. A fonte DejaVu distribuída possui sua [licença própria](src/main/resources/fonts/LICENSE-DejaVu.txt).
