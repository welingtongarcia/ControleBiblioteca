# Instalação e execução local

## Pré-requisitos

Java 21 e Docker Compose v2. O Maven Wrapper está versionado, portanto Maven instalado é opcional após a primeira execução.

## Execução com Docker

1. Copie `.env.example` para `.env`.
2. Substitua ambos os valores de senha por uma senha local forte e mantenha `POSTGRES_PASSWORD` e `DB_PASSWORD` iguais.
3. Execute `docker compose up --build`.
4. Consulte `http://localhost:8080/actuator/health/readiness` para verificar disponibilidade.

O login visual já existe, mas credenciais de usuário ainda não são criadas nesta fase. O bootstrap de usuário será entregue na Fase 2.

## Verificação

Execute `./mvnw clean verify`. O conjunto sobe PostgreSQL via Testcontainers, aplica Flyway em banco vazio e verifica a página de login com MockMvc.
