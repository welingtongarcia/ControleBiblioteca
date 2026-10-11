# Minha Biblioteca

Sistema web privado para gerenciar livros e histórias em quadrinhos, construído como monólito Java 21/Spring Boot MVC/Thymeleaf/PostgreSQL.

## Estado atual

O sistema inclui autenticação e cadastro de usuários, catálogo de livros e quadrinhos, capas, exemplares físicos, páginas e progresso de leitura, estrelas, financeiro, relatórios e backup portátil. No cadastro de edição, escolha **Livro** ou **Quadrinho** em **Tipo de publicação**. O catálogo permite filtrar cada tipo. Os registros anteriores permanecem quadrinhos, com seus dados preservados.

Os identificadores técnicos antigos (pacotes Java, banco, caminhos e serviços Docker) permanecem iguais para manter instalações existentes. Os artefatos de requisitos e arquitetura estão em [specs](specs) e [docs](docs).

## Ambiente verificado em 2026-10-08

| Componente | Resultado |
| --- | --- |
| Java | OpenJDK 21.0.12.1 |
| Maven | 3.9.12 |
| Docker Engine | 29.9.0 (daemon acessível) |
| Docker Compose | v5.6.0 |
| GitHub Spec Kit | Não instalado/detectável; artefatos equivalentes adotados |

## Executar

Consulte o [guia de instalação](docs/installation.md). A verificação completa é feita com `./mvnw clean verify`.

Para registrar compras e explorar gastos por período, coleção, criador, personagem e editora, consulte o [guia do Financeiro](docs/financeiro.md).

## Fotos dos criadores

Em **Criadores → Novo criador** ou **Editar**, use **Enviar foto** para escolher PNG ou JPEG de até 5 MB e clique em **Salvar criador**. O arquivo é opcional; em edição, deixar sem arquivo mantém a foto atual. É possível substituir ou remover a foto. Após um erro de formulário, selecione o arquivo novamente. As fotos aparecem na listagem, são privadas por usuário e fazem parte do backup portátil.

## Lista de desejos

No menu **Minha coleção → Lista de desejos**, registre livros e quadrinhos que pretende adquirir. Informe título, autor, editora, ISBN, preço estimado, prioridade e observações. A busca aceita título, autor ou ISBN; os filtros mostram pendentes, adquiridos ou todos. Você pode editar, excluir e marcar como adquirido ou pendente. A lista é privada e faz parte do backup.

Desejos não entram nos totais ou no Financeiro. Ao adquirir uma publicação, cadastre-a também no catálogo com o valor efetivamente pago e a data da compra. Marcar um desejo como adquirido não cria uma edição automaticamente.

## Backup portátil

Para mudar de máquina sem perder o acervo, use **Backup e migração** no menu do sistema. Veja [o guia de backup e restauração](docs/backup-migracao.md). O ZIP transporta os dados e capas da sua conta; a aplicação é instalada separadamente.
