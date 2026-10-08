# Minha Gibiteca

Sistema web privado para gerenciar coleção de histórias em quadrinhos, construído como monólito Java 21/Spring Boot MVC/Thymeleaf/PostgreSQL.

## Estado atual

A Fase 1 está em execução: a fundação Spring Boot MVC/Thymeleaf, Flyway, PostgreSQL, Docker Compose, health checks, login visual e testes de fumaça já foram criados. A autenticação com usuários persistidos e os módulos de negócio permanecem nas próximas fases. Os artefatos de requisitos, arquitetura e decomposição estão em [specs/001-mvp-minha-gibiteca](specs/001-mvp-minha-gibiteca) e em [docs](docs).

## Ambiente verificado em 2026-10-08

| Componente | Resultado |
| --- | --- |
| Java | OpenJDK 21.0.12.1 |
| Maven | 3.9.12 |
| Docker Engine | 29.9.0 (daemon acessível) |
| Docker Compose | v5.6.0 |
| GitHub Spec Kit | Não instalado/detectável; artefatos equivalentes adotados |

## Executar

Consulte o [guia de instalação](docs/installation.md). A verificação completa é feita com `./mvnw clean verify`.
