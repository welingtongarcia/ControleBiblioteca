# ADR 0002 — Administrador inicial por variáveis de ambiente

**Status:** Aceita  
**Data:** 2026-10-08

## Contexto

O MVP é privado e não oferece autocadastro nem recuperação de senha. Ainda assim, uma instalação vazia precisa ter um primeiro usuário sem versionar credenciais.

## Decisão

No primeiro início com banco vazio, a aplicação lê `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD`, valida senha com no mínimo 12 caracteres e cria um único usuário `ADMIN` com senha bcrypt. Depois que qualquer usuário existe, as variáveis são ignoradas.

Quando ambas as variáveis estão ausentes em banco vazio, a aplicação inicia sem usuário e registra aviso operacional; quando somente uma está ausente ou a senha é inválida, a inicialização falha para evitar configuração incompleta. Os valores não são registrados em logs.

## Consequências

O operador deve armazenar os valores em gerenciador de segredos ou `.env` não versionado antes da primeira inicialização. A troca de senha e gestão de contas serão fluxos administrativos futuros; não são substituídos por repetir o bootstrap.
