# Cadastro de edição → detalhes e capa

## Constitution → specify → clarify

Constituição preservada: MVC/Thymeleaf, PRG, CSRF e autorização por proprietário. Após salvar uma nova edição, abrir seus detalhes em vez do catálogo. O envio da capa continua opcional, no formulário existente da tela de detalhes.

- CAD-001: POST /edicoes válido redireciona para /edicoes/{id} da edição recém-criada, com confirmação do cadastro e orientação para adicionar a capa.
- CAD-002: preservar retorno seguro ao catálogo/painel de origem nos detalhes quando informado, sem substituir o destino pelo catálogo. Retornos externos ou inválidos não devem ser aceitos.
- CAD-003: validação inválida mantém formulário e dados, sem cadastrar ou redirecionar. Upload permanece protegido por CSRF, validação de imagem e propriedade; edição e outros cadastros mantêm seus fluxos.

## Plan → tasks → analyze

- [ ] T1 Usar UUID retornado por EditionRegistrationService.save no redirect e orientar na confirmação. Reutilizar interceptor de navegação que preserva contexto nos destinos de detalhes.
- [ ] T2 Atualizar expectativas antigas e verificar via MockMvc destino correspondente ao registro salvo, detalhes/formulário de capa, contexto seguro, retorno inválido e validação; executar suíte.
- [ ] T3 Registrar resultados, arquivos e pendências; commit após validação real.

Análise: NavigationContext.redirect já anexa returnTo validado ao destino de detalhes; não requer alterar infraestrutura de navegação, template, esquema ou serviço transacional. Não cadastrar capa automaticamente nem tornar imagem obrigatória.
