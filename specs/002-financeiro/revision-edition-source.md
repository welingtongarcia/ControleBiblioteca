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

- [ ] FIN-RT001 Migration/data no cadastro e testes. R001/002/006.
- [ ] FIN-RT002 Agregações/rotas/interface e navegação. R001/003/004/005.
- [ ] FIN-RT003 Verificação completa, documentação e commit. R001–006.

## Analyze

Principal risco corrigido: a fonte anterior exigia duplicação de cadastro que o usuário não quer. Não sincronizar ledger e edição: isso recriaria duplicidade e ambiguidade. Valores ausentes reduzem cobertura e podem subestimar gastos; comparação usa somente o total conhecido, com aviso. Alterações de classificação/valor de edição mudam a análise retroativamente, sem snapshot histórico. Data sem precisão permanece vazia. Excluir edição não deve apagar legado: FK opcional protege valores, sem inventar um arquivo de histórico na interface. Sem alegação de benchmark/auditoria WCAG integral.
