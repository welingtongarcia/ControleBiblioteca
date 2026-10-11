# Cadastro público de usuários

- USU-001: login oferece Criar conta; GET/POST /cadastro públicos com e-mail, senha e confirmação. Sucesso redireciona ao login com confirmação; não autenticar automaticamente.
- USU-002: e-mail normalizado e único; validar formato/tamanho e senhas iguais, de 12 a 64 caracteres e até 72 bytes UTF-8 (limite BCrypt). Hash BCrypt existente, nunca salvar senha/confirmacão em claro. Novas contas enabled=true e role=USER, sem aceitar campos administrativos.
- USU-003: CSRF obrigatório; erros em pt-BR, preservando só e-mail e limpando senhas na tela/modelo de retorno. Contas existentes nunca substituídas. Duplicidade concorrente tratada pela constraint única.
- USU-004: usuário autenticado não cria outra conta pelo fluxo público; coleções isoladas pelos mecanismos existentes. Interface responsiva e acessível por teclado. Sem nome obrigatório, verificação de e-mail ou recuperação de senha nesta entrega.

Tarefas:
- [x] Serviço de cadastro e formulário/controller, sem entidades JPA na apresentação.
- [x] Liberar somente /cadastro e adicionar página/link de login.
- [x] Testar persistência/hash, duplicidade, validação, CSRF, papel fixo, login e isolamento; verificar interface/suíte.

Sem mudança de esquema: users já possui e-mail único, enabled e role. Publicação na internet exige HTTPS, configuração de produção e proteção contra abuso/rate limiting no ambiente; este fluxo não comprova posse do e-mail.

## Implementação e verificação

UserRegistrationService normaliza/valida entrada, usa PasswordEncoder existente e fixa USER/conta ativa. saveAndFlush e constraint uk_users_email_normalized impedem duplicidade; conflito é convertido em erro genérico sem substituir contas. Command não possui toString com senha. RegistrationForm valida confirmação/tamanho/bytes; RegistrationController permite apenas três campos e limpa senhas e valores rejeitados do BindingResult antes de devolver erros. Sucesso usa flash e PRG para login; usuário já autenticado segue para o início.

SecurityConfiguration libera /cadastro mantendo CSRF e demais rotas privadas. auth/register.html usa campos rotulados/autocomplete, resumo de erros e botões de navegação. Login inclui Criar conta e confirmação por flash. CSS auth existente ampliado; docs/installation.md explica o fluxo e limites.

UserRegistrationServiceTest: 3 testes de validação, papel/hash e duplicidade, incluindo simulação Mockito de corrida de constraint. MinhaGibitecaApplicationTest: 3 testes com MockMvc/PostgreSQL para cadastro/login real, hash BCrypt, e-mail normalizado, tentativa de elevação de papel ignorada, coleção vazia/isolada, validação, duplicidade, senhas/rejeições removidas do modelo, CSRF e sessão autenticada. InterfaceIntegrationTest: formulário público, erros e login pós-cadastro com auditoria de rótulos/IDs/ARIA.

Execução final: `./mvnw -q clean verify`, exit 0; 225 testes, 0 falhas, 0 erros, 0 ignorados. `git diff --check`: exit 0. Navegador com prévias sintéticas em servidor local temporário: 4 estados (login, cadastro, erros e login com confirmação) em 320/390/768/1280px, 16 verificações sem overflow horizontal e sem senhas preenchidas; inspeção visual e ordem de foco e-mail → senha → confirmação → criar conta. Sem envio de cadastro real pelo navegador. Aba/servidor encerrados e viewport restaurado.

Dados reais/data/ preservados; aplicação não reiniciada. Sem confirmação de e-mail, recuperação de senha, gerenciamento administrativo ou rate limiting de cadastro nesta entrega. Proteção adicional de implantação pública permanece uma pendência explícita, não foi testada/declarada concluída.
