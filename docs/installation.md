# Instalação e execução local

## Pré-requisitos

Java 21 e Docker Compose v2. O Maven Wrapper está versionado, portanto Maven instalado é opcional após a primeira execução.

## Execução com Docker

1. Copie `.env.example` para `.env`.
2. Substitua ambos os valores de senha por uma senha local forte e mantenha `POSTGRES_PASSWORD` e `DB_PASSWORD` iguais. Defina também o e-mail e uma senha de ao menos 12 caracteres para o administrador inicial.
3. Execute `docker compose up --build`.
4. Consulte `http://localhost:8080/actuator/health/readiness` para verificar disponibilidade.

No primeiro início em um banco vazio, o administrador é criado somente pelas variáveis `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD`. Após existir algum usuário, essas variáveis são ignoradas. Nunca versione o `.env`.

## Verificação

Execute `./mvnw clean verify`. O conjunto sobe PostgreSQL via Testcontainers, aplica Flyway em banco vazio e verifica a página de login com MockMvc.

## Backup e migração

Para levar a coleção a outra máquina ou sistema operacional, use **Painéis e análises → Backup e migração**. O [guia de backup e restauração](backup-migracao.md) explica o ZIP pessoal, a preparação da nova instalação e o cuidado com volumes de capas existentes no Docker.

## Execução pelo IntelliJ IDEA

O projeto carrega automaticamente o arquivo local `.env` ao executar `MinhaGibitecaApplication`. Para usar o banco local pelo IntelliJ, suba apenas `postgres` com Docker Compose; o serviço expõe a porta somente em `127.0.0.1:5432`. Pare o serviço `app` do Compose para liberar a porta 8080 e então execute a classe principal pelo IntelliJ.

## Consulta por ISBN

### Visibilidade do menu lateral

Nas páginas privadas, **Ocultar menu** esconde toda a barra lateral e libera espaço para o conteúdo. O botão muda para **Mostrar menu** e permanece disponível fora da barra. A escolha é lembrada neste navegador entre páginas e recarregamentos. Sem JavaScript, o menu permanece visível; se o armazenamento local estiver bloqueado, o botão funciona, mas a escolha não persiste. Os grupos Sua coleção e Dados do catálogo continuam recolhíveis independentemente.

### Busca gratuita pelo título

Na tela **Nova edição**, use **Buscar dados pelo título** quando não tiver ISBN ou a consulta por ISBN não encontrar a HQ. Digite o título no campo específico, escolha Open Library ou Google Books e clique em **Pesquisar pelo título**. A pesquisa exibe até cinco candidatos; consulte o link da fonte e confira idioma, editora e ano. Clique em **Aplicar dados desta edição** para preencher somente campos vazios; **Salvar edição** continua sendo necessário.

Open Library não exige chave. Google Books utiliza a mesma `GOOGLE_BOOKS_API_KEY` gratuita configurada para ISBN e está sujeito a cotas e termos do provedor. Não há integração com fontes pagas, contratação automática ou importação em massa. Metabooks e GCD não foram incluídos; Comic Vine também não foi incluído nesta entrega devido às restrições de reutilização de seus termos.

Somente o título digitado para pesquisa é enviado à fonte escolhida. Dados financeiros, data de compra, suporte, leitura e observações não são enviados nem alterados. Editora/autores só são associados a registros já cadastrados pelo usuário, por nome único; capas continuam por upload. Resultados e seleção pertencem ao usuário autenticado, ficam disponíveis por dez minutos e podem expirar antes se o cache atingir 200 consultas. Seleção expirada exige pesquisar novamente. A aplicação da seleção não faz nova chamada HTTP nem salva no banco.

A pesquisa por título permite uma nova consulta por segundo por processo, com cache de resultados. A busca ISBN tem seu limite próprio. Cada requisição externa mantém timeout de conexão/leitura 3/5 segundos e limite de resposta 256 KiB. Não há garantia de cobertura de todas as HQs brasileiras, e nenhuma fonte indisponível bloqueia o cadastro manual.

No cadastro de edição, informe ISBN-10/13 e use Buscar pelo ISBN. A Open Library recebe somente esse identificador; se retornar ausência, o Google Books é consultado quando configurado. Nenhum título privado, valor ou estado de leitura é enviado. A consulta preenche campos vazios de título/ano e associa editora/criadores já cadastrados pelo proprietário apenas quando o nome corresponde de forma única. Sugestões identificam a fonte e continuam revisáveis; é necessário Salvar para persistir. Capa aparece somente como indicação/link da fonte, sem importação automática; use o upload após salvar.

Sem resultado, falha de rede ou ISBN inválido, continue o cadastro manual. O ISBN manual continua opcional e não passa a exigir checksum para salvar. Open Library não exige chave. Para habilitar a segunda fonte, configure `GOOGLE_BOOKS_API_KEY` no ambiente ou `.env` local e reinicie a aplicação. No IntelliJ, esse arquivo é importado pelo projeto; Docker Compose repassa a variável ao serviço app. Não envie a chave pelo chat, não a versione e restrinja-a à Books API no Google Cloud. A criação/configuração da chave fica a cargo do administrador conforme a [documentação oficial do Google Books](https://developers.google.com/books/docs/v1/using#APIKey). Sem chave, a interface informa consulta incompleta; cota/403/429 ou falha de rede não significam ISBN inexistente. Não há garantia de que a segunda fonte tenha todas as HQs.

O serviço usa HTTPS, timeouts de conexão/leitura de 3/5 segundos por fonte, limite de resposta de 256 KiB por fonte, cache público de dez minutos/200 ISBNs e no máximo uma nova busca por segundo por processo (até uma chamada por fonte); não use para importações em massa. Somente ausência da Open Library aciona Google Books; falha da primeira fonte permite tentar depois ou continuar manualmente. Google só aceita identificador ISBN correspondente, inclusive equivalência ISBN-10/13. Ausência em ambas é cacheada; consulta incompleta/falha não é. Endpoint Books API da Open Library é legado, podendo deixar de ser oferecido: o fallback manual não depende dele.

Para uso frequente, configure `ISBN_USER_AGENT=MinhaGibiteca/0.1 (seu-contato)` no ambiente ou `.env`, usando um contato que você autorize enviar ao provedor. Nenhum contato é inferido da conta do usuário. As [orientações oficiais da Open Library](https://openlibrary.org/developers/api) recomendam identificação com nome da aplicação/contato e cache. A [Books API](https://openlibrary.org/dev/docs/api/books) documenta os metadados usados nesta integração. Sem rede disponível, os demais cadastros continuam funcionando.
