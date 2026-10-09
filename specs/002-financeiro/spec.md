# Financeiro — especificação proposta

2026-10-09. Status: especificação inicial; base financeira aguardando decisão do usuário. Não implementar cálculos/migrações antes de fechar clarificação, plano, tarefas e análise.

## Objetivo e escopo solicitado

Aba Financeiro privada para acompanhar gastos registrados, aplicar filtros combinados, visualizar gráficos comparativos e comparar períodos/personagens. Evolução solicitada explicitamente pelo usuário, além do MVP inicial. Manter Java 21/MVC/Thymeleaf/CSS/PostgreSQL, DTOs, Flyway e autorização por dono.

## Requisitos

- FIN-001: menu Financeiro com resumo, filtros e listagem paginada que expliquem o universo analisado; todos os indicadores/gráficos obedecem à mesma seleção, não só à página da listagem.
- FIN-002: filtros combinados por período de compra, série/coleção, criador, personagem, editora e suporte físico/digital/não informado; limpar filtros e parâmetros GET preservados. Coleção significa série existente nesta entrega.
- FIN-003: fonte financeira explícita, valores decimais, desconhecido diferente de zero e preço de capa nunca tratado como gasto. Não somar referência da edição à aquisição de suas cópias. Totais calculados na leitura seguinte após alterações, sem cache desatualizado.
- FIN-004: data de compra real opcional, nunca inferida da criação do cadastro ou ano de publicação. Sem data entra no total sem filtro temporal, mas não em um período selecionado; quantidade/valor sem data destacados para mostrar cobertura. Migrações preservam dados anteriores.
- FIN-005: personagens privados cadastrados manualmente e associados a zero ou mais edições, sem inferência por título/ISBN. Criador existente não possui papel autor/desenhista: usar rótulo Criador nesta etapa, sem inventar autoria. Vínculos próprios, validação, PRG/CSRF e proteção de referências.
- FIN-006: indicadores de total conhecido, registros analisados, valor médio dos registros com preço conhecido e cobertura de preços/datas. Sem dados/denominador zero: informação insuficiente, não percentuais fictícios.
- FIN-007: gráficos de linha por mês e barras comparando período atual/anterior; barras horizontais de principais séries/criadores/personagens/editoras; distribuição em rosca por editora e suporte quando grupos são mutuamente exclusivos. Limitar rankings e agregar demais em Outros somente quando matematicamente válido.
- FIN-008: tabelas equivalentes acessíveis para todos os gráficos, textos pt-BR, valores em reais, legenda e cores que não sejam a única informação. SVG renderizado no servidor/HTML/CSS, sem SPA/Node ou dependência de serviços externos.
- FIN-009: comparação com período anterior de mesma duração em dias, imediatamente anterior à data inicial. Mostrar datas comparadas, diferenças absolutas; percentual somente se base anterior positiva. Filtros bibliográficos iguais para ambos. Sem período selecionado, orientar escolha antes da comparação temporal.
- FIN-010: rankings por quantidade e valor registrado, com links de filtro para explorar séries/criadores/personagens. Uma edição/transação associada a vários personagens/criadores conta uma vez no total filtrado, mas pode aparecer em várias linhas de ranking. Indicar atribuição compartilhada; não apresentar soma dessas linhas como total nem usar rosca para grupos sobrepostos.
- FIN-011: análises descritivas adicionais: mês com maior gasto, concentração por editora, físicos versus digitais, valor médio por registro e valor registrado em edições ainda não lidas. Não alegar avaliação de mercado, rentabilidade, economia real ou recomendações de investimento; sempre indicar base e cobertura.
- FIN-012: nenhuma migração automática de gastos ou datas existentes sem regra aprovada e verificada; dado alheio não aparece em opções, listagem, gráficos ou agregados.

## Clarificação bloqueante

Escolher um modelo:

1. **Valores cadastrados nas edições (recomendação de continuidade):** cada edição contribui uma vez com reference_paid_price, igual ao resumo inicial, incluindo digitais. Adicionar data de compra opcional ao registro da edição. Cópias extras não aumentam esse total; valores de aquisição de exemplares ficam separados. Rótulo valor pago registrado, não livro-caixa de transações.
2. **Compras individuais:** cada aquisição é um lançamento próprio vinculado a uma edição; admite cópias extras e compras digitais, datas/valores por lançamento. Referências de edição nunca somadas automaticamente. Exige fluxo de lançamentos e decisão de reaproveitamento dos dados existentes, com revisão explícita para não duplicar.

Nenhuma destas fontes será escolhida silenciosamente.

## Aceitação prevista

- FIN-AC-001: vazio, gratuitos, valores ausentes, sem datas e física/digital/desconhecida; totais e média BigDecimal corretos.
- FIN-AC-002: filtros combinados por dimensão/período, ausência de datas, limites inclusivos, mais de uma página e parâmetros preservados; resultados de outro dono sempre excluídos.
- FIN-AC-003: múltiplos criadores/personagens e cópias não multiplicam total; ranking compartilhado claramente explicado.
- FIN-AC-004: períodos com mesma duração, transição mês/ano/ano bissexto, zeros e base insuficiente; comparação consistente.
- FIN-AC-005: atualizar valores/datas/vínculos e leitura reflete na próxima leitura; POST inválido não altera resultados.
- FIN-AC-006: CSRF, autenticação, autorização de filtros/rotas, XSS, datas inválidas/invertidas, retorno local e referência estrangeira.
- FIN-AC-007: migrations conservadoras e índices conforme consultas; agregações no banco, paginação/limites e verificação de N+1.
- FIN-AC-008: gráficos/tabelas iguais para filtros ativos, teclado, foco e 320px; JUnit/Mockito, MockMvc/PostgreSQL Testcontainers e clean verify. Sem alegar testes/deploy executados antes de evidência.
