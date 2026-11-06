# Importação de edição pelo link Amazon — SDD

## Constituição, escopo e esclarecimentos

Aplicam-se a constituição e AGENTS.md: monólito MVC Java 21, pt-BR, CSRF, PRG, dados privados por proprietário e nenhuma persistência sem confirmação. A importação é exclusiva do cadastro de nova edição. A validação de conteúdo e dimensões da capa é compartilhada com o envio manual. Sem API paga, credenciais Amazon, sessão de cliente, automação de navegador, proxy, solução de CAPTCHA ou contorno de bloqueios.

## Requisitos

- AM-01: aceitar URL HTTPS de produto em `amazon.com.br` ou `www.amazon.com.br`, formatos `/dp/ASIN` e `/gp/product/ASIN`, inclusive prefixo descritivo antes de `/dp/`. Canonicalizar para `https://www.amazon.com.br/dp/ASIN`, remover parâmetros de rastreamento. ASIN de 10 caracteres alfanuméricos não implica ISBN: validar checksum antes de converter ISBN-10 para ISBN-13. Links curtos devem ser expandidos pelo usuário; rejeitar demais domínios, credenciais, portas, caracteres de controle, paths ambíguos/escapados e esquemas diferentes de HTTPS.
- AM-02: consulta de melhor esforço somente à URL canônica fixa, sem redirects, cookies ou autenticação. Timeout de conexão/leitura, HTML até 2 MiB, limite de frequência e cache efêmero. Ao encontrar bloqueio/consentimento/CAPTCHA, resposta não HTML, erro ou ausência de dados, não tentar contornar; buscar ISBN validado nas fontes já existentes (Open Library/Google Books se configurado). Avisar claramente quando só o ISBN foi identificado ou a consulta falhou. Nenhum dado inventado a partir do slug, preço ou ASIN Kindle.
- AM-03: extrair título, ISBN, autores, editora, ano, páginas, formato, sinopse e referência de capa quando disponíveis em JSON-LD ou elementos da ficha pública. Dados são sugestões limitadas e texto simples; fonte e campos ausentes explícitos. Não inferir número editorial, personagens, suporte físico/digital, tipo Livro/Quadrinho, valor pago ou data de compra. Preço anunciado não entra no financeiro.
- AM-04: POST autenticado/CSRF busca e mostra prévia sem alterar formulário nem banco. POST separado aplica somente campos vazios após conferir token aleatório vinculado ao proprietário, válido por 10 min, no máximo 200 prévias na memória. Não aceitar metadados enviados pelo navegador como verdade. Editora/autores só vinculados quando nomes correspondem unicamente a registros da própria conta; sem criar cadastros auxiliares. Nunca substituir ISBN manual ou outros campos preenchidos. Erros de conversão preservados com BindingResult/flash.
- AM-05: referência de capa é um link HTTPS em host de imagens Amazon permitido. HTML recebido nunca é renderizado; links de origem e capa validados, com proteção de nova aba. URLs inválidas e conteúdo de respostas não aparecem em logs/erros. Links com parâmetros são descartados antes do acesso e do armazenamento temporário.
- AM-07: oferecer “Usar capa da Amazon” na prévia quando houver capa, desmarcada por padrão. Preservar a escolha no rascunho. No cadastro válido e confirmado, resolver a referência pelo token temporário do proprietário e baixar somente JPEG/PNG até 5 MiB, sem redirecionamentos e com limites de tempo. Validar formato real e dimensões antes de decodificar. Usar o armazenamento de capas existente. Se a prévia expirar, o download falhar ou a imagem for inválida, manter a edição e avisar na página de detalhes, onde é possível enviar a capa manualmente.
- AM-06: pt-BR, labels, avisos acessíveis e responsividade; cadastro manual sempre disponível. Testes unitários para URLs/SSRF/parser/cache/timeout/fallback e MockMvc+PostgreSQL isolado para CSRF, proprietário, prévia, aplicação e preservação. Usar o ISBN do exemplo `655448244X` → `9786554482448`; não prometer que o exemplo terá metadados se a Amazon bloquear e as fontes não o catalogarem.

## Plano e tarefas

- [x] AM-07: preservar opção no formulário, validar token ao salvar e reutilizar o serviço de capas com download limitado. Verificar fluxo MVC, conteúdo rejeitado e falha sem perder a edição. Sem alteração de esquema.

1. Parser de link e DTO de sugestões na aplicação, cliente HTTP restrito e parser HTML jsoup na infraestrutura.
2. Serviço com fallback ISBN, cache de prévias isolado por usuário e expiração; duas rotas no controller existente para preservar binder/rascunho.
3. Seção Importar pelo link da Amazon no formulário de nova edição, prévia, confirmação e referências.
4. Testes de segurança e fluxo; verificação Maven sem sobreposição, conferência da interface e documentação das limitações.

