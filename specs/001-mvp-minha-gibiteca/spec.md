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
  - T012: pesquisa literal, sem distinguir maiúsculas, por título, ISBN ou número editorial; filtros combináveis de editora, série, formato e suporte físico/digital (tipo desta fase). Formato sem distinção de maiúsculas. Opções de filtros somente do proprietário; referências alheias retornam 404. Ordenações permitidas: título A–Z/Z–A, cadastro mais recente/mais antigo, sempre com UUID como desempate. Vinte registros por página; página negativa vira zero e ordenação desconhecida usa título A–Z. Parâmetros de filtro/ordenação persistem ao navegar entre páginas; limpar filtros retorna à lista inicial. UUID, suporte ou página malformados retornam 400 em português. Detalhe apresenta nomes de editora/série e associações em consultas de quantidade fixa; exemplares permanecem dependentes de T014.
- **FR-025:** detalhe de edição mostra metadados, capa, obras/criadores associados e seus exemplares próprios.
- **FR-026:** cadastro e edição permitem indicar suporte físico ou digital, preço de capa e valor pago de referência, ambos opcionais em reais. O valor pago de referência pertence ao registro privado da edição, sem representar valores de várias cópias; preços por exemplar serão registrados na aquisição de cada exemplar. Valores ausentes permanecem desconhecidos, zero é válido; rejeitar negativos, mais de duas casas decimais e valores acima de R$ 9.999.999.999,99. Aceitar vírgula ou ponto decimal, sem separador de milhares no formulário. Cadastros novos começam como físicos; registros anteriores recebem suporte não informado para não presumir sua natureza. Listagem mostra o suporte; detalhe e edição preservam os valores, formatados em pt-BR.

### Exemplares e localização

- **FR-030:** usuário pode adicionar, editar e retirar um exemplar de edição autorizada, informando conservação, observações, localização e origem/aquisição quando conhecidos.
  - T014: exemplar físico vinculado a edição própria física ou de suporte ainda não informado; edições digitais não recebem exemplares físicos. Conservação obrigatória com padrão Não informada; opções Novo, Muito bom, Bom, Regular e Ruim. Localização opcional própria; observações de conservação/gerais até 2000 caracteres cada; origem textual até 160, data de aquisição opcional não futura, preço opcional em reais (decimal até R$ 9.999.999.999,99, duas casas, não negativo; zero é gratuito). Nenhum preço é inferido do valor de referência da edição. Edição do exemplar não troca a edição bibliográfica. Remoção exige confirmação e CSRF.
- **FR-031:** múltiplos exemplares da mesma edição são permitidos. O sistema sinaliza possível duplicação, mas não bloqueia o cadastro.
  - Detalhe mostra exemplares próprios em páginas de vinte, conservação, localização completa e dados de aquisição. Contagem acima de um gera aviso, inclusive após edição/remoção. Não bloquear duplicado. Não alterar suporte para digital enquanto houver exemplares físicos; desassociação por remoção explícita primeiro.
- **FR-032:** usuário pode gerenciar localizações hierárquicas de até quatro níveis: cômodo, estante, prateleira e caixa; níveis intermediários são opcionais. Um exemplar referencia uma localização final válida do mesmo proprietário.
  - T013: cadastrar, buscar, listar em páginas de vinte, editar e excluir localizações próprias. Nome obrigatório (até 120 caracteres), tipo obrigatório, pai opcional, observações até 2000 caracteres. Ordem permitida: cômodo → estante → prateleira → caixa, com saltos e raízes de qualquer tipo. Pai deve ter tipo anterior ao filho; mudanças também respeitam tipos dos filhos existentes. Impedir auto-referência e ciclos; nomes sem distinção de maiúsculas são únicos entre irmãos (inclusive raízes). Listagem mostra caminho completo e tipo em pt-BR. Exclusão exige confirmação e bloqueia localizações com filhos, orientando realocação explícita. Proteção de referências de exemplares será integrada em T014, quando essa tabela existir.
- **FR-033:** tentativa de retirar entidade com referências deve ser bloqueada ou exigir desassociação/realocação explícita, conforme regra de cada fluxo.
  - T014: bloquear exclusão de localização com exemplares e de edição com exemplares, com mensagem pt-BR; o banco também protege referências. Exemplar pode ser realocado pelo formulário de edição, inclusive para nenhuma localização.

### Checklist, visão geral e dados portáveis

- **FR-040:** usuário pode manter checklist manual por série, registrando somente números editoriais explicitamente conhecidos/configurados e se os possui. Não há inferência a partir de sequência numérica.
  - T015: cadastrar, editar e excluir itens próprios. Número editorial textual obrigatório, até 40 caracteres, preservando zeros, letras e especiais; único por série/proprietário após trim e comparação sem distinguir maiúsculas. Rótulo opcional até 180 e observações até 2000. Padrão Não possuo; estado Possuo é alterado manualmente no formulário, independente de exemplares ou suporte. Edição não troca a série do item. Remoção exige confirmação e CSRF. Lista de vinte por página em ordem de cadastro com UUID de desempate; página negativa vira zero. Série com checklist não é apagável até remoção explícita dos itens.
