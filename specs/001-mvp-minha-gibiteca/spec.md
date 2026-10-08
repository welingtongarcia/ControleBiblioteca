# Especificação funcional — MVP 1.0 Minha Gibiteca

**Status:** proposta pronta para planejamento  
**Rastreio:** FR-* (funcional), NFR-* (não funcional), AC-* (aceite)

## Objetivo e atores

Permitir que um colecionador autenticado catalogue, encontre e administre até 10.000 exemplares físicos de HQs sem expor seu acervo a outros usuários. O MVP é inicialmente privado, mas o isolamento de dados é obrigatório desde já. O ator primário é o **colecionador**; o sistema terá a noção de **administrador** somente para administração futura, sem telas administrativas no MVP.

## Vocabulário obrigatório

- **Obra:** conteúdo criativo/história, reutilizável entre publicações.
- **Série editorial:** publicação de uma editora com periodicidade/ordenação próprias.
- **Edição:** publicação bibliográfica concreta; possui número editorial textual, formato, variante, capa e metadados.
- **Exemplar físico:** unidade possuída por um usuário; possui estado, localização e aquisição próprios.

Uma edição contém zero ou mais obras e uma obra pode estar em várias edições. Uma edição pode ter muitos exemplares do mesmo proprietário. A ausência de uma obra detalhada não impede cadastrar a edição no MVP.

## Requisitos funcionais

### Acesso e isolamento

- **FR-001:** usuário não autenticado é direcionado ao login ao solicitar rota privada.
- **FR-002:** usuário autenticado pode encerrar sua sessão; sessão e cookies seguem a configuração segura do ambiente.
- **FR-003:** toda edição, exemplar, série, editora e localização visualizada ou alterada deve estar autorizada para o proprietário atual. Identificadores adivinhados de outro usuário retornam 404, sem revelar existência.
- **FR-004:** a criação inicial de usuário administrador/local é feita por mecanismo de bootstrap documentado; recuperação de senha fica fora do MVP até existir e-mail operacional.

### Editoras e séries

- **FR-010:** usuário pode criar, listar, pesquisar, editar e excluir editoras próprias com nome obrigatório, país opcional e observações opcionais.
- **FR-011:** usuário pode criar, listar, pesquisar, editar e excluir séries próprias com nome obrigatório, editora opcional, período/status/observações opcionais e ordenação editorial não presumida.
- **FR-012:** uma editora/série com dependências não pode ser excluída silenciosamente; a tela informa a dependência e oferece apenas fluxo seguro definido no plano.

### Edições e catálogo

- **FR-020:** usuário pode criar, consultar, editar e solicitar exclusão de edição própria com título obrigatório, número editorial textual opcional, editora/série opcionais, ano parcial opcional, ISBN opcional, formato, variante e metadados essenciais opcionais.
- **FR-021:** o mesmo ISBN pode existir em mais de uma edição; ISBN em branco é válido. Uma combinação de título, série, número e variante pode gerar aviso de possível duplicidade, nunca bloqueio automático.
- **FR-022:** usuário pode associar zero ou mais obras e criadores a uma edição. Uma obra pode ocorrer em várias edições.
- **FR-023:** usuário pode enviar e substituir uma capa de edição autorizada; o sistema valida arquivo e exibe miniatura sem armazenar binário no PostgreSQL.
- **FR-024:** catálogo lista edições próprias com paginação no servidor, busca textual, filtros de editora/série/formato/tipo e ordenação previsível.
- **FR-025:** detalhe de edição mostra metadados, capa, obras/criadores associados e seus exemplares próprios.

### Exemplares e localização

- **FR-030:** usuário pode adicionar, editar e retirar um exemplar de edição autorizada, informando conservação, observações, localização e origem/aquisição quando conhecidos.
- **FR-031:** múltiplos exemplares da mesma edição são permitidos. O sistema sinaliza possível duplicação, mas não bloqueia o cadastro.
- **FR-032:** usuário pode gerenciar localizações hierárquicas de até quatro níveis: cômodo, estante, prateleira e caixa; níveis intermediários são opcionais. Um exemplar referencia uma localização final válida do mesmo proprietário.
- **FR-033:** tentativa de retirar entidade com referências deve ser bloqueada ou exigir desassociação/realocação explícita, conforme regra de cada fluxo.

