# Regras permanentes — Minha Gibiteca

## Método de trabalho

- Seguir Spec-Driven Development: `constitution → specify → clarify → plan → tasks → analyze → implement → verificação`.
- Antes de alterar comportamento, atualizar a especificação aplicável em `specs/` e manter requisitos e tarefas rastreáveis.
- Preservar alterações existentes e não usar comandos destrutivos de Git. Cada fase concluída deve ter um commit coerente, depois de validação real.
- Não declarar testes, deploys ou integrações como concluídos sem evidência executada.

## Limites técnicos

- Java 21, Maven, Spring Boot MVC, Thymeleaf, CSS próprio e PostgreSQL. É um monólito com renderização no servidor.
- Não introduzir frontend separado, SPA, React, TypeScript, Node como requisito de build, ou entidades JPA diretamente em controllers/forms.
- Organizar o código por módulos de domínio e separar presentation, application, domain e infrastructure.
- Usar Flyway para todo esquema; `ddl-auto=update` é proibido fora de experimentos locais explicitamente isolados.
- Injeção por construtor, Bean Validation, PRG para formulários e autorização por proprietário em toda leitura/mutação de dados privados.

## Dados, segurança e UX

- Distinguir Obra, Série, Edição e Exemplar físico. ISBN é opcional e não universalmente único; valores monetários usam decimal.
- Arquivos de capa ficam em armazenamento de arquivos, nunca em BLOB nas tabelas centrais. Validar tipo, conteúdo, tamanho e autorização de upload.
- Manter CSRF, cookies seguros em produção, proteção contra XSS/SQL injection e logs sem segredos.
- Interface em português brasileiro, responsiva e compatível com WCAG 2.2 AA nos fluxos entregues.

## Qualidade

- Cobrir regras e serviços com JUnit/Mockito; controllers MVC com MockMvc; persistência com PostgreSQL em Testcontainers.
- Evitar N+1, paginar listas e criar índices conforme consultas especificadas.
- Ao fim de cada fase, registrar arquivos, resultados dos comandos, requisitos atendidos, pendências e riscos.
