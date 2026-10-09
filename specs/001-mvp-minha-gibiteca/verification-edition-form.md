# Verificação T031 — formulário de edição

Data: 2026-10-09. Requisitos FR-047, AC-019; regressão AC-009/013/015/016.

## Arquivos e entrega

- spec.md, plan.md, tasks.md, analysis.md: planejamento/clarificação/análise antes da implementação, mantendo constituição.
- templates/edicoes/form.html: formulário compartilhado de novo/editar em fieldsets; ISBN opcional com detalhe da busca; identificação; suporte/valores; vínculos bibliográficos; observações. Orientações sobre capa/exemplares, campos opcionais e semântica de preços.
- static/css/main.css: estilos escopados, grade responsiva, seções e foco; largura ampliada só neste formulário.
- messages_pt_BR.properties: mensagens amigáveis para binding de ano/editora/série/obras/criadores, sem conversão técnica em inglês.
- MinhaGibitecaApplicationTest.java: três testes novos (vazio, seleção/remoção múltipla e rascunho inválido), fixtures sintéticas em target/ui-edition-form.

## Evidências executadas

- ./mvnw clean verify -q: execução final código 0, 115 testes, zero falhas/erros/skips; PostgreSQL 16 real via Testcontainers, MockMvc e Thymeleaf; JAR gerado.
- Cadastro com duas obras/dois criadores, preços decimais e retorno filtrado; GET edição conserva seleções/valores. POST de edição com marcadores de checkbox remove vínculos. Opções de outro proprietário não aparecem.
- POST inválido cobre título, ano, valor negativo, preço não numérico e vínculos inválidos: não salva, mantém rascunho, mostra resumo com links e aria-invalid/mensagens nos campos; conteúdo de observações escapado. Teste rejeita mensagens técnicas em inglês.
- Regressões existentes de ISBN preservam rascunho, campos preenchidos, sugestões/fonte escapadas, CSRF, PRG e associações próprias; testes de navegação, preços e bloqueios também passam.
- Inspeção no navegador in-app de HTML efetivamente renderizado com dados sintéticos, servidor local de prévia: novo preenchido/edição em desktop 1280 px (conteúdo 1265 px), novo/vazio/erros em 320 px (conteúdo 305 px por barra vertical), sem overflow horizontal. Layout, valores e seleção conferidos; Enter expande orientação ISBN; Space altera checkbox; links de erro focam title e workIds com destaque visível. Skip link foca main-content. Após tradução, reload confirma ausência de mensagens técnicas.
- Nenhum formulário enviado no navegador; dados reais não foram usados/alterados. Viewport restaurado, aba fechada e servidor temporário encerrado.
- git diff --check sem problemas.

Tentativas anteriores detectaram tipos de entidades nos fixtures, uso incorreto de DetailedError e expectativa de texto sem entidades HTML; corrigidos e reexecutados. A inspeção visual detectou mensagens de binding em inglês, corrigidas antes da validação final.

## Limites

Sem esquema, serviço/consulta adicional, dependência ou implementação de upload no cadastro; capa continua após salvar. Google Books permanece adiado, fontes externas simuladas nos testes; não alegar lookup real. Esta conferência não é auditoria integral WCAG nem E2E autenticado em produção; servidor coberto por MockMvc. Reiniciar a aplicação no IntelliJ para carregar os recursos novos. data/ não versionado foi preservado.
