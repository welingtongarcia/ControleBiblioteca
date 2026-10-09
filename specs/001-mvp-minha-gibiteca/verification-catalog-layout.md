# Verificação T030 — catálogo de edições

Data: 2026-10-09. Requisitos FR-046, AC-018 e regressão AC-005/009/013/014.

## Entrega e arquivos

- spec.md, plan.md, tasks.md e analysis.md: requisitos, clarificação, plano/tarefa e análise antes da implementação.
- templates/edicoes/list.html: busca/ordem, filtros expansíveis abertos quando ativos, cartões, capa/fallback, metadados, valores, leitura, ações contextuais e exclusão expansível com aviso.
- static/css/main.css: estilos escopados ao catálogo, grade adaptável, alvos/foco de teclado e quebras responsivas.
- MinhaGibitecaApplicationTest.java: dois cenários novos de apresentação/vazios e faixa/filtros no teste paginado existente; fixtures sintéticas em target/ui-catalog.

## Evidências executadas

- ./mvnw clean verify -q: execução final com código 0; 112 testes, zero falhas/erros/skips. Integração MockMvc/Thymeleaf com PostgreSQL 16 via Testcontainers; JAR gerado. Tentativas anteriores revelaram imports ausentes, XPath incompatível com HTML5 e strings vazias confundidas com filtros ativos; corrigidos antes da execução final.
- Cartões: quatro registros, suporte físico/digital/desconhecido, preços distintos/zero/ausentes, variante/ISBN/ano/formato, capa própria, título escapado e estado lido/não lido. CSRF gerado pelo Thymeleaf, sem campo manual duplicado.
- Regressão existente: filtros combinados, 25 resultados em duas páginas, retorno contextual, PRG, autorização, leitura e preços; faixa inicial 1–20 conferida.
- Navegador in-app em prévia local do HTML realmente renderizado, não no banco do usuário: desktop 1280 px, conteúdo 1265 px; móvel 320 px, conteúdo 305 px (barra vertical), sem overflow horizontal. Inspeção visual de busca, grade, filtros e cartões com valores/ações; disclosure de filtros e exclusão abriu com Enter, aviso e foco visíveis. Link para pular conteúdo focou main-content. Vazio e consulta sem resultados conferidos; filtro Digital selecionado e aberto automaticamente. Nenhum POST executado pelo navegador.
- Viewport restaurado, aba temporária fechada e servidor de prévia encerrado. git diff --check sem problemas.

## Limites e riscos

Sem migração, dependências, consultas adicionais por cartão ou alteração de dados reais. Não inferir exemplares/posse nem avaliação de mercado. Não é auditoria integral WCAG, benchmark p95 ou E2E autenticado em produção; fluxos do servidor cobertos por MockMvc. Prévia estática não é deploy. Reiniciar aplicação no IntelliJ para carregar os recursos atualizados. Configuração Google Books permanece adiada.
