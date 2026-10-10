# Leitura por páginas e banner estável — 2026-10-09

## Constitution → specify → clarify

Constituição mantida. Interpretado “independente de parceiros” como independentemente do painel/conteúdo do banner. Pedido inclui controle do total de páginas e páginas já lidas por edição, sem confundir com exemplares.

- PAGE-001: cadastro/edição possui total de páginas opcional (1–100000) e páginas lidas (0–total, padrão 0). Proibir contagem positiva sem total conhecido, negativos e excesso; validar formulário, domínio e banco Flyway. Dados antigos preservam `is_read` e ficam com total desconhecido/0 páginas registradas; não inventar extensão.
- PAGE-002: detalhes exibem contagem e progresso, e permitem atualizar páginas lidas com autorização por proprietário, CSRF, validação e PRG. Com total conhecido, igualdade marca concluída; abaixo do total marca não concluída. Interruptor “Já li” preenche todas as páginas conhecidas; desligar reinicia em zero. Informar essas consequências. Sem total, interruptor conserva funcionamento anterior; zerar/remover total não inventa leitura anterior.
- PAGE-003: painel geral, banner de leitura e séries mostram somas de páginas lidas/total conhecido e percentual por páginas separado do percentual por edições. Exibir cobertura de totais conhecidos; desconhecidos ficam fora do denominador de páginas, mas dentro do percentual por edições. Agregação pelo proprietário e série, sem N+1; atualizar na próxima requisição após mutações.
- PAGE-004: todos os painéis do banner compartilham uma altura por largura de tela, determinada pelo maior conteúdo, incluindo textos longos; alternar não move os controles/fileiras. Responsivo e sem corte/rolagem interna obrigatória. Painéis invisíveis não recebem foco nem aparecem para tecnologia assistiva. Manter pausa e movimento reduzido.

## Plan → tasks → analyze

- [ ] PG-T1 Flyway V17, invariantes de domínio, campos Command/DTO/form e integração de cadastro/edição; preservar construtores usados pelos testes.
- [ ] PG-T2 Atualização parcial protegida nos detalhes, sincronização do interruptor e agregados por páginas geral/série; templates e orientações pt-BR.
- [ ] PG-T3 Empilhar painéis no mesmo espaço CSS com altura do maior, mantendo inatividade sem foco/exposição; testes do controlador do banner.
- [ ] PG-T4 JUnit/MockMvc/PostgreSQL, upgrade de dados legados, validação de limites/isolamento/CSRF/agregados e QA de altura/responsividade; registrar resultados reais e limitações.

Sem novo frontend, polling ou leitura inferida de exemplar. Reusar consulta agrupada de leitura e DTO com construtor compatível. Soma de páginas usa bigint para evitar overflow; desconhecidos são explicitamente excluídos. Formulário de edição preenche os valores persistidos; editar a extensão com total conhecido recalcula conclusão usando páginas lidas explicitamente informadas. Para não perder conteúdo ao fixar tamanho, CSS Grid sobrepõe os painéis, inclusive os inativos, e usa sua altura máxima; `visibility`, `inert` e `aria-hidden` garantem inatividade. Nenhum tamanho absoluto que corte conteúdo longo.
