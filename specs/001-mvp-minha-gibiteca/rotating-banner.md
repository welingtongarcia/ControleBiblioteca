# Banner rotativo do Início — 2026-10-09

## Constitution → specify → clarify

Constituição mantida. Pedido: substituir o destaque único por banner rotativo com informações interessantes da Gibiteca. Complementa `visual-home.md`: autoplay passa a ser permitido somente no banner, nunca nas fileiras. Assumido ciclo de oito segundos com controles explícitos; não adicionar serviço externo, biblioteca ou build frontend.

- BANNER-001: quatro painéis: edição em destaque (ou convite de primeiro cadastro), progresso real de leitura, composição física/digital da coleção e total pago nos cadastros com acesso ao Financeiro. Não confundir edições com exemplares nem somar valores de ambos; explicar preços ausentes. Visão geral e fileiras preservadas.
- BANNER-002: dados agregados existentes por proprietário, recalculados a cada requisição; sem estimar totais a partir das fileiras limitadas. Capas continuam protegidas e conteúdo escapado.
- BANNER-003: rotação a cada oito segundos, anterior/próximo, seleção direta e pausar/retomar. Ao receber foco, parar até retomada explícita; hover e aba invisível suspendem o temporizador. Movimento reduzido inicia pausado e mudanças dessa preferência também pausam. Apenas o painel visível permite foco; estado anunciado somente em navegação manual, sem anúncios automáticos repetitivos.
- BANNER-004: pt-BR, layout responsivo 320/390/768/1024/1440 com menu aberto/oculto, foco visível, sem animação obrigatória. Sem JavaScript, todos os conteúdos continuam disponíveis e controles inoperantes não são mostrados.

## Plan → tasks → analyze

- [ ] BN-T1 Reutilizar DTOs/serviços de resumo e leitura no handler do Início, mantendo nomes distintos do painel completo; testar isolamento e atualização real das métricas.
- [ ] BN-T2 Compor os quatro painéis SSR e controles acessíveis, CSS próprio e JavaScript progressivo pequeno sem dependência.
- [ ] BN-T3 Executar Maven/JUnit/MockMvc/PostgreSQL, exportar fixtures e verificar rotação, pausa, foco, seleção direta e responsividade no navegador; registrar evidências e limites.

Não alterar banco nem regras financeiras/leitura. As consultas agregadas já existentes preservam semântica e autorização, evitando calcular estatísticas sobre a seleção de 12. A ausência de preço não significa gasto zero. Interação será nativa por botões, sem capturar teclas fora dos controles e sem deslocar foco ao rotacionar. Não reativar autoplay após foco sem ação do usuário. Sem JavaScript, a composição linear é a alternativa funcional.
