# Destino após login — 2026-10-10

## Constitution → specify → clarify

Constituição mantida; ajuste restrito ao destino de autenticação bem-sucedida.

- LOGIN-001: todo login bem-sucedido redireciona para `/dashboard`, a página Início visual. Uma requisição privada salva antes do login não substitui esse destino; Visão geral continua em `/visao-geral`.
- LOGIN-002: manter autenticação persistida, sessão, CSRF, autorização por proprietário, erro de credenciais em `/login?error` e logout. Não modificar navegação de usuários já autenticados nem formulários de domínio.

## Plan → tasks → analyze

- [x] LH-T1 Configurar destino obrigatório no formLogin do Spring Security.
- [x] LH-T2 Testar login direto, requisição salva a catálogo/financeiro/Visão geral, acesso autenticado ao Início, remoção da requisição salva e falhas/CSRF; executar Maven.

A configuração atual não define success URL e usa a requisição salva ou `/`. Usar `defaultSuccessUrl("/dashboard", true)` com o handler padrão, que também remove a requisição salva. Não criar controller/handler customizado, alterações de banco ou dependências.

Aceite: login direto e login após acesso anônimo a outras telas chegam ao Início; credenciais inválidas e POST sem CSRF não autenticam. Verificação com MockMvc, filtros reais do Spring Security e PostgreSQL Testcontainers, sem alterar usuários/dados reais.

## Implement → verificação

- `SecurityConfiguration.java`: destino fixo `/dashboard` após autenticação (LOGIN-001), mantendo filtros e configuração restante (LOGIN-002).
- `MinhaGibitecaApplicationTest.java`: login direto com usuário persistido e e-mail normalizado; nova sessão para cada requisição salva ao catálogo com filtro, financeiro e Visão geral; redirect para Início, remoção da requisição salva, renderização autenticada do Início e acesso posterior às telas originalmente solicitadas. Credenciais inválidas redirecionam para `/login?error`; ausência de CSRF retorna 403.
- `./mvnw verify -q`: saída 0; 161 testes, 0 falhas, 0 erros, 0 ignorados, com PostgreSQL Testcontainers. `git diff --check`: sem erros. Planejamento validado/registrado no commit `2d6be7c` antes da implementação.
- Sem mudanças de interface/esquema, sem deploy ou reinicialização do processo do IntelliJ. Reiniciar a aplicação para carregar a configuração Java modificada. Diretório `data/` e dados reais preservados. Nenhuma pendência funcional identificada; validação do fluxo HTTP executada via MockMvc, sem login manual no navegador.
