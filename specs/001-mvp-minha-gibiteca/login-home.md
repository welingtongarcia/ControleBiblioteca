# Destino após login — 2026-10-10

## Constitution → specify → clarify

Constituição mantida; ajuste restrito ao destino de autenticação bem-sucedida.

- LOGIN-001: todo login bem-sucedido redireciona para `/dashboard`, a página Início visual. Uma requisição privada salva antes do login não substitui esse destino; Visão geral continua em `/visao-geral`.
- LOGIN-002: manter autenticação persistida, sessão, CSRF, autorização por proprietário, erro de credenciais em `/login?error` e logout. Não modificar navegação de usuários já autenticados nem formulários de domínio.

## Plan → tasks → analyze

- [ ] LH-T1 Configurar destino obrigatório no formLogin do Spring Security.
- [ ] LH-T2 Testar login direto, requisição salva a catálogo/financeiro/Visão geral, acesso autenticado ao Início, remoção da requisição salva e falhas/CSRF; executar Maven.

A configuração atual não define success URL e usa a requisição salva ou `/`. Usar `defaultSuccessUrl("/dashboard", true)` com o handler padrão, que também remove a requisição salva. Não criar controller/handler customizado, alterações de banco ou dependências.

Aceite: login direto e login após acesso anônimo a outras telas chegam ao Início; credenciais inválidas e POST sem CSRF não autenticam. Verificação com MockMvc, filtros reais do Spring Security e PostgreSQL Testcontainers, sem alterar usuários/dados reais.
