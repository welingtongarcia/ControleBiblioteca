# Organização de Editar edição — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Melhorar ordem e orientação do formulário compartilhado, com atalhos exclusivos da edição. Preservar todos os dados, validação, vínculos, autorização, CSRF e PRG.

- EEL-001: ordem Identificação, ISBN, Suporte e valores, Páginas e leitura, Vínculos bibliográficos, Observações; identificação da HQ no cabeçalho ao editar. Busca externa continua exclusiva do cadastro, sem novas chamadas ao editar.
- EEL-002: navegação por âncoras na edição para identificação, compra, leitura, vínculos e observações; ações superiores Salvar alterações e Cancelar, mantendo ações inferiores e o mesmo formulário POST. Nenhum dado salvo ao usar âncoras/Cancelar.
- EEL-003: largura confortável, agrupamento responsivo e foco/atalhos por teclado, sem controles sobrepostos ou overflow em 320/390/768/1024/1440 px. Erros e rascunho preservados; campos de páginas com aria-invalid e descrição de erro.

## Plan → tasks → analyze

- [ ] EEL-T1 Reordenar fieldsets, acrescentar atalhos/ações e CSS específico, sem mudar backend.
- [ ] EEL-T2 Verificar ordem/ações/validação em MockMvc e suíte Maven; QA edição em desktop/móvel.

Hoje Páginas e leitura precede o título e ISBN. Template atende cadastro/edição: reordenação comum mantém cadastro coerente; elementos superiores exclusivos da edição. Botão superior usa atributo form com ID do formulário, sem form aninhado ou JavaScript. Nenhum CSS sticky que oculte campos/foco.
