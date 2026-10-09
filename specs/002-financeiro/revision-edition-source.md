# Revisão aprovada — financeiro no cadastro da HQ

2026-10-09. O usuário substituiu expressamente a escolha anterior: não quer registrar compras separadas; quer os valores do cadastro da HQ e a data da compra nesse mesmo cadastro. Constituição mantida, sem novo frontend ou serviço externo.

## Specify / clarify

- FIN-R001: fonte única das análises é `editions.reference_paid_price` (campo Valor pago). Cada edição conta uma vez, seja física ou digital; exemplares extras e o ledger anterior não multiplicam ou somam gastos. Preço de capa não é gasto.
- FIN-R002: adicionar `editions.purchase_date`, opcional, entre 1900 e hoje, no formulário de criação/edição da HQ e nos detalhes. Não inferir data antiga nem copiar datas/preços do ledger.
- FIN-R003: edições existentes entram imediatamente no relatório sem período; valor ausente não é zero nem gratuito. Total soma valores conhecidos, média divide apenas pelas edições com valor informado. Contagem e cobertura explicitam quantas edições não têm valor/data.
- FIN-R004: filtros, gráficos, rankings e comparação mantêm os mesmos critérios já aprovados, agora sobre edições. Período exclui data desconhecida; sem período, evolução mensal segue últimos 12 meses. Compartilhamento por personagem/criador continua não aditivo.
- FIN-R005: remover registro/edição/exclusão de compras e seus links da interface e rotas públicas. Financeiro oferece editar a própria edição com retorno aos filtros. Cadastro/edição/exclusão/leitura da edição reflete na próxima consulta financeira.
- FIN-R006: preservar tabelas/lançamentos antigos sem usá-los nos relatórios. Para não bloquear exclusão de HQ por um fluxo desativado, tornar vínculo antigo opcional e desligá-lo ao excluir a edição; valor/data/origem/notas do lançamento permanecem intactos. Não excluir lançamentos nem migrá-los para valores da HQ.

Esta revisão prevalece sobre as regras anteriores de ledger em spec/clarify/plan. Não há ambiguidade de fonte pendente: “valores do cadastro HQ” significa o campo já existente **Valor pago**, não preço de capa. Sem demanda por controle por cópia nesta revisão.

## Plan

1. Flyway V15 adiciona data opcional e constraint/index owner/data. Preserva ledger com FK antiga opcional/SET NULL apenas edition_id.
2. Entity/Command/DTO/Form carregam e validam a data; compatibilidade dos construtores anteriores mantém integrações existentes. MVC autoriza antes da validação na edição, CSRF/PRG existentes.
3. Repository financeiro agrega diretamente edições, com EXISTS e agrupamentos próprios, COALESCE para valores ausentes, média com denominador conhecido. Remover CRUD do ledger do código ativo.
4. Ajustar linguagem, indicadores de cobertura, gráficos e lista de edições. Atualizar guia para o novo fluxo.
5. Testes unitários/domínio e PostgreSQL/MockMvc: dados prévios, preços ausentes/zero, data validada, isolamento, refresh, filtros/paginação, legado não somado e preservado após exclusão.

## Tasks

- [x] FIN-RT001 Migration/data no cadastro e testes. R001/002/006.
- [x] FIN-RT002 Agregações/rotas/interface e navegação. R001/003/004/005.
- [x] FIN-RT003 Verificação completa, documentação e commit. R001–006.

## Analyze

Principal risco corrigido: a fonte anterior exigia duplicação de cadastro que o usuário não quer. Não sincronizar ledger e edição: isso recriaria duplicidade e ambiguidade. Valores ausentes reduzem cobertura e podem subestimar gastos; comparação usa somente o total conhecido, com aviso. Alterações de classificação/valor de edição mudam a análise retroativamente, sem snapshot histórico. Data sem precisão permanece vazia. Excluir edição não deve apagar legado: FK opcional protege valores, sem inventar um arquivo de histórico na interface. Sem alegação de benchmark/auditoria WCAG integral.

## Verificação executada

- Planejamento registrado em `6e9d224` antes das alterações funcionais. Arquivos: migration V15, Edition/Command/EditionView/EditionForm/EditionController e templates de edição; FinanceRepository/Service/View/Controller e painel; navegação compartilhada; guia docs/financeiro.md e testes finance/shared.
- `./mvnw clean verify -q`: **exit 0, 133 testes, 0 falhas, 0 erros, 0 ignorados**. JUnit/Mockito, MockMvc e PostgreSQL 16 em Testcontainers. Primeira rodada encontrou retorno ao catálogo em vez do Financeiro; corrigida allowlist local e coberta por testes antes da rodada final.
- FIN-R001/003/004: integração soma 350 a partir de cinco edições (quatro valores conhecidos, média 87,50), ignorando preço de capa 999, exemplar 333 e ledger 7777. Filtros combinados, vínculos compartilhados, suporte desconhecido, valores ausentes/zero, meses zerados e comparação 150 versus 100 (50%) cobertos. Paginação de 24 edições mantém total integral e distribuição com grupo residual.
- FIN-R002/005: POST cria/edita valor/data na própria edição, mostra data nos detalhes e no formulário de edição, limpa valor/data, recalcula seleção temporal, reflete leitura e exclusão. Datas antes de 1900, futuras e malformadas conservam rascunho, sem mutação. CSRF, dono, XSS, autenticação e retorno validado mantidos; rotas antigas GET/POST retornam 404 autenticado.
- FIN-R006: upgrade explícito de V14 com dados para V15 preserva preços, registros de compra/data/notas e deixa data da HQ desconhecida. Teste de exclusão da HQ mantém lançamento antigo com valor 500/notas/proprietário e vínculo nulo, sem somá-lo ao relatório.
- QA em prévia local de HTML produzido por MockMvc: data ao lado do valor pago no desktop e empilhada no celular, foco por teclado, valor/data preenchidos, alertas de cobertura e tabela mensal aberta por Enter com 12 meses/total conhecido correto. Formulário e painel sem overflow horizontal em 320 px (documento 305 px com barra de rolagem). Interface sem links para registrar compra separada.
- `git diff --check`: exit 0. Consultas continuam agregadas, limitadas/paginadas, sem N+1 por linha e com índice owner/purchase_date/id na V15.

Limites: banco do usuário e processo IntelliJ não alterados durante testes; reinício aplica V15 no banco configurado. Não realizado deploy, benchmark de acervo grande ou auditoria integral WCAG/leitores de tela. Aviso de agente Mockito não impediu testes. `data/` local preservado fora do commit. Foram removidos controller/form/serviços de CRUD de compras do fluxo ativo e os templates exclusivos desse fluxo; nenhum lançamento existente foi apagado.
