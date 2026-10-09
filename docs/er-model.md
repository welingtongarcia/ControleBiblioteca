# Modelo entidade-relacionamento lógico

```text
User 1 ─── * Publisher
User 1 ─── * Series ─── 0..1 Publisher
User 1 ─── * Edition ─── 0..1 Publisher
                           └── 0..1 Series
Edition * ─── * Work            (EditionWork)
Edition * ─── * Creator         (EditionCreator)
Edition 1 ─── * PhysicalCopy ─── 0..1 StorageLocation
User 1 ─── * StorageLocation (auto-referência pai, profundidade <= 4)
Series 1 ─── * SeriesChecklistItem
User 1 ─── * SeriesChecklistItem
Edition 1 ─── 0..1 CoverImage
```

## Regras estruturais

- `owner_user_id` é obrigatório em Publisher, Series, Edition, PhysicalCopy, StorageLocation e SeriesChecklistItem.
- Relações escolhidas no formulário (editora, série, edição, localização) devem pertencer ao mesmo `owner_user_id`; a camada de aplicação valida isso antes de persistir.
- `EditionWork` e `EditionCreator` impedem pares duplicados com chave/índice único composto. Papéis de criador são opcionais e textuais controlados posteriormente.
- Uma `StorageLocation` pode apontar para pai do mesmo dono; validação impede ciclo e profundidade maior que quatro. Ordem: ROOM → SHELF_UNIT → SHELF → BOX, permitindo omitir níveis e começar com qualquer tipo na raiz. Cada ligação aumenta a posição do tipo. V10 garante pai do mesmo dono com FK composta e nomes únicos entre irmãos.
- `CoverImage` guarda somente chave de armazenamento, nome original sanitizado, tipo, tamanho, hash e datas. O conteúdo vive fora do banco.
- Edição não é apagável enquanto houver PhysicalCopy; demais exclusões seguem a mesma validação de dependências especificada.
