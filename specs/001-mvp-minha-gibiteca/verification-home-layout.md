# Verificação — reorganização da página inicial

Data: 2026-10-09. T029, FR-045, AC-017.

## Planejamento e arquivos

Constituição/AGENTS revistos. Especificação, clarificação comunicada, plano, tarefas e análise antes da alteração funcional. Escopo: hierarquia visual do início, sem novos indicadores, regras financeiras ou consultas. Google Books permanece sem configuração nesta tarefa.

- dashboard/index.html: título/contexto/busca, Cadastrar edição e Ver catálogo no topo; quatro cartões agrupam edições/suporte, exemplares/cobertura, valor pago e leitura geral. Banner promocional removido. Estado vazio orienta primeiro cadastro; aviso de preço incompleto permanece visível. Metodologia e valores de aquisição dos exemplares em details/summary nativo, sem JS. Catálogo/séries/localizações agrupados como acervo; editoras/obras/criadores como apoio, footer sem CTA repetido. IDs dos indicadores preservados; singular/plural do aviso ajustado.
- dashboard.css: classes home-* isolam cabeçalho, ações, métricas, disclosure e atalhos. Cartões 4→2→1 colunas, layout flexível e alvos mínimos de 44px. Sidebar e demais telas preservadas; estilos antigos compartilhados não removidos.
- MinhaGibitecaApplicationTest: asserções de ordem/ausência do banner, dados de coleção parcial e leitura 33,3%, indicador/cópias, aviso fora do disclosure e estado vazio; fixtures HTML renderizadas em target/ui-home/index.html e empty.html.

Controller, serviços, queries, entidades e schema não alterados. Atualização por nova requisição e autorização por dono continuam nas regras existentes.

## Evidências executadas

`./mvnw clean verify -q` final: código 0, Java 21; **110 testes sem falhas, erros ou ignorados**. 39 integrados/MVC, 22 navegação, 10 checklist, 8 exemplares, 4 leitura, 3 resumo, 4 ISBN, 6 Open Library, 9 Google Books, 5 composição. PostgreSQL 16 Testcontainers, migrations V1–V13 e upgrade verificados. Execuções anteriores também passaram; execução final inclui asserções e ajustes textuais finais.

MVC para / e /dashboard verifica título, CSS, ordem ações → indicadores → acervo → apoio, destinos reais, busca GET, logout POST/CSRF, anonimato e vazio. Coleção própria de três edições (uma física, uma digital, uma não informada), quatro cópias e uma lida: total registrado R$ 1.499,00 sem dupla contagem; aquisições separadas R$ 30,30; leitura 33,3%; aviso de um preço ausente visível. Dados de outro proprietário excluídos. Regressões continuam validando atualizações após cadastro/edição/exclusão, preço desconhecido/zero e alteração de suporte/leitura.

Prévia estática de HTML efetivamente renderizado pelo MockMvc: desktop padrão conferido por capturas de viewport superior/inferior; cartões, ações e atalhos legíveis. Details expandido via Enter, estado open=true, valor separado e metodologia acessíveis. Celular 320×780: cartões em coluna, topo/estado vazio e atalhos inferiores conferidos; document.scrollWidth=305 e innerWidth=320 (15px de scrollbar), sem overflow horizontal, tanto com disclosure aberto quanto fechado. Pular para o conteúdo por Enter focou main-content. Coleção vazia também conferida em desktop.

Limitações da ferramenta visual: captura fullPage falhou, então usadas capturas de viewport/rolagem e árvore de acessibilidade. Summary aparecia como botão na árvore, mas locator por role não o encontrava; locator pelo texto e Enter funcionou. Servidor temporário encerrou durante regeneração das fixtures; reiniciado, prévia retomada em aba nova sem alterar segurança ou página do usuário. Viewport restaurado; aba de conferência final fechada, aba temporária com erro não conseguiu ser fechada explicitamente pela política da ferramenta e fica sujeita à limpeza automática ao fim do turno. Nenhum formulário POST submetido na prévia estática.

`git diff --check`: código 0. Servidor temporário encerrado ao concluir. data/ preservado e não versionado; nenhuma mutação do banco real, reinício da aplicação em uso ou deploy.

## Pendências e riscos

Auditoria WCAG completa, teste E2E da sessão real e desempenho em massa permanecem nas tarefas gerais. Dados não fazem polling/push: página já aberta requer recarregar. Cópias não são inferidas de edição física; total não é avaliação de mercado. Reiniciar/recarregar a aplicação pelo IntelliJ permite ver o template/CSS atualizado conforme cache da execução.
