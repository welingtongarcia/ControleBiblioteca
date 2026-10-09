# Plano preliminar — Financeiro

Não fechado enquanto a fonte financeira aguarda confirmação.

1. Fechar fonte/semântica e ajustes de escopo; conferir constituição e rastreabilidade.
2. Modelar datas e personagens/vínculos por proprietário com Flyway; se lançamentos individuais escolhidos, especificar fluxo e reaproveitamento conservador antes da migração.
3. Módulo financeiro com presentation/application/domain/infrastructure, DTO de filtros e agregações PostgreSQL parametrizadas. Filtrar vínculo via EXISTS para evitar multiplicação de joins; validar dono de cada opção. Agregações fixas e rankings limitados, registros paginados.
4. Serviço de comparações/médias/cobertura em decimal e datas reais; cálculos descritivos determinísticos, sem IA ou chamadas externas.
5. Tela Financeiro compartilhando menu/trilhas, filtro GET, indicadores, gráficos SVG e tabelas equivalentes; links para refinar seleção sem ampliar escopo.
6. Testes unitários e PostgreSQL/MockMvc para números, ausência, filtros, datas, múltiplos vínculos/isolamento, migrations e atualização; QA visual desktop/móvel/teclado.
7. Registrar comandos/resultados/limites, marcar tarefas atendidas e commits coerentes após validação de cada fase.

Sem pacote frontend, schema automático, importação silenciosa de preços/datas ou consultas por cartão. Índices finais dependem do modelo aprovado.
