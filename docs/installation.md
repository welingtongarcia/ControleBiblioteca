# Instalação e execução local

## Pré-requisitos

Java 21 e Docker Compose v2. O Maven Wrapper está versionado, portanto Maven instalado é opcional após a primeira execução.

## Execução com Docker

1. Copie `.env.example` para `.env`.
2. Substitua ambos os valores de senha por uma senha local forte e mantenha `POSTGRES_PASSWORD` e `DB_PASSWORD` iguais. Defina também o e-mail e uma senha de ao menos 12 caracteres para o administrador inicial.
3. Execute `docker compose up --build`.
4. Consulte `http://localhost:8080/actuator/health/readiness` para verificar disponibilidade.

No primeiro início em um banco vazio, o administrador é criado somente pelas variáveis `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD`. Após existir algum usuário, essas variáveis são ignoradas. Nunca versione o `.env`.

## Verificação

Execute `./mvnw clean verify`. O conjunto sobe PostgreSQL via Testcontainers, aplica Flyway em banco vazio e verifica a página de login com MockMvc.

## Execução pelo IntelliJ IDEA

O projeto carrega automaticamente o arquivo local `.env` ao executar `MinhaGibitecaApplication`. Para usar o banco local pelo IntelliJ, suba apenas `postgres` com Docker Compose; o serviço expõe a porta somente em `127.0.0.1:5432`. Pare o serviço `app` do Compose para liberar a porta 8080 e então execute a classe principal pelo IntelliJ.

## Consulta por ISBN

No cadastro de edição, informe ISBN-10/13 e use Buscar pelo ISBN. A Open Library recebe somente esse identificador; nenhum título privado, valor ou estado de leitura é enviado. A consulta preenche campos vazios de título/ano e associa editora/criadores já cadastrados pelo proprietário apenas quando o nome corresponde de forma única. Sugestões continuam revisáveis; é necessário Salvar para persistir. Capa aparece somente como indicação/link da fonte, sem importação automática; use o upload após salvar.

Sem resultado, falha de rede ou ISBN inválido, continue o cadastro manual. O ISBN manual continua opcional e não passa a exigir checksum para salvar. Não há chave de API obrigatória. O serviço usa HTTPS, timeouts de conexão/leitura de 3/5 segundos, limite de resposta de 256 KiB, cache público de dez minutos/200 ISBNs e no máximo uma nova consulta externa por segundo por processo; não use para importações em massa. Endpoint Books API é legado, podendo deixar de ser oferecido: o fallback manual não depende dele.

Para uso frequente, configure `ISBN_USER_AGENT=MinhaGibiteca/0.1 (seu-contato)` no ambiente ou `.env`, usando um contato que você autorize enviar ao provedor. Nenhum contato é inferido da conta do usuário. As [orientações oficiais da Open Library](https://openlibrary.org/developers/api) recomendam identificação com nome da aplicação/contato e cache. A [Books API](https://openlibrary.org/dev/docs/api/books) documenta os metadados usados nesta integração. Sem rede disponível, os demais cadastros continuam funcionando.
