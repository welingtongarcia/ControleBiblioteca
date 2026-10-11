# Publicação e execução documentada

Pedido: repositório público e instruções detalhadas de execução.

- Repositório de destino: welingtongarcia/ControleBiblioteca. API confirmou visibilidade PUBLIC; não foi necessária alteração de visibilidade. Descrição atualizada com nome, recursos e tecnologias.
- README descreve configuração, Docker, IntelliJ, terminal/JAR, Windows/Linux/macOS, primeiro acesso, testes, atualização, backup e problemas comuns. Não define uma licença sem escolha do proprietário.
- Complemento Docker local explícito: cookie HTTP somente para execução local e porta 8080 ligada a 127.0.0.1; configuração base de produção permanece inalterada. Requer Compose >= 2.24.4 para substituir a lista de portas com !override, conforme documentação oficial Docker.
- .dockerignore exclui data; .gitignore já exclui data e .env. Revisão do histórico não encontrou .env/data versionados nem os padrões pesquisados de tokens GitHub/OpenAI ou chaves privadas; essa busca não equivale a auditoria completa de segurança.

Verificação: configuração Compose base e local validadas usando .env.example, sem subir/parar containers, acessar banco real ou expor credenciais locais; propriedades e portas da configuração combinada conferidas. git diff --check sem problemas. Nenhuma alteração no código da aplicação; última suíte anterior passou com 239 testes. Não foi simulado um novo ambiente Windows nem a infraestrutura HTTPS de produção.
