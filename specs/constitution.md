# Constituição do projeto Minha Gibiteca

**Versão:** 1.0.0  
**Ratificada em:** 2026-10-08

## Princípios

1. **Especificação antes de código.** Toda alteração funcional começa com requisito, critérios de aceitação e impacto documentados. Código sem cobertura de especificação não entra no projeto.
2. **Integridade do acervo acima de conveniência.** Obra, Série, Edição e Exemplar são conceitos distintos. Dados do acervo não são apagados nem inferidos silenciosamente; exclusões devem ser deliberadas, auditáveis e compatíveis com dependências.
3. **Privacidade por proprietário.** Todo dado particular possui dono lógico. Autenticação e autorização são verificadas no servidor para cada leitura e escrita; a interface nunca é a única barreira.
4. **Monólito simples e sustentável.** Uma aplicação Java 21/Spring Boot MVC, templates Thymeleaf e PostgreSQL. Módulos internos claros precedem qualquer divisão em serviços ou frontend separado.
5. **Banco evolutivo e explícito.** Flyway controla integralmente o esquema. Chaves, constraints, índices e regras de nulidade refletem a especificação; `ddl-auto=update` é vedado em produção.
6. **Segurança desde o primeiro fluxo.** Sessão, CSRF, validação, codificação de saída, proteção de upload, segredo externo e logs seguros são requisitos de entrega, não acabamento.
7. **Acessibilidade e honestidade da interface.** HTML semântico, navegação por teclado, contraste, mensagens de erro compreensíveis e responsividade são critérios de aceite. Não há controles decorativos sem comportamento real.
8. **Testes proporcionais ao risco.** Regras de domínio, serviços, persistência PostgreSQL real e fluxos MVC essenciais recebem testes automatizados. Resultados são reportados exatamente como executados.
9. **Desempenho desde o modelo.** Meta de 10.000 exemplares por usuário exige paginação, índices, consultas sem N+1 e limites para uploads desde o MVP.
10. **Decisões rastreáveis e reversíveis.** Decisões de impacto viram ADR; premissas e pendências ficam explícitas. Preferir mudanças pequenas, verificáveis e reversíveis.

## Governança

Esta constituição prevalece sobre preferências locais de implementação. Alterações exigem justificativa, atualização da versão/data e análise de impacto nos artefatos SDD. Cada fase verifica conformidade com estes princípios.
