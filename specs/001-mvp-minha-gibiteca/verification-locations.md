# Verificação — localizações

Data: 2026-10-09. T013, FR-032/033, AC-010, NFR-001/002/004.

## Regras e análise

Especificação e plano atualizados antes do código. Quatro tipos ordenados permitem saltos e raízes de qualquer tipo; toda ligação aumenta a posição. Edição também valida tipos dos filhos. Auto-referência e ancestrais impedem ciclos. Serviço usa transação e lock do proprietário para serializar alterações de árvore; FK composta impede pai alheio no próprio banco. Nomes são únicos entre irmãos/raízes, sem distinção de maiúsculas. Não há remoção recursiva implícita.

## Arquivos

- Novo módulo inventory: StorageLocation/LocationLevel; LocationService e LocationView DTO; StorageLocationRepository/LocationQueries; LocationController/LocationForm.
- V10 cria storage_locations, constraints e índices. Dicionário e ER atualizados.
- Templates localizacoes/list, form e delete: busca paginada, caminho completo, validação de campos e confirmação POST/CSRF. Link no painel e mensagens de conversão pt-BR.
- Testes integrados e limpeza do banco de teste incluem as localizações.

## Evidências

`./mvnw clean verify -q`: código 0; 19 testes, zero falhas/erros, JAR gerado. PostgreSQL real em Testcontainers aplicou migrations V1–V10; atualização de schema V7 também passou.

Casos novos: caminho completo de quatro níveis; realocação com salto; auto-referência/ciclo/ordem invertida/tipo incompatível com filhos/duplicado na raiz rejeitados; exclusão de pai bloqueada e folha removida; formulários MVC e erros renderizados; opção e pai alheios protegidos; CSRF e confirmação de exclusão; paginação 20 + 5; FK do banco rejeita pai de outro proprietário.

`git diff --check`: sem erros.

## Pendências

Bloqueio de localização referenciada por exemplar depende de T014, pois não há tabela de exemplares nesta fase. Teste concorrente do lock, aferição de desempenho, E2E no navegador e auditoria visual/WCAG completa ainda não executados. Listagem usa consulta com joins ancestrais e paginação no banco; opções de pai carregam as localizações próprias. Aplicação e banco local da coleção não reiniciados/migrados manualmente; Flyway aplica V10 na próxima inicialização. data preexistente preservado.
