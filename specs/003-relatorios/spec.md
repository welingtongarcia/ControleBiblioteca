# Relatórios da coleção — primeira etapa

## Constitution → specify → clarify (2026-10-10)

Constituição v1 mantida. Pedido aprovado: resumo e catálogo do acervo com filtros, PDF e CSV. Leitura detalhada, financeiro comparativo e faltantes específicos ficam para etapas posteriores. Relatórios são consultas do estado atual, sem compras ou registros adicionais.

- REL-001: aba Relatórios em Painéis e análises. Selecionar Resumo da coleção ou Catálogo do acervo. Prévia paginada por 20 edições, ordem título/ID, vazio explicado e links às HQs preservando filtros no retorno.
- REL-002: filtros combináveis por título/ISBN/número, coleção (série), editora, criador, função geral do criador, personagem, suporte físico/digital/não informado, lida/não lida e intervalo de compra inclusivo. Duas datas ou nenhuma, 1900 até hoje, início <= fim. IDs devem pertencer ao usuário. Função combinada com criador deve ser da mesma pessoa; sem criador, qualquer associado com a função.
- REL-003: resumo do recorte mostra edições, edições físicas/digitais/não informadas, exemplares físicos vinculados, gasto registrado nas HQs, preços não informados, edições lidas, páginas informadas/lidas/restantes e edições sem páginas. Valores vêm exclusivamente de editions.reference_paid_price como no Financeiro; não somar novamente acquisition_price. Edição física não é sinônimo de exemplar. Páginas desconhecidas não viram zero conhecido e devem ser sinalizadas. Data não informada exclui a HQ só quando há filtro de período.
- REL-004: PDF direto com título, data/hora de emissão pt-BR (America/Sao_Paulo), filtros por nomes, resumo, distribuição por suporte e, no catálogo, todas as edições do recorte com título/número/ISBN, série/editora/criadores/funções/personagens, suporte/leitura/páginas, preço/data e localização dos exemplares. Paginação, texto longo com quebra e fontes Unicode embutidas; não buscar capas/recursos externos. CSV UTF-8 BOM, separador ponto e vírgula, escaping e proteção contra fórmulas; resumo em chave/valor, catálogo uma linha por edição com metadados e indicação de ausências. Incluir filtros/data de emissão. Downloads privados sem cache e nomes fixos, sem ownerId externo.
- REL-005: exportação contém todo o recorte, não só a página. Limite explícito de 10.000 edições para catálogo, rejeitar excesso com orientação sem truncamento silencioso. Resumo pode ser emitido acima do limite pois não enumera HQs. Consultas parametrizadas, sem multiplicar valores por joins de criadores/exemplares. Snapshot consistente em transação read-only repeatable-read para totais/linhas; sem persistir relatório.
- REL-006: pt-BR, HTML semântico, filtros rotulados, foco, responsividade, CSRF mantido nas ações existentes, autorização em tela/exportações. CSV/PDF não são backups completos nem avaliação de mercado. Testar conteúdo, números, filtros, propriedade, segurança e paginação.

## Plan → tasks → analyze

- [ ] REL-T1 Criar módulo reports com filtros/DTOs/serviço e consultas JDBC, contagens/agregados sem N+1. Usar índices existentes por proprietário/título/série/editora/data e vínculos; Flyway V19 para índice creator_functions(function, creator_id), usado pelo filtro de atuação.
- [ ] REL-T2 Tela, navegação/retorno e links de emissão com filtros intactos. Sem frontend novo.
- [ ] REL-T3 Emissores Java CSV e PDF (Apache PDFBox 3.0.8, conforme documentação oficial), fonte DejaVu Sans embutida com licença, sem dependência do sistema operacional em runtime.
- [ ] REL-T4 JUnit/Mockito, PostgreSQL Testcontainers/MockMvc, snapshots de arquivos sintéticos, renderização Poppler do PDF e QA desktop/móvel; suíte verify; registrar resultados e commit.

Riscos tratados: dupla contagem evitada por EXISTS/agregados, dados ausentes explícitos, CSV injection neutralizada, exportação limitada, grafismos sem uso de cores como única informação. PDF usa texto selecionável, mas não será declarado PDF/UA. Fonte sem glifo será sinalizada/substituída sem derrubar emissão; original permanece no CSV. Não alterar DB real ou reiniciar IntelliJ na verificação. `data/` preexistente preservado.

## Verificação do planejamento

Lidos AGENTS.md, constituição, consultas do painel/Financeiro, esquema de edições/exemplares e navegação. Financeiro usa preço/data da HQ; distinção de exemplares mantida. PDFBox dependency confirmada em https://pdfbox.apache.org/3.0/getting-started.html. `git diff --check` antes do commit de planejamento.
