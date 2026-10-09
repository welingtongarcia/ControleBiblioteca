# Análise preliminar — Financeiro

Solicitação amplia o MVP com controle financeiro, agora autorizado em escopo, mas não determina fonte contábil. Principal risco: referências de edição e aquisições de cópias representam universos diferentes e podem ser o mesmo gasto. Decisão do usuário necessária para fechar cálculos/modelo.

Outros riscos: data desconhecida excluída silenciosamente dos períodos; joins multiplicando valores; personagens/autoria inferidos; grupos de ranking sobrepostos apresentados como distribuição; percentual de mudança com base zero. Requisitos explicitam cobertura, EXISTS, vínculos manuais, atribuição compartilhada e base insuficiente.

Arquitetura compatível com constituição se agregações/DTOs/autorizações estiverem no servidor, Flyway para mudanças, dados antigos conservados e todos os gráficos tiverem equivalente tabular. Novos filtros/rotas precisam integrar allowlist de retorno/menu; não relaxar CSRF/dono.

Status: análise inicial, não gate concluído. Testes, desempenho, migrations e gráficos ainda não implementados/executados. data/ não versionado preservado.
