# Páginas lidas e não lidas nos relatórios — 2026-10-10

## Constitution → specify → clarify

Complemento de PAGE-001/003 em page-reading.md; constituição e modelo existentes mantidos.

- PNR-001: cadastro e edição preservam Total de páginas opcional e Páginas já lidas (zero por padrão). Explicar que alimentar esses campos atualiza relatórios; não duplicar controles ou migrations já entregues.
- PNR-002: Visão geral, banner de leitura, séries e checklist apresentam total de páginas informado, páginas lidas e páginas não lidas. Não lidas = total conhecido − lidas, com contagem por edição (não por exemplar) e autorização já existente. Contagem derivada em ReadingProgress, não persistida.
- PNR-003: detalhes da edição mostram páginas não lidas quando o total é conhecido. Preservar atualização parcial, conclusão/reset pelo interruptor, validação e PRG. Relatórios refletem cadastro/edição/mudanças de leitura/exclusão na próxima consulta.
- PNR-004: totais desconhecidos não entram nas somas; informar cobertura e, sem qualquer total, mostrar ausência de informação em vez de estimar páginas não lidas. Interface pt-BR, responsiva, acessível.

## Plan → tasks → analyze

- [ ] PNR-T1 Acrescentar contagem derivada e apresentação compartilhada; ajuda no cadastro e detalhes.
- [ ] PNR-T2 Cobrir zero/parcial/concluído, cadastro e atualização em relatórios com testes e suíte Maven; QA visual de estados conhecidos/desconhecidos.

Total/páginas lidas já são persistidos na migration V17 e enviados pelo formulário. ReadingQueries já agrega com filtro de proprietário/série e bigint; não mudar SQL nem esquema. Usar fragmento compartilhado para manter consistência e CSS próprio com escopo de páginas. Sem novo módulo financeiro ou leitura automática de exemplares.