- **FR-041:** tela de faltantes apresenta somente entradas de checklist explicitamente marcadas como não possuídas; edições com exemplares podem ser usadas como evidência visual, mas não criam itens automaticamente.
  - T015: tela por série, paginada, filtra somente owned=false no banco. Checklist vazio mostra orientação para cadastrar números conhecidos, nunca gera sequências; marcar Possuo retira o item dos faltantes, desmarcar o devolve. Links na lista de séries e navegação entre checklist completo/faltantes. Evidência visual de exemplares fica fora desta etapa.
- **FR-050:** dashboard apresenta totais reais do proprietário: exemplares, edições distintas, séries, distribuição por editora e formato/tipo, aquisições recentes disponíveis e possíveis duplicados.
  - Correção T025 aprovada: total principal soma reference_paid_price de cada edição cadastrada uma única vez, incluindo físicas, digitais e suporte não informado, independentemente de exemplares. Mostrar contagens de edições físicas/digitais e total cadastrado; suporte desconhecido fica explícito. Exemplares físicos e edições com exemplares permanecem separados. Soma acquisition_price continua em indicador secundário claramente separado, nunca adicionada ao total principal. Preço de capa não entra em nenhuma soma. Cobertura/ausência/zero definidos por cada origem; vazio zero, todos os valores desconhecidos Não informado. Dados atuais na próxima leitura após cadastro/edição de edição ou exemplar. T025 substitui a semântica principal de T024 abaixo; não altera registros nem infere propriedade/cópias.
  - T024 entrega o resumo inicial: quantidade de exemplares físicos, edições cadastradas (incluindo digitais e sem exemplares), edições com exemplares e total pago registrado nos exemplares. Somar acquisition_price em decimal, inclusive zero, sem multiplicar joins nem usar preço de capa/reference_paid_price. Valores desconhecidos não são inferidos: mostrar quantidade sem preço e avisar que o total é parcial. Coleção vazia mostra zero; exemplares todos sem preço mostram total não informado. Não representa avaliação de mercado nem gastos digitais. Dados somente do usuário da sessão e atualizados a cada acesso a / ou /dashboard. Distribuições, séries, recentes e duplicados de T016 permanecem pendentes.
- **FR-051:** página inicial autenticada oferece navegação clara pelos módulos entregues, pesquisa no catálogo, atalho para nova edição e saída segura da sessão; textos em pt-BR, layout responsivo, foco visível e link para pular a navegação. Nesta melhoria visual, não exibir estatísticas ou atividades fictícias; agregações reais continuam em T016. Ilustração decorativa não representa registros da coleção.
- **FR-052:** telas privadas compartilham menu dos módulos, marca sempre ligada ao início, saída POST/CSRF, seção ativa e trilha de navegação. Cadastros têm voltar/cancelar explícitos: exemplar retorna à edição, item de checklist ao checklist, demais cadastros à lista; edição bibliográfica existente pode retornar ao detalhe. Preservar consulta/página/filtros da lista ao abrir registros/formulários e voltar ou salvar; checklist/faltantes preservam modo e paginação na edição de itens. Contexto de retorno aceita somente rotas locais permitidas da mesma seção e parâmetros conhecidos, nunca URLs externas, cabeçalhos Referer ou history.back(). Links do menu abrem a lista principal sem filtros anteriores; limpar busca permanece explícito. Telas de erro oferecem retorno à seção ou início, sem expor nomes de recursos alheios. Login permanece separado.
- **FR-060:** usuário autenticado exporta CSV UTF-8 de suas edições e exemplares, com cabeçalho estável, sem dados de outros usuários. O download é auditado em log sem conteúdo sensível.

## Cenários de aceitação prioritários

- **AC-001 / FR-003:** dado o usuário A autenticado, quando acessa a URL de uma edição de B, então recebe 404 e nenhum título/metadado de B é renderizado.
- **AC-002 / FR-020/021:** dado um formulário de edição com ISBN vazio e número `1A`, quando válido, então a edição é persistida e mostrada após redirect.
- **AC-003 / FR-031:** dadas duas inserções autorizadas para a mesma edição, quando a segunda é salva, então ambas aparecem e há aviso de possível duplicata.
- **AC-004 / FR-023:** dado arquivo de imagem permitido, quando enviado para uma edição do proprietário, então recebe nome não previsível, miniatura é exibida e o banco mantém apenas metadados/referência.
- **AC-005 / FR-024:** dado catálogo com volume acima de uma página, quando há filtro e ordenação, então o resultado é paginado no servidor e preserva os parâmetros.
  - Evidência T012: mais de vinte edições correspondentes e dados de outro usuário; testar filtros combinados, pesquisa literal contendo `%`/`_`, ISBN/numeração, ordenação com desempate e links que mantêm filtros. Detalhe mostra nomes bibliográficos próprios e acesso alheio retorna 404. Não confundir teste funcional de paginação com aferição p95 de NFR-003.
