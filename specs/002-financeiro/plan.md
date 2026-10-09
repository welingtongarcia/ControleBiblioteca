# Plano preliminar — Financeiro

> Histórico da implementação anterior. A revisão vigente está em [revision-edition-source.md](revision-edition-source.md): valores e data no cadastro da HQ substituem o ledger como fonte financeira.

Modelo aprovado: compras individuais em financial_purchases; SQL/JDBC parametrizado em repository separado, DTOs/serviços por domínio, sem entidades nos controllers.

Gate fechado: migration V14 cria ledger e personagens/vínculos com FKs compostas por proprietário; dinheiro numeric(12,2), uma cópia por lançamento. Lock de usuário sincroniza compra/exclusão de edição; exclusão de personagem referenciado bloqueada. Autorização antecede renderização de formulários de edição/remoção.

1. Fonte/semântica confirmadas em clarify.md; constituição compatível e rastreabilidade FIN-T002.
2. Modelar datas e personagens/vínculos por proprietário com Flyway; se lançamentos individuais escolhidos, especificar fluxo e reaproveitamento conservador antes da migração.
3. Módulo financeiro com presentation/application/domain/infrastructure, DTO de filtros e agregações PostgreSQL parametrizadas. Filtrar vínculo via EXISTS para evitar multiplicação de joins; validar dono de cada opção. Agregações fixas e rankings limitados, registros paginados.
4. Serviço de comparações/médias/cobertura em decimal e datas reais; cálculos descritivos determinísticos, sem IA ou chamadas externas.
5. Tela Financeiro compartilhando menu/trilhas, filtro GET, indicadores, gráficos SVG e tabelas equivalentes; links para refinar seleção sem ampliar escopo.
6. Testes unitários e PostgreSQL/MockMvc para números, ausência, filtros, datas, múltiplos vínculos/isolamento, migrations e atualização; QA visual desktop/móvel/teclado.
7. Registrar comandos/resultados/limites, marcar tarefas atendidas e commits coerentes após validação de cada fase.

Sem pacote frontend, schema automático, importação silenciosa de preços/datas ou consultas por cartão. Índices finais dependem do modelo aprovado.
