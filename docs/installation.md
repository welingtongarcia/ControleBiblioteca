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
