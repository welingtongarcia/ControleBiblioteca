# OneDrive por rclone — SDD

## Constituição e escopo

Aplicam-se AGENTS.md e a constituição existente: Java 21/MVC/Thymeleaf, CSRF, proprietário por consulta e mutação, pt-BR, nenhuma alteração remota ou exposição de credenciais. Complementa `016-onedrive` sem remover OAuth direto. Não requer conta Azure própria; usa o cliente compartilhado do rclone. Consentimento Microsoft continua obrigatório.

## Especificação e esclarecimentos

- RC-01: habilitação opcional, sem impedir inicialização se o executável ou a autorização ainda não existir. Instalar rclone separadamente para execução nativa; imagem Docker incluirá o programa.
- RC-02: responsável configura no computador servidor, interativamente, um remote chamado `onedrive`, do tipo `onedrive`, por usuário. Arquivo exclusivo `<diretório configurado>/<UUID autenticado>.conf`. A interface mostra somente o UUID da própria conta e orientações; nunca aceita comandos, nomes de executável ou caminhos de configuração do navegador. Login Microsoft somente no navegador do rclone, não coletado pela aplicação.
- RC-03: botão POST com CSRF verifica configuração e acesso antes de ativar. Separação por proprietário, sem conexão compartilhada entre contas. Reconectar/trocar provedor limpa seleção/índice/vínculos com aviso prévio; desconectar remove apenas dados no banco, não revoga nem apaga a configuração rclone.
- RC-04: reutilizar seleção de até 20 pastas, navegação, sincronização manual recursiva, formatos, paginação, filtros e vínculos existentes. Comandos limitados a `listremotes --long` e `lsjson`/`--stat`, sem shell, sem downloads, uploads, links públicos ou operações de escrita. Configuração externa pode renovar seu token e deve ser gravável somente pelo usuário do serviço.
- RC-05: `--read-only`, escopos `Files.Read offline_access`, sem herdar variáveis RCLONE_*; stdin fechado; stderr descartado sem logs; JSON limitado a 16 MiB, processo a 20 segundos. Mesmos limites da sincronização existente de itens/pastas/consultas e orçamento de 90 s, podendo exceder por uma consulta de até 20 s. Falha não publica índice parcial. Rejeitar caminhos relativos perigosos/absolutos/remotes arbitrários, atalhos simbólicos de configuração e dados inválidos.
- RC-06: arquivo de configuração contém tokens e fica fora do Git, contexto Docker e backup portátil. Provedor rclone marcado explicitamente na conexão existente, sem tokens no banco; não alterar migrações aplicadas. `lsjson` não fornece link Web confiável: não gerar link de compartilhamento nem exibir botão de abertura para esses itens. Pastas movidas devem ser selecionadas novamente; IDs de arquivos preservados quando o backend fornece ID.
- RC-07: documentação de instalação Linux/Windows/macOS, consentimento somente leitura e configuração Docker; nenhuma cobrança pelo rclone, sujeitos a limites do cliente compartilhado/Microsoft e planos de armazenamento existentes. Não prometer conexão real sem consentimento executado.

## Plano e análise

Adicionar settings, executor seguro e adaptador rclone na infraestrutura; serviço existente escolhe adaptador pelo marcador de provedor da conexão, preservando transações de publicação e autorização. Controller adiciona ativação rclone; templates explicam configuração por conta e ambos os modos. Não há frontend separado nem dependência nova de build. Testes unitários de subprocesso/JSON e MockMvc+PostgreSQL com executor simulado; executar também rclone real contra dados locais sintéticos, sem conta pessoal, se o binário estiver disponível.

## Tarefas rastreáveis

- [ ] RC-01/02/05: configuração e executor seguro, testes de limites e isolamento.
- [ ] RC-03/04/06: adaptador, serviço, rotas e interface; regressão OAuth e testes de persistência.
- [ ] RC-07: guias e configuração Docker; validar build/testes e registrar evidências/limitações.

Riscos: configuração rclone confiada ao administrador do servidor, tokens externos sem criptografia própria da aplicação (permissões restritas necessárias), expiração/revogação e throttling Microsoft, integração real dependente do usuário. Não habilitar ou autorizar conta real automaticamente.
