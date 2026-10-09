# Financeiro — valores do cadastro da HQ

## Começar a usar

1. Reinicie a aplicação após atualizar o código. O Flyway aplica a migração V15 no banco configurado; não é necessário SQL manual.
2. Cadastre ou edite uma edição no catálogo. Preencha **Valor pago (R$)** e **Data da compra** no próprio cadastro da HQ. Não há formulário separado de compra.
3. A data é opcional, entre 1900 e hoje. Se desconhecida, deixe em branco; nunca use a data de publicação/cadastro como substituto. Valor em branco é desconhecido; zero informado é gratuito.
4. Abra **Financeiro** e combine filtros de período, coleção/série, criador/autor, personagem, editora e suporte. Para períodos, preencha início e fim, até 3.660 dias.
5. Para corrigir um valor ou data, use **Editar HQ** na lista financeira. Ao salvar, você volta ao painel com os filtros preservados.

Edições existentes já entram no painel sem período. Cada edição conta uma única vez, física ou digital, independentemente de quantos exemplares você tenha. Cadastro, edição, exclusão e alteração de leitura refletem na próxima consulta, sem cache de indicadores.

## Cálculos e gráficos

- Total: soma do **Valor pago** das edições selecionadas. Não soma preço de capa nem valores de aquisição de exemplares extras.
- Contagem: quantidade de edições selecionadas, incluindo valores desconhecidos.
- Média: total dividido apenas pela quantidade com valor informado, incluindo gratuitas. Sem valores conhecidos, mostra “Não informado”.
- Cobertura: quantidade de edições sem valor e sem data. Totais monetários representam somente dados conhecidos e podem subestimar gastos reais.
- Evolução mensal: data real de compra da edição. Sem período selecionado, últimos 12 meses; os demais totais abrangem todas as edições da seleção. Meses sem gasto conhecido mostram zero.
- Comparação temporal: período anterior com a mesma duração e filtros. Mostra cobertura de ambos e variação apenas se o total anterior for positivo.
- Rankings: séries, criadores e personagens por valor conhecido ou quantidade de edições, até dez grupos; links refinam os filtros.
- Roscas: distribuição monetária por editora e físico/digital/não informado. Grupos exclusivos; editoras excedentes agrupadas em “Outras editoras”.

Os gráficos usam toda a seleção, não somente a página da lista. Tabelas equivalentes abrem pelo teclado. Uma HQ pode aparecer integralmente em vários personagens/criadores: suas linhas não são parcelas somáveis.

## Classificações e histórico

Cadastre personagens no menu **Personagens** e vincule-os no formulário da edição. Coleção corresponde à série atual; Criador inclui os papéis já vinculados, sem presumir autoria ou inferir personagens pelo título/ISBN. Alterar classificação ou leitura da edição muda a análise retroativamente.

Sem data, a HQ participa do total sem período, mas fica fora de períodos e do gráfico mensal. Não há importação automática de datas antigas.

O antigo fluxo de compras individuais foi desativado, incluindo suas rotas. Seus registros permanecem no banco, mas não entram nos cálculos nem são transferidos para o cadastro da HQ. Se uma edição for excluída, somente o vínculo do lançamento antigo é desligado; valor, data, origem, observações e proprietário permanecem preservados.

O resumo inicial e o Financeiro sem filtros usam a mesma fonte de valores por edição. Estas análises não estimam valor de mercado ou rentabilidade.
