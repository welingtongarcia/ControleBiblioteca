# Organização de Editar edição — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Melhorar ordem e orientação do formulário compartilhado, com atalhos exclusivos da edição. Preservar todos os dados, validação, vínculos, autorização, CSRF e PRG.

- EEL-001: ordem Identificação, ISBN, Suporte e valores, Páginas e leitura, Vínculos bibliográficos, Observações; identificação da HQ no cabeçalho ao editar. Busca externa continua exclusiva do cadastro, sem novas chamadas ao editar.
- EEL-002: navegação por âncoras na edição para identificação, compra, leitura, vínculos e observações; ações superiores Salvar alterações e Cancelar, mantendo ações inferiores e o mesmo formulário POST. Nenhum dado salvo ao usar âncoras/Cancelar.
- EEL-003: largura confortável, agrupamento responsivo e foco/atalhos por teclado, sem controles sobrepostos ou overflow em 320/390/768/1024/1440 px. Erros e rascunho preservados; campos de páginas com aria-invalid e descrição de erro.

## Plan → tasks → analyze

- [x] EEL-T1 Reordenar fieldsets, acrescentar atalhos/ações e CSS específico, sem mudar backend.
- [x] EEL-T2 Verificar ordem/ações/validação em MockMvc e suíte Maven; QA edição em desktop/móvel.

Hoje Páginas e leitura precede o título e ISBN. Template atende cadastro/edição: reordenação comum mantém cadastro coerente; elementos superiores exclusivos da edição. Botão superior usa atributo form com ID do formulário, sem form aninhado ou JavaScript. Nenhum CSS sticky que oculte campos/foco.

## Implement → verificação

- `templates/edicoes/form.html`: nova ordem, título da HQ, navegação por âncoras, ações superiores exclusivas da edição e associação HTML ao formulário único. Aria-invalid/descrição nos campos de páginas. `static/css/main.css`: toolbar, atalhos e título com escopo específico. `MinhaGibitecaApplicationTest.java`: ordem dos fieldsets, formulário/botão superior e exportação completa de CSS/JS das prévias.
- `./mvnw verify -q`: saída 0, 162 testes sem falhas/erros/ignorados, incluindo preservação e remoção de vínculos, erros/rascunhos, retorno contextual, páginas, lookup exclusivo do cadastro, CSRF e proprietário com PostgreSQL Testcontainers.
- QA das prévias `target/ui-edition-form`: edição, cadastro vazio/preenchido e erros em 320/390/768/1024/1440 px (20 combinações), sem overflow ou campos fora da viewport. Capturas desktop/móvel inspecionadas. Atalho Leitura posiciona fieldset a 24,08 px do topo; Tab do total de páginas foca páginas lidas com outline 3 px. Botão superior referencia edition-entry-form, toolbar grid confirmado. Sem submissão na prévia estática.
- Cache anterior dos recursos da prévia identificado e validação final feita em nova origem local com os CSS/JS atuais. Servidores temporários encerrados e viewport restaurado. `git diff --check`: sem erros. Planejamento registrado em `b94dea2` antes do código.
- Sem deploy/reinicialização do IntelliJ, mudanças de domínio/esquema ou dados reais; `data/` preservado. Recarregar recursos/reiniciar a aplicação se necessário. Sem pendência funcional identificada no escopo.

### Complemento da verificação das prévias

- Corrigida a exportação de `home-showcase.css` e `sidebar.js` no helper de prévias do formulário (antes faltavam nesses arquivos estáticos de teste). Três testes direcionados de formulário, vínculos e validação executados após o ajuste, saída 0.
- Repetidas as 20 combinações responsivas com todos os CSS/JS carregados (HTTP 200/304), sem overflow horizontal ou campos fora da viewport. Menu lateral carregado; servidor temporário encerrado, aba de teste fechada e viewport restaurado.
