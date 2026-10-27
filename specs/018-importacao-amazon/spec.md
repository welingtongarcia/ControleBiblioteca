# Importação de edição pelo link Amazon — SDD

## Constituição, escopo e esclarecimentos

Aplicam-se a constituição e AGENTS.md: monólito MVC Java 21, pt-BR, CSRF, PRG, dados privados por proprietário e nenhuma persistência sem confirmação. Somente cadastro de nova edição; edição existente, compras, leitura e uploads não mudam. Sem API paga, credenciais Amazon, sessão de cliente, automação de navegador, proxy, solução de CAPTCHA ou contorno de bloqueios.

## Requisitos

- AM-01: aceitar URL HTTPS de produto em `amazon.com.br` ou `www.amazon.com.br`, formatos `/dp/ASIN` e `/gp/product/ASIN`, inclusive prefixo descritivo antes de `/dp/`. Canonicalizar para `https://www.amazon.com.br/dp/ASIN`, remover parâmetros de rastreamento. ASIN de 10 caracteres alfanuméricos não implica ISBN: validar checksum antes de converter ISBN-10 para ISBN-13. Links curtos devem ser expandidos pelo usuário; rejeitar demais domínios, credenciais, portas, caracteres de controle, paths ambíguos/escapados e esquemas diferentes de HTTPS.
- AM-02: consulta de melhor esforço somente à URL canônica fixa, sem redirects, cookies ou autenticação. Timeout de conexão/leitura, HTML até 2 MiB, limite de frequência e cache efêmero. Ao encontrar bloqueio/consentimento/CAPTCHA, resposta não HTML, erro ou ausência de dados, não tentar contornar; buscar ISBN validado nas fontes já existentes (Open Library/Google Books se configurado). Avisar claramente quando só o ISBN foi identificado ou a consulta falhou. Nenhum dado inventado a partir do slug, preço ou ASIN Kindle.
- AM-03: extrair título, ISBN, autores, editora, ano, páginas, formato, sinopse e referência de capa quando disponíveis em JSON-LD ou elementos da ficha pública. Dados são sugestões limitadas e texto simples; fonte e campos ausentes explícitos. Não inferir número editorial, personagens, suporte físico/digital, tipo Livro/Quadrinho, valor pago ou data de compra. Preço anunciado não entra no financeiro.
- AM-04: POST autenticado/CSRF busca e mostra prévia sem alterar formulário nem banco. POST separado aplica somente campos vazios após conferir token aleatório vinculado ao proprietário, válido por 10 min, no máximo 200 prévias na memória. Não aceitar metadados enviados pelo navegador como verdade. Editora/autores só vinculados quando nomes correspondem unicamente a registros da própria conta; sem criar cadastros auxiliares. Nunca substituir ISBN manual ou outros campos preenchidos. Erros de conversão preservados com BindingResult/flash.
- AM-05: referência de capa é apenas link HTTPS em host de imagens Amazon permitido, sem download automático e sem incorporar imagens remotas na interface. Upload de capa existente continua após salvar. HTML recebido nunca é renderizado; links de origem e capa validados, com proteção de nova aba. URLs inválidas e conteúdo de respostas não aparecem em logs/erros. Links com parâmetros são descartados antes do acesso e do armazenamento temporário.
- AM-06: pt-BR, labels, avisos acessíveis e responsividade; cadastro manual sempre disponível. Testes unitários para URLs/SSRF/parser/cache/timeout/fallback e MockMvc+PostgreSQL isolado para CSRF, proprietário, prévia, aplicação e preservação. Usar o ISBN do exemplo `655448244X` → `9786554482448`; não prometer que o exemplo terá metadados se a Amazon bloquear e as fontes não o catalogarem.

## Plano e tarefas

1. Parser de link e DTO de sugestões na aplicação, cliente HTTP restrito e parser HTML jsoup na infraestrutura.
2. Serviço com fallback ISBN, cache de prévias isolado por usuário e expiração; duas rotas no controller existente para preservar binder/rascunho.
3. Seção Importar pelo link da Amazon no formulário de nova edição, prévia, confirmação e referências.
4. Testes de segurança e fluxo; verificação Maven sem sobreposição, conferência da interface e documentação das limitações.

- [ ] AM-01/02/03/05: link, HTTP e extração.
- [ ] AM-04/06: serviço, rotas, interface e testes.
- [ ] Verificação, arquivos, resultados, pendências e riscos registrados.

Análise: nenhuma alteração de esquema/Flyway necessária. API oficial Amazon possui requisitos e restrições de uso; esta consulta pública não garante disponibilidade ou estabilidade. Acesso negado é condição normal e deve resultar em aviso, não evasão. jsoup é apenas parser Java; não introduz runtime frontend. Chaves Google Books existentes não serão criadas nem alteradas. Não haverá indexação de outro site nem importação via Maratona.