### Checklist, visão geral e dados portáveis

- **FR-040:** usuário pode manter checklist manual por série, registrando somente números editoriais explicitamente conhecidos/configurados e se os possui. Não há inferência a partir de sequência numérica.
- **FR-041:** tela de faltantes apresenta somente entradas de checklist explicitamente marcadas como não possuídas; edições com exemplares podem ser usadas como evidência visual, mas não criam itens automaticamente.
- **FR-050:** dashboard apresenta totais reais do proprietário: exemplares, edições distintas, séries, distribuição por editora e formato/tipo, aquisições recentes disponíveis e possíveis duplicados.
- **FR-060:** usuário autenticado exporta CSV UTF-8 de suas edições e exemplares, com cabeçalho estável, sem dados de outros usuários. O download é auditado em log sem conteúdo sensível.

## Cenários de aceitação prioritários

- **AC-001 / FR-003:** dado o usuário A autenticado, quando acessa a URL de uma edição de B, então recebe 404 e nenhum título/metadado de B é renderizado.
- **AC-002 / FR-020/021:** dado um formulário de edição com ISBN vazio e número `1A`, quando válido, então a edição é persistida e mostrada após redirect.
- **AC-003 / FR-031:** dadas duas inserções autorizadas para a mesma edição, quando a segunda é salva, então ambas aparecem e há aviso de possível duplicata.
- **AC-004 / FR-023:** dado arquivo de imagem permitido, quando enviado para uma edição do proprietário, então recebe nome não previsível, miniatura é exibida e o banco mantém apenas metadados/referência.
- **AC-005 / FR-024:** dado catálogo com volume acima de uma página, quando há filtro e ordenação, então o resultado é paginado no servidor e preserva os parâmetros.
- **AC-006 / FR-040/041:** dado checklist configurado com `0`, `1A` e `Especial`, quando `1A` está faltante, então somente `1A` aparece como lacuna; nenhum número inventado é listado.
- **AC-007 / FR-050:** dadas alterações no banco, quando o dashboard é aberto, então seus totais correspondem às consultas persistidas, sem valores de demonstração.
- **AC-008 / FR-060:** dado o usuário A, quando exporta CSV, então o arquivo contém somente registros A, possui cabeçalho e codificação UTF-8.

## Requisitos não funcionais

- **NFR-001:** idioma pt-BR, HTML5 semântico, navegação por teclado, foco visível, contraste adequado e erros associados aos campos; alvo WCAG 2.2 AA nos fluxos implementados.
- **NFR-002:** todas as listas volumosas usam paginação/filtragem no banco; consultas críticas evitam N+1 e possuem índices documentados.
- **NFR-003:** em ambiente de referência, páginas de lista/detalhe devem ter p95 de aplicação abaixo de 500 ms com 10.000 exemplares do usuário, excluindo transferência de imagens. A meta será medida na fase de testes de desempenho, não alegada antecipadamente.
- **NFR-004:** CSRF em formulários mutáveis; Bean Validation no servidor; SQL parametrizado via JPA; saída Thymeleaf escapada; cabeçalhos de segurança e limites de upload configurados.
- **NFR-005:** PostgreSQL e imagens persistem em volumes/armazenamento durável; segredos entram por variáveis de ambiente; backups/restauração são documentados e testados antes da preparação de produção.
- **NFR-006:** build único Maven, sem Node obrigatório: `./mvnw clean verify` gera o JAR executável.

## Fora do escopo do MVP

Reconhecimento por IA, scanner/barcode, consulta automática de ISBN, preços/valorização, empréstimos, recursos sociais, wishlist avançada, controle financeiro e recuperação de senha por e-mail. Constam do roadmap, sem endpoints ou controles simulados.
