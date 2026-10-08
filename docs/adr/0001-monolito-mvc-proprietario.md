# ADR 0001 — Monólito MVC com dados proprietários no MVP

**Status:** Aceita  
**Data:** 2026-10-08

## Contexto

O produto precisa ser privado no início, mas poder evoluir para múltiplos usuários e catálogo compartilhado. A experiência requisitada é server-side com Java 21, Spring Boot MVC, Thymeleaf, CSS e PostgreSQL.

## Decisão

Construir um monólito modular com renderização no servidor. Cada entidade do MVP que contém dados bibliográficos ou particulares é propriedade de um usuário. Controllers e serviços recebem/validam o proprietário autenticado, retornando 404 para recursos alheios. Não haverá SPA, frontend independente ou catálogo bibliográfico global nesta fase.

## Consequências

O isolamento é simples e forte, porém dois usuários podem manter seus próprios metadados para uma mesma publicação. Uma evolução para catálogo global exigirá migração/desduplicação planejada, não uma alteração silenciosa. A modularização interna preserva caminho de evolução sem custo de microsserviços no MVP.

## Implementação da fundação

Foi adotado Spring Boot 3.5.16 e springdoc-openapi 2.9.1. A combinação é compatível com Java 21, Maven 3.9 e Spring Boot 3.5.x, respectivamente. A UI e os endpoints OpenAPI permanecem desabilitados até que exista uma integração REST especificada.