- [x] AM-01/02/03/05: link, HTTP e extração.
- [x] AM-04/06: serviço, rotas, interface e testes.
- [x] Verificação, arquivos, resultados, pendências e riscos registrados.

Análise: nenhuma alteração de esquema/Flyway necessária. API oficial Amazon possui requisitos e restrições de uso; esta consulta pública não garante disponibilidade ou estabilidade. Acesso negado é condição normal e deve resultar em aviso, não evasão. jsoup é apenas parser Java; não introduz runtime frontend. Chaves Google Books existentes não serão criadas nem alteradas. Não haverá indexação de outro site nem importação via Maratona.

## Verificação da entrega inicial — 27/10/2026

- `JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64 ./mvnw -q clean verify`: saída 0; 34 suítes, **281 testes**, zero falhas/erros/ignorados. Empacotamento JAR concluído. Novos testes: 6 de URLs/cache/fallback, 6 de parser/HTTP/gzip/limites e 2 fluxos MockMvc com PostgreSQL isolado. Verificados CSRF/autenticação, token de outro proprietário, expiração, consulta sem persistir, confirmação explícita, edição de campos manuais, dinheiro pt-BR, associações da própria conta, XSS, páginas inválidas, redirects e resposta não HTML.
- Consulta real do cliente Java 21 ao produto `655448244X`: título Thorgal: Série Clássica (Vol. 1 de 4), ISBN `9786554482448`, 420 páginas, ano 2025, editora Pipoca e Nanquim, nomes de criadores, sinopse e referência de capa encontrados. Sem login Amazon, imagens baixadas ou dados pessoais enviados. O resultado real não foi gravado no acervo do usuário.
- Prévia sintética renderizada por MockMvc e inspecionada no navegador: seção de importação, prévia e botão de aplicação presentes; larguras 320/390/768/1280 px sem transbordamento horizontal. Valores sintéticos escapados corretamente. Servidor/aba temporários encerrados após a inspeção.
- `git diff --check`: saída 0. Teste de interface com integração OneDrive desativada agora declara essa configuração explicitamente, sem depender do `.env` local do usuário.

Arquivos: `AmazonProductLink`, `AmazonMetadata`, `AmazonPageProvider`, `AmazonImportService`, `AmazonPageClient`, `AmazonImportController`; binder de formulário extraído para `EditionFormBinding` e compartilhado com `EditionController`, sem alterar campos permitidos ou conversão monetária; template `edicoes/form.html`; dependência jsoup 1.23.2; testes acima; README e `docs/importacao-amazon.md`. Nenhuma migração ou modificação de `.env` foi necessária.

Evidências temporárias: `/tmp/amazon-verify.log`, relatórios `target/surefire-reports/`, prévia `target/ui-review/amazon-preview.html`. Rodadas intermediárias corrigiram uma asserção de teste que não compilava e a limpeza de FKs do banco de teste; somente a rodada limpa final foi considerada concluída.

Pendências e riscos: a disponibilidade futura depende da Amazon e da cobertura das fontes bibliográficas; links curtos/outros países não são suportados; ISBN ausente/ASIN Kindle não será inventado. Capa é referência, sem importação automática. Autores/editora sem correspondência local exigem cadastro manual. A sinopse preenche observações apenas quando vazias. A instância do usuário não foi reiniciada nem foi realizada publicação no GitHub; reiniciar pelo IntelliJ para carregar as classes novas. Não se afirma suporte a todos os produtos ou layouts Amazon.

## Verificação da opção de capa — 06/11/2026

- `./mvnw -q clean verify` com Java 21 e `git diff --check` retornaram código 0. Foram 282 testes em 34 suítes, com zero falhas, erros ou testes ignorados.
- MockMvc e PostgreSQL isolado verificaram prévia com opção desmarcada, seleção preservada, validação antes do download, token de outro proprietário, imagem salva e servida pela rota de capa e edição preservada quando o formato real diverge do declarado. O cliente foi testado com host indevido, redirecionamento, resposta HTML e tamanho excedido.
- O cliente baixou e decodificou a capa JPEG de 1159 × 1500 pixels do produto `655448244X` sem gravá-la no acervo. O download depende da disponibilidade da imagem e da validade da prévia.
- Arquivos principais: `AmazonCoverImportService`, `AmazonCoverProvider`, `AmazonCoverClient`, `EditionCoverService`, formulário e controllers de cadastro, testes de importação e guia `docs/importacao-amazon.md`.
