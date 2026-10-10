# Páginas lidas e não lidas nos relatórios — 2026-10-10

## Constitution → specify → clarify

Complemento de PAGE-001/003 em page-reading.md; constituição e modelo existentes mantidos.

- PNR-001: cadastro e edição preservam Total de páginas opcional e Páginas já lidas (zero por padrão). Explicar que alimentar esses campos atualiza relatórios; não duplicar controles ou migrations já entregues.
- PNR-002: Visão geral, banner de leitura, séries e checklist apresentam total de páginas informado, páginas lidas e páginas não lidas. Não lidas = total conhecido − lidas, com contagem por edição (não por exemplar) e autorização já existente. Contagem derivada em ReadingProgress, não persistida.
- PNR-003: detalhes da edição mostram páginas não lidas quando o total é conhecido. Preservar atualização parcial, conclusão/reset pelo interruptor, validação e PRG. Relatórios refletem cadastro/edição/mudanças de leitura/exclusão na próxima consulta.
- PNR-004: totais desconhecidos não entram nas somas; informar cobertura e, sem qualquer total, mostrar ausência de informação em vez de estimar páginas não lidas. Interface pt-BR, responsiva, acessível.

## Plan → tasks → analyze

- [x] PNR-T1 Acrescentar contagem derivada e apresentação compartilhada; ajuda no cadastro e detalhes.
- [x] PNR-T2 Cobrir zero/parcial/concluído, cadastro e atualização em relatórios com testes e suíte Maven; QA visual de estados conhecidos/desconhecidos.

Total/páginas lidas já são persistidos na migration V17 e enviados pelo formulário. ReadingQueries já agrega com filtro de proprietário/série e bigint; não mudar SQL nem esquema. Usar fragmento compartilhado para manter consistência e CSS próprio com escopo de páginas. Sem novo módulo financeiro ou leitura automática de exemplares.

## Implement → verificação

- `ReadingProgress.java`: pagesUnread derivado. `fragments/reading.html`: contagens explícitas total/lidas/não lidas com cobertura; reutilizado por Visão geral, banner, séries e checklist. `edicoes/form.html`: orientação sobre cadastro de páginas/relatórios; `edicoes/detail.html`: não lidas por edição. `dashboard.css`: blocos responsivos. `ReadingTest.java` e `MinhaGibitecaApplicationTest.java`: cobertura incremental (PNR-001/002/003/004).
- `./mvnw -q -Dui.review.export=true verify`: execução final com saída 0, 162 testes sem falhas/erros/ignorados, PostgreSQL Testcontainers. Primeira execução apontou cenário de teste que removia associação à série ao editar; corrigido formulário de teste para enviar seriesId, sem alteração das regras de produção.
- Verificados campos no GET de cadastro, persistência 120/30 e restante 90, atualização parcial 60/120 e restante 60, edição 150/120 e restante 30 em quatro rotas (Início/Visão geral/séries/checklist), conclusão/reset e exclusão com restante zero. Unitários cobrem total desconhecido, zero lidas, parcial e todas lidas. Autorização/CSRF/validação já testados no fluxo mantido.
- QA com prévias MockMvc em `target/ui-showcase`: relatório com 300 total/140 lidas/160 não lidas, detalhes e edição 120/60, início vazio e banner conhecido em 320/390/768/1024/1440 px. Rodada final sem cache antigo: 25 layouts sem overflow e estabilidade de altura dos quatro painéis do banner nas cinco larguras; CSS grid/borda 1 px confirmado no navegador. Estados desconhecidos não exibem soma inventada. Cache anterior de CSS da prévia detectado; validação final repetida em outra origem local com recursos atuais.
- `git diff --check`: sem erros. Planejamento em `4780d32` antes da implementação. Dados reais e `data/` preservados. Não houve deploy/reinicialização do IntelliJ ou mudança de esquema. Reiniciar aplicação e atualizar CSS se necessário. Atualização ocorre na próxima consulta da página, sem sincronização em tempo real entre abas. Sem pendência funcional identificada.
