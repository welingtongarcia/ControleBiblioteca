# Verificação — busca assistida por ISBN

Data: 2026-10-09. T027, FR-027/028, AC-015.

## Decisões e arquivos

Spec, plan, tasks e analysis atualizados antes da implementação. Clarificação comunicada: busca somente no cadastro novo, preenchimento de campos vazios, associação por nome único nos cadastros próprios, sugestões sem criação automática e capa apenas por consulta à fonte/upload manual. Revisão e salvar continuam obrigatórios. Constituição e stack preservadas; nenhuma alteração de esquema.

- IsbnMetadata/IsbnProvider/IsbnLookupService: DTO, porta, checksum ISBN-10/13, normalização, cache de até 200 entradas por dez minutos (inclusive ausências), uma consulta externa por segundo por processo, mensagens pt-BR e fallback manual.
- IsbnClientConfiguration/OpenLibraryIsbnProvider: HTTPS e host fixos, sem redirecionamentos, conexão de três segundos/leitura de cinco segundos, resposta até 256 KiB, dados textuais limitados e URLs externas ignoradas. Sem chave; User-Agent configurável. Somente o ISBN é enviado.
- EditionController: POST autenticado/CSRF, PRG com rascunho/contexto, binding permitido explícito; título/ano apenas vazios; editora/criadores associados somente por correspondência única própria. Busca não grava edição ou cadastros auxiliares.
- edicoes/form.html, dashboard.css e application.yml: botão Buscar pelo ISBN, aviso de revisão, sugestões escapadas, link fixo da fonte, feedback e layout responsivo.
- IsbnLookupTest, OpenLibraryIsbnProviderTest e MinhaGibitecaApplicationTest: serviço, HTTP simulado e fluxo MVC/PostgreSQL; fixture descartável em target/ui-isbn. docs/installation.md contém uso e configuração.

## Evidências executadas

`./mvnw clean verify -q`: execução final com código 0; **95 testes, zero falhas, erros ou ignorados**. São 38 integrados, 22 navegação, 10 checklist, 8 exemplares, 4 leitura, 3 resumo, 4 serviço ISBN e 6 adaptador HTTP. PostgreSQL 16 em Testcontainers, migrações V1–V13 e teste de upgrade executados. Tentativas anteriores revelaram uma asserção ambígua de FlashMap e padrão de redirect incompatível com barras; reparados antes da execução final. GET de renderização usa parâmetro returnTo decodificado, como recebido pelo servlet real.

Cobertura nova: checksum e normalização; inválido sem chamada externa; cache positivo/negativo e expiração; limite de chamadas/falha sem cache; JSON incompleto/malformado, resposta excessiva, 429 e timeout. URL de fonte sempre fixa, mesmo com URLs maliciosas na resposta. MVC verifica login, CSRF 403, preenchimento sem persistência, isolamento dos cadastros de outro proprietário, conteúdo escapado, preservação de título/ano já informados, valor pago/digital/observações/contexto, erros de conversão e fallback. Salvar explicitamente depois da busca persiste a edição.

Smoke externo separado com curl, ISBN público 9780140328721 da documentação do provedor: código 0, aproximadamente 0,776 s; retornou Fantastic Mr. Fox, Roald Dahl, Puffin e October 1, 1988. Isso verifica disponibilidade do endpoint naquele momento, não representa teste E2E da sessão real ou do cliente Java conectado à internet. Testes do adaptador Java usam HTTP simulado e não dependem da rede.

Prévia do HTML renderizado pelo MockMvc: desktop conferido visualmente; celular 320 × 780 com captura completa, documento 320/320 sem rolagem horizontal. Sugestões, dados preservados e campos legíveis; retorno/cancelar mantêm q=HQ e page=1. Pular para o conteúdo por teclado focou main-content. Nenhum POST na prévia estática; mutações verificadas via MockMvc. Viewport restaurado, aba fechada e servidor temporário encerrado.

`git diff --check`: sem erros. data/ preservado e não versionado. Nenhum acesso de gravação ao banco real e aplicação em uso não reiniciada.

## Limites e riscos

Cobertura bibliográfica depende da Open Library, especialmente para HQs brasileiras. Books API é endpoint legado; pode exigir adaptação futura. Só consulta individual; cache/limite são locais por processo (múltiplas instâncias exigiriam coordenação). User-Agent pode incluir contato público via ISBN_USER_AGENT, sem segredo. Não importa capa, não consulta Google Books nem cria editora/autores. ISBN continua opcional e livre no cadastro manual; validação específica somente na busca. Edição existente não ganha busca nesta etapa. Valores, série, numeração, suporte e leitura não são inferidos. Timeout de leitura protege o socket, não é orçamento global de toda a operação. Auditoria WCAG completa e E2E da sessão real continuam pendentes nas tarefas gerais.
