# Cadastro de edição → detalhes e capa

## Constitution → specify → clarify

Constituição preservada: MVC/Thymeleaf, PRG, CSRF e autorização por proprietário. Após salvar uma nova edição, abrir seus detalhes em vez do catálogo. O envio da capa continua opcional, no formulário existente da tela de detalhes.

- CAD-001: POST /edicoes válido redireciona para /edicoes/{id} da edição recém-criada, com confirmação do cadastro e orientação para adicionar a capa.
- CAD-002: preservar retorno seguro ao catálogo/painel de origem nos detalhes quando informado, sem substituir o destino pelo catálogo. Retornos externos ou inválidos não devem ser aceitos.
- CAD-003: validação inválida mantém formulário e dados, sem cadastrar ou redirecionar. Upload permanece protegido por CSRF, validação de imagem e propriedade; edição e outros cadastros mantêm seus fluxos.

## Plan → tasks → analyze

- [x] T1 Usar UUID retornado por EditionRegistrationService.save no redirect e orientar na confirmação. No interceptor, preservar destino de detalhes especificamente no cadastro de edição, anexando contexto seguro.
- [x] T2 Atualizar expectativas antigas e verificar via MockMvc destino correspondente ao registro salvo, detalhes/formulário de capa, contexto seguro, retorno inválido e validação; executar suíte.
- [x] T3 Registrar resultados, arquivos e pendências; commit após validação real.

Análise: NavigationContext.redirect anexa returnTo do catálogo aos detalhes, mas redireciona diretamente a painéis de origem. O interceptor deve usar withReturn especificamente no POST /edicoes bem-sucedido, sem mudar fluxos de edição ou outros cadastros. Não requer alterar template, esquema ou serviço transacional. Não cadastrar capa automaticamente nem tornar imagem obrigatória.

## Implementação → verificação

EditionController usa o UUID devolvido pelo serviço transacional e redireciona para os detalhes; confirmação orienta o envio da capa. NavigationConfiguration mantém o destino no POST /edicoes e usa NavigationContext.withReturn com contexto já validado. Outros POSTs permanecem com comportamento anterior.

MinhaGibitecaApplicationTest cobre criação sem origem, origem filtrada, Início, Financeiro e origem externa rejeitada; confirma UUID salvo, HTML do formulário de capa, CSRF, mensagem, propriedade e validação sem criação. Expectativas antigas atualizadas nesse arquivo e em FinanceIntegrationTest; testes de upload existentes continuam na suíte.

Primeira execução de `./mvnw -q clean verify`: exit 1, 214 testes e 4 falhas de expectativas antigas/padrão de URL, sem erros; expectativas ajustadas. Execução final do mesmo comando: exit 0, 214 testes, 0 falhas, 0 erros, 0 ignorados. `git diff --check`: exit 0. CAD-001/002/003 atendidos.

Sem alteração de esquema, template ou dados reais; data/ preservado. Sem nova avaliação visual, pois layout não foi alterado; HTML conferido via MockMvc. Aplicação real não foi reiniciada: reiniciá-la para usar o novo fluxo.
