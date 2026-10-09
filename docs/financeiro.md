# Financeiro — compras individuais

## Começar a usar

1. Reinicie a aplicação após atualizar o código. O Flyway aplica a migração V14 no banco configurado; não é necessário executar SQL manualmente.
2. Abra **Financeiro → Registrar compra**, selecione uma edição e informe o valor daquela cópia. Para duas cópias, registre duas compras. Compras digitais também são aceitas; zero representa uma compra gratuita.
3. Informe a data real da compra, se conhecida, entre 1900 e hoje. Não use a data de cadastro como substituto. Loja/origem e observações são opcionais.
4. Use os filtros em conjunto e clique em **Aplicar filtros**. Para uma análise temporal, preencha início e fim (até 3.660 dias). Os links dos rankings refinam a seleção; editar/excluir uma compra preserva os filtros de retorno.

Os números são recalculados ao abrir o painel após salvar, editar ou excluir uma compra. As classificações, personagens, criadores e estado de leitura refletem os dados atuais da edição.

## Personagens, autores e coleções

Cadastre personagens no menu **Personagens** e associe-os no formulário de edição. Não há inferência automática a partir do título ou ISBN. A coleção corresponde à série da edição. O filtro **Criador** inclui autores, artistas e outros papéis já vinculados; não presume que todo criador seja roteirista.

## O que os gráficos mostram

- Resumo: total pago, quantidade de compras, média por compra e gasto em edições ainda não lidas.
- Evolução mensal: valores por mês, incluindo meses sem compras, e mês de maior gasto. Sem filtro de período, mostra os últimos 12 meses, enquanto o total e os rankings abrangem todas as compras selecionadas.
- Comparação temporal: período selecionado versus o anterior, com exatamente a mesma duração em dias e os mesmos filtros. Base anterior zero não produz percentual de variação.
- Barras: principais séries, criadores e personagens, ordenados por gasto ou quantidade. Mostram até dez grupos; os totais não são limitados à página da lista.
- Roscas: distribuição monetária por editora e por suporte físico/digital/não informado. Quando necessário, editoras restantes ficam em “Outras editoras”. Compras gratuitas continuam na contagem, mas não formam proporções monetárias.

Cada gráfico tem uma tabela equivalente que pode ser aberta pelo teclado. Uma compra pode ser atribuída integralmente a mais de um personagem/criador: esses rankings são compartilhados, não parcelas somáveis do total.

## Por que o Financeiro está zerado se já tenho edições?

O modelo escolhido é um livro de compras separado. Preços já cadastrados em edições ou exemplares **não são importados nem somados automaticamente**, evitando duplicidade e datas inventadas. Registre as compras que deseja analisar. O resumo da página inicial mantém seu cálculo de referência por edição; não representa o mesmo conjunto de dados do Financeiro.

Uma compra não cria automaticamente um exemplar físico. A data pode ficar em branco: nesse caso, o lançamento entra nos totais sem período, mas não nas análises mensais nem em intervalos selecionados. O painel informa a quantidade e o valor desses lançamentos sem data.

Edições com compras vinculadas não podem ser excluídas para preservar o histórico. Personagens vinculados a edições também exigem desvinculação antes de exclusão. Confirmações de exclusão de compras não apagam a edição ou seus exemplares.

Estas análises organizam gastos registrados; não estimam valor de mercado, rentabilidade ou todas as compras reais que ainda não foram lançadas.
