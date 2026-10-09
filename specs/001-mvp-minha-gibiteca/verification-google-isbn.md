# Verificação — segunda fonte ISBN

Data: 2026-10-09. T028, FR-029, AC-016.

## Planejamento e mudanças

Constituição/AGENTS revistos. Spec → clarificação comunicada → plan → tasks → analysis atualizados antes do código. Google Books consulta apenas após ausência da Open Library; fonte primária com erro não dispara a segunda. Chave por ambiente, sem criar conta/serviço/cota. Sem schema/migração ou frontend separado. T028 altera o limite histórico de T027 para uma nova busca por segundo/processo, no máximo uma chamada por fonte.

- CompositeIsbnProvider @Primary seleciona fonte em ordem; IsbnLookupService mantém cache/taxa e adiciona conversão ISBN-10/13.
- GoogleBooksIsbnProvider/IsbnClientConfiguration: host HTTPS fixo, query isbn, maxResults=5, chave mantida no servidor, sem redirects; conexão 3s/leitura 5s e resposta 256 KiB por fonte. Aceita somente ISBN equivalente nos identificadores; metadados tipados/limitados, nenhuma URL da resposta consumida. Mensagens seguras para chave ausente/cota/erros, sem causa/segredos.
- IsbnMetadata controla fonte tipada e links fixos; EditionController informa ausência nas duas somente quando ambas foram consultadas. Formulário mostra a fonte real e explica envio/fallback, revisão e cadastro manual. Associações/gravação/dados próprios mantidos.
- application.yml, .env.example, docker-compose.yml e docs/installation.md documentam/repassam GOOGLE_BOOKS_API_KEY sem valor real versionado. .env do usuário não foi alterado.
- GoogleBooksIsbnProviderTest, CompositeIsbnProviderTest e teste MVC adicional cobrem novo comportamento.

## Comandos e evidências

`./mvnw test -q -Dtest=GoogleBooksIsbnProviderTest,CompositeIsbnProviderTest,IsbnLookupTest,OpenLibraryIsbnProviderTest`: código 0; 24 testes específicos.

`./mvnw clean verify -q`: código 0, Java 21, **110 testes sem falhas, erros ou ignorados**: 39 MVC/integrados, 22 navegação, 10 checklist, 8 exemplares, 4 leitura, 3 resumo, 4 serviço ISBN, 6 Open Library, 9 Google Books, 5 composição. PostgreSQL 16 em Testcontainers, migrações V1–V13 e upgrade verificados. Testes HTTP determinísticos, sem chave real/internet.

Cobertura: primária encontrada/falha sem Google, ausência com fallback e ordem, cache composto de sucesso/ausência e falha não cacheada; ISBN-10 equivalente (incluindo X), 979 e rejeição de inválidos antes da rede. JSON com primeiro resultado errado ignorado, ISBN ausente/divergente/inválido, opcionais ausentes, URL maliciosa ignorada, ausência explícita, chave vazia sem HTTP, JSON inválido/oversized, 429 e timeout sem vazamento de chave/corpo/causa. MVC renderiza fonte/link Google corretos, escapa script, mantém preço/observações e não persiste edição na consulta. Testes anteriores seguem cobrindo PRG/CSRF/proprietário/salvar e preservação dos demais campos.

`docker compose --env-file .env.example config --quiet`: código 0; valida estrutura, não é deploy. `git diff --check`: código 0.

Smoke externo sem chave com ISBN 9786559601325: endpoint Google respondeu erro HTTP 429, quota zero. Curl retornou código 0 porque não foi usado --fail, portanto esse código não indica sucesso da API. Não foram contornadas cotas ou tentadas URLs alternativas. Documentação primária consultada: https://developers.google.com/books/docs/v1/using e https://developers.google.com/books/docs/v1/reference/volumes/list; exige identificação via chave/token para dados públicos. Não há evidência de metadados reais desse ISBN no Google Books.

Prévia descartável do HTML efetivamente renderizado por MockMvc em target/ui-google-isbn, com metadados simulados: desktop padrão e móvel 320×780 conferidos em capturas completas, documento 320/320 sem rolagem horizontal; fonte Google Books, texto escapado, campos e revisão legíveis. Pular para o conteúdo por teclado focou main-content. Nenhum POST na prévia estática. Viewport restaurado, aba fechada, servidor temporário encerrado. data/ preservado; nenhum reinício/deploy ou mutação do banco real.

## Pendências e riscos

Implementação e testes concluídos. Ativação real depende de GOOGLE_BOOKS_API_KEY e cota válida configuradas pelo administrador; não pedir chave no chat. Reiniciar IntelliJ após configuração. Não foi executado E2E na sessão real, nem comprovada cobertura do ISBN solicitado. Capa continua manual, sem criação automática de auxiliares. Busca sequencial pode consumir até os timeouts de ambas as fontes; timeout de leitura não é orçamento total. Cache/taxa locais ao processo. Serviço externo pode não conter a HQ; auditoria WCAG completa/desempenho e tarefas gerais permanecem pendentes.
