# Dicionário de dados inicial

Este é o contrato de modelagem para migrations da Fase 1 em diante; os nomes físicos finais são confirmados antes de cada migration.

| Entidade | Campos essenciais | Constraints/índices |
| --- | --- | --- |
| `users` | `id`, `email`, `password_hash`, `role`, `enabled`, auditoria | e-mail único normalizado; hash nunca retornado. |
| `publishers` | `id`, `owner_user_id`, `name`, `country_code?`, `notes?`, auditoria | único por `(owner_user_id, normalized_name)`; índice de lista por dono/nome. |
| `series` | `id`, `owner_user_id`, `publisher_id?`, `name`, `start_year?`, `end_year?`, `status?`, `notes?` | FK de mesma propriedade validada; índice dono/nome e dono/editora. |
| `editions` | `id`, `owner_user_id`, `series_id?`, `publisher_id?`, `title`, `editorial_number?`, `variant?`, `publication_year?`, `isbn?`, `format`, `edition_type?`, `description?`, auditoria | ISBN nullable e não único; índices dono/título, dono/série, dono/editora; número e variante texto limitado. |
| `works` | `id`, `owner_user_id`, `title`, `original_year?`, `summary?` | índice dono/título. |
| `edition_works` | `edition_id`, `work_id`, `sequence?` | unicidade `(edition_id, work_id)`; FKs protegidas. |
| `creators` | `id`, `owner_user_id`, `name`, `sort_name?` | índice dono/nome. |
| `edition_creators` | `edition_id`, `creator_id`, `role?`, `sequence?` | unicidade `(edition_id, creator_id, role)`; role não nulo se usado na unicidade. |
| `storage_locations` | `id`, `owner_user_id`, `parent_id?`, `level`, `name`, `notes?` | índice dono/pai/nome; ciclo/profundidade na aplicação. |
| `physical_copies` | `id`, `owner_user_id`, `edition_id`, `storage_location_id?`, `condition`, `condition_notes?`, `acquisition_source?`, `acquisition_date?`, `acquisition_price?`, `notes?`, auditoria | preço `numeric(19,2)` nullable; índice dono/edição e dono/localização. |
| `series_checklist_items` | `id`, `owner_user_id`, `series_id`, `editorial_number`, `label?`, `owned`, `notes?` | único `(owner_user_id, series_id, editorial_number)`; nenhum número é gerado. |
| `cover_images` | `id`, `owner_user_id`, `edition_id`, `storage_key`, `original_filename`, `content_type`, `byte_size`, `sha256`, auditoria | um por edição no MVP; `storage_key` único; nenhuma coluna binária. |

Todos os timestamps usam instante UTC; valores monetários usam `numeric`, nunca `float`. Comprimentos, enums e regras exatas serão consolidados nos forms e na migration correspondente antes de implementação.

Extensão FR-026 (V8): `editions.media_type` nullable PHYSICAL/DIGITAL; `cover_price` e `reference_paid_price` nullable `numeric(12,2)`, não negativos, em BRL. Null representa desconhecido e zero representa gratuito. Valor pago de referência não agrega preços de exemplares. Registros antigos preservam campos novos nulos.

Catálogo T012 (V9): índices de edição por `(owner_user_id, lower(format))`, `(owner_user_id, media_type, normalized_title, id)` e `(owner_user_id, created_at, id)` atendem filtros de formato/suporte e ordenação por cadastro. Busca textual literal usa LIKE com escape e proprietário obrigatório; índices B-tree não garantem acelerar buscas com `%termo%`. Plano de execução e desempenho em 10.000 exemplares serão medidos em T018.

Localizações T013 (V10): `storage_locations` contém id/owner_user_id/parent_id opcional, level (ROOM/SHELF_UNIT/SHELF/BOX), name/normalized_name (120), notes opcional (2000). FK `(parent_id, owner_user_id)` garante pai do mesmo dono; constraint impede auto-referência. Unicidade NULLS NOT DISTINCT em `(owner_user_id, parent_id, normalized_name)` também protege nomes de raízes. Índices dono/nome/id e dono/pai. Ordem dos tipos e ciclos validados pelo serviço; mutações serializadas via lock do proprietário. Exclusão com filhos bloqueada por serviço e FK. Referências de exemplares serão acrescentadas em T014.
