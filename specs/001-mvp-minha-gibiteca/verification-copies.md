# Verificação — exemplares físicos

Data: 2026-10-09. T014, FR-030/031/033, AC-003/011, NFR-001/002/004.

## Escopo e arquivos

Especificação e plano detalhados antes de alterar comportamento. Aquisição é simplificada: preço BRL opcional por cópia, sem inferência pelo valor de referência da edição; zero permitido; conservação obrigatória com padrão Não informada. Várias cópias são permitidas. Edição física ou de suporte desconhecido recebe exemplares, digital não recebe. Edição do exemplar não troca sua referência bibliográfica.

- inventory/domain: PhysicalCopy e CopyCondition; application: CopyService e CopyView DTO; infrastructure: PhysicalCopyRepository e CopyQueries com joins de localização e paginação por vinte.
- inventory/presentation: CopyController e CopyForm com validação, decimal com vírgula/ponto, PRG, ownership e confirmação POST/CSRF. Templates exemplares/form e delete inteiramente pt-BR.
- V11: physical_copies, preço numeric(12,2), conservação restrita, FKs compostas de proprietário/edição/localização, índices e auditoria obrigatória.
- EditionService/Controller e LocationService: bloqueio de referências com orientação ao usuário, inclusive mudança de edição para digital. Mutações usam lock do proprietário em transação.
- Detail de edição: cópias, aquisição, caminho de localização, ações e aviso de duplicados; paginação. Lista de edições renderiza erros de exclusão; confirmação de localização informa dependentes. CSS limita largura de campos de exemplares e mensagens de conversão permanecem pt-BR.
- Dicionário, ER, análise e tarefas atualizados; testes CopyServiceTest e MinhaGibitecaApplicationTest.

## Evidências executadas

`./mvnw clean verify -q`: código 0; 31 testes (8 JUnit/Mockito de serviço e 23 integrados MockMvc/PostgreSQL), zero falhas, erros ou testes ignorados. JAR gerado. Testcontainers com PostgreSQL 16 aplicou V1–V11; atualização do schema V7 até V11 também passou sem alterar dados bibliográficos existentes.

Casos: cópias com R$ 29,90 e R$ 0,00; aviso de duplicados; edição de conservação/localização sem trocar edição; preço desconhecido permanece nulo, sem inferência. Datas e valores formatados em pt-BR, observações com HTML escapadas. Preços negativos, escala excessiva, limite excedido, texto/separador de milhares e data inválida/futura rejeitados; serviço unitário também valida regras. Suporte desconhecido aceito e digital rejeitado; botão de criação oculto em digital. Recursos e referências alheios retornam 404; tentativas não alteram dados. Exclusão de edição/localização ocupada e mudança para digital bloqueadas. Realocação libera localização; remover exemplar só com CSRF/confirmação e preserva edição. Paginação 20 + 1 com link MVC e numeração. FKs do PostgreSQL rejeitam edição/localização alheias e exclusão de edição referenciada por SQL direto.

`git diff --check`: sem erros.

## Pendências e riscos

T015 (checklist explícito/faltantes) é a próxima etapa. Teste concorrente de locks, medição com 10 mil exemplares, E2E de navegador e auditoria visual/WCAG completa não foram executados nesta fase, permanecendo para T018/T020. Seleção de localização carrega todas as opções próprias; avaliar em volume. Regra física/digital é aplicada pelos serviços, não por trigger para SQL manual. Mockito emite aviso de autocarregamento de agente no Java 21; não houve falha, mas a configuração de testes deve ser revista antes de atualização de JDK.

Aplicação e banco da coleção local não foram reiniciados/migrados manualmente; Flyway aplicará V11 na próxima inicialização. Dados preexistentes em data/ preservados e excluídos do commit.