- **AC-006 / FR-040/041:** dado checklist configurado com `0`, `1A` e `Especial`, quando `1A` está faltante, então somente `1A` aparece como lacuna; nenhum número inventado é listado.
  - T015: exercitar cadastro/edição/remoção, número obrigatório/limites/duplicados normalizados, owned manual mesmo após cadastrar/remover exemplar, isolamento de séries/itens, paginação, confirmação/CSRF e bloqueio de exclusão da série referenciada.
- **AC-007 / FR-050:** dadas alterações no banco, quando o dashboard é aberto, então seus totais correspondem às consultas persistidas, sem valores de demonstração.
  - T025: edição física sem exemplar e valor pago registrado já aparece no total e na contagem física; digital entra na contagem e soma; suporte nulo identificado. Exemplares não multiplicam referências. POST de edição muda valor/suporte na próxima leitura, preço removido altera cobertura; owner isolado; referências e aquisições separadas, sem fallback entre fontes.
  - T024: testar vazio, todos desconhecidos, gratuito, preços distintos da mesma edição, edição sem exemplar/digital, preço de referência não somado, isolamento entre usuários e atualização após editar/remover exemplar. Formatação em reais pt-BR, aviso de cobertura e acesso anônimo bloqueado. Após cada POST válido de cadastro/edição de exemplar, o próximo GET da inicial deve refletir a transação concluída, sem recálculo manual ou valores em cache. POST inválido não muda os totais. Página já aberta requer recarregamento; atualização em tempo real entre abas não faz parte deste fluxo MVC.
- **AC-012 / FR-051:** usuário autenticado acessa / ou /dashboard e encontra busca GET /edicoes, nova edição, catálogo, séries/checklist, localizações, editoras, obras e criadores. Saída usa POST /logout com CSRF. Navegação e conteúdo permanecem utilizáveis em telas estreitas; visitante anônimo segue redirecionado ao login. Não são apresentados totais fictícios.
- **AC-013 / FR-052:** percorrer lista → detalhe/cadastro → edição/exemplar e séries → checklist/faltantes → edição/remoção de item; menu e seção ativa consistentes, trilhas e cancelamentos apontam a pais autorizados. Busca/filtros/página de origem sobrevivem ao retorno e PRG, inclusive erros de validação; modo faltantes é preservado. returnTo externo, outro módulo ou rota arbitrária é ignorado; usuário alheio continua recebendo 404. Conferir navegação por teclado e telas estreitas.
- **AC-008 / FR-060:** dado o usuário A, quando exporta CSV, então o arquivo contém somente registros A, possui cabeçalho e codificação UTF-8.
- **AC-009 / FR-026:** cadastrar edição digital com preço de capa `29,90` e valor pago `19,50`, verificar persistência decimal e detalhe em reais; editar para físico com valores vazios preserva ausência, zero continua zero. Entradas inválidas retornam erros nos campos e dados alheios continuam retornando 404. Migration preserva dados anteriores com suporte e preços não informados.
- **AC-010 / FR-032/033:** criar cômodo → estante → prateleira → caixa e caminho com salto de nível; editar nome/pai preservando hierarquia. Rejeitar pai alheio, auto-referência, ciclos, ordem invertida, mudança de tipo incompatível com filhos e nome repetido entre irmãos. Bloquear exclusão com filhos; excluir folha somente após confirmação. Listagem paginada e opções de pai não expõem localizações alheias.
- **AC-011 / FR-030/031/033:** cadastrar duas cópias autorizadas com valores distintos, exibir ambas e aviso; editar conservação/localização/aquisição sem alterar edição. Bloquear referência alheia, edição digital, preço inválido, data futura, exclusão de localização/edição referenciada e mudança para digital. Remover exemplar só com confirmação/CSRF; referência torna-se livre depois da última remoção.

## Requisitos não funcionais

- **NFR-001:** idioma pt-BR, HTML5 semântico, navegação por teclado, foco visível, contraste adequado e erros associados aos campos; alvo WCAG 2.2 AA nos fluxos implementados.
- **NFR-002:** todas as listas volumosas usam paginação/filtragem no banco; consultas críticas evitam N+1 e possuem índices documentados.
- **NFR-003:** em ambiente de referência, páginas de lista/detalhe devem ter p95 de aplicação abaixo de 500 ms com 10.000 exemplares do usuário, excluindo transferência de imagens. A meta será medida na fase de testes de desempenho, não alegada antecipadamente.
- **NFR-004:** CSRF em formulários mutáveis; Bean Validation no servidor; SQL parametrizado via JPA; saída Thymeleaf escapada; cabeçalhos de segurança e limites de upload configurados.
- **NFR-005:** PostgreSQL e imagens persistem em volumes/armazenamento durável; segredos entram por variáveis de ambiente; backups/restauração são documentados e testados antes da preparação de produção.
- **NFR-006:** build único Maven, sem Node obrigatório: `./mvnw clean verify` gera o JAR executável.

## Fora do escopo do MVP

Reconhecimento por IA, scanner/barcode, consulta automática de ISBN, avaliação automática de preços/valorização, empréstimos, recursos sociais, wishlist avançada, controle financeiro e recuperação de senha por e-mail. O registro manual de preço de capa e valor pago está incluído conforme FR-026.
