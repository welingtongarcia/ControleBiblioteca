# Organização das telas de edições

Reorganizar sem remover dados ou alterar regras do acervo:
- ORG-001: detalhes começam pela capa e identificação (título, número, editora, série, suporte e edição). Envio de capa continua acessível e aberto quando ausente.
- ORG-002: leitura/progresso e avaliação juntos; depois dados editoriais, compra/valores, conteúdo, exemplares, observações e exclusão com confirmação expansível.
- ORG-003: catálogo mantém busca/filtros/ordenação/paginação; cartões exibem identificação, leitura/nota, valor pago e ações. ISBN, variante e preço de capa ficam nos detalhes.
- ORG-004: responsivo, teclado/foco, rótulos pt-BR, CSRF e links/contexto preservados. Sem mudanças no banco ou endpoints.

Tarefas:
- [x] Reorganizar templates e CSS, preservando formulários de estrelas, capa, leitura e exemplares.
- [x] Atualizar testes do catálogo e testar hierarquia, informações preservadas e confirmação de exclusão.
- [x] Executar suíte e verificar layout em larguras desktop/mobile; registrar resultados e limitações.

## Verificação

Templates edicoes/detail.html e list.html reorganizados; dashboard.css adiciona layouts responsivos locais. Capa aberta para inclusão quando ausente e recolhida para alteração quando já existe; atalhos de seção; leitura/estrelas e dados/compra em pares no desktop, empilhados no celular; conteúdo agrupado, exemplares preservados e observações ao final. Exclusão exige abrir a confirmação nativa e acionar o botão final, preservando POST/CSRF e restrições do servidor. ISBN, variante e preço de capa permanecem nos detalhes, não nos cartões.

MinhaGibitecaApplicationTest atualizado para o catálogo simplificado. Novo teste InterfaceIntegrationTest cobre sequência das seções, dados preservados, notas, textos escapados, CSRF, confirmação inicialmente fechada, capa ausente/presente e edição digital sem exemplares físicos.

`./mvnw -q clean verify`: exit 0, 218 testes, 0 falhas/erros/ignorados. Após ajuste semântico do rótulo da capa para não anteceder h1 com h2: `./mvnw -q -Dtest=InterfaceIntegrationTest verify`, exit 0, 9 testes sem falhas/erros/ignorados; artefato recompilado. `git diff --check`: exit 0.

Navegador: prévias sintéticas do MockMvc, servidas temporariamente em localhost, não o banco real. Seis estados (detalhes com/sem capa, digital, detalhes com título longo, catálogo normal e longo) em 320/390/768/1280px: 24 verificações sem overflow horizontal. Inspeção visual do catálogo mobile e leitura/estrelas mobile/desktop; atalhos funcionam; Enter abre/fecha confirmação sem enviar exclusão. Formulários reais continuam cobertos por testes MVC, sem submissões no navegador de prévia. Aba fechada e viewport restaurado.

Sem nova migração ou alteração do acervo; implementação de avaliações preexistente preservada. Aplicação real não reiniciada. Reiniciar no IntelliJ para carregar os templates/CSS atualizados.
