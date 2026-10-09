# Análise preliminar — Financeiro

Solicitação amplia o MVP com controle financeiro, agora autorizado em escopo, mas não determina fonte contábil. Principal risco: referências de edição e aquisições de cópias representam universos diferentes e podem ser o mesmo gasto. Decisão do usuário necessária para fechar cálculos/modelo.

Outros riscos: data desconhecida excluída silenciosamente dos períodos; joins multiplicando valores; personagens/autoria inferidos; grupos de ranking sobrepostos apresentados como distribuição; percentual de mudança com base zero. Requisitos explicitam cobertura, EXISTS, vínculos manuais, atribuição compartilhada e base insuficiente.

Arquitetura compatível com constituição se agregações/DTOs/autorizações estiverem no servidor, Flyway para mudanças, dados antigos conservados e todos os gráficos tiverem equivalente tabular. Novos filtros/rotas precisam integrar allowlist de retorno/menu; não relaxar CSRF/dono.

Status: escolha 2 confirmada; gate concluído para implementação conforme clarify/plan. Valor obrigatório no ledger torna cobertura de preços 100% dos lançamentos, não 100% das compras reais do usuário; alerta de universo evita falsa completude. Datas ainda opcionais; período invalida intervalo parcial/invertido/excessivo, não inventa datas. Personagens privados com FKs compostas e vínculo via EXISTS. Bloquear edição referenciada preserva histórico, sincronizando lock de usuário com criação/mudança do lançamento. Consultas fixas/paginadas; sem N+1, desempenho em massa precisa evidência própria e não será alegado por teste funcional.

Testes, migrations e gráficos ainda não executados neste gate. data/ não versionado preservado.
