# Esclarecimentos e premissas do MVP

| ID | Decisão/premissa | Impacto | Situação |
| --- | --- | --- | --- |
| CL-01 | O MVP suporta contas privadas, mas não terá auto cadastro público. Um bootstrap seguro cria o primeiro usuário. | Evita expor uma aplicação privada; exige comando/documentação de operação. | Resolvido |
| CL-02 | Editoras, séries e edições são proprietárias por usuário no MVP, mesmo que metadados possam ser compartilhados no futuro. | Isolamento direto e simples; evita catálogo global prematuro. | Resolvido |
| CL-03 | Ano da edição é armazenado como `anoPublicacao` inteiro opcional; mês/dia ficam fora do MVP. | Mantém metadados incompletos sem inventar precisão. | Resolvido |
| CL-04 | Checklist é fonte explícita das lacunas; exemplares não geram nem removem itens automaticamente. | Impede inferência errada em numeração irregular. | Resolvido |
| CL-05 | Exclusão física será inicialmente bloqueada em registros referenciados; itens sem dependências podem ser excluídos. Soft delete geral não entra sem requisito de histórico. | Protege integridade e limita complexidade. | Resolvido |
| CL-06 | Capa será guardada em diretório/volume local configurável no MVP, com interface de armazenamento para futura troca por objeto storage. | Requer backup conjunto dos arquivos e banco. | Resolvido |
| CL-07 | Aquisição é simplificada no MVP como campos do exemplar (origem, data opcional, valor opcional); `Acquisition`/itens ficam para evolução. | Entrega rastreio básico sem fluxo financeiro. | Resolvido |
| CL-08 | Formatos e conservação serão enums controlados inicialmente, com `OUTRO` e descrição opcional quando necessário. | Facilita filtro e relatório; preserva flexibilidade. | Resolvido |
| CL-09 | Número editorial e variante são texto livre limitado, por suportarem `0`, `1A`, `1/2` e especiais. | Não há ordenação numérica presumida. | Resolvido |
| CL-10 | O prazo/SLA de produção não foi definido. A meta de p95 é uma meta de validação local/de referência, não garantia de hosting. | Não bloqueia MVP; requer medição futura. | Aberto, não bloqueante |

Não há questão de alto impacto ainda pendente. Qualquer mudança em privacidade, modelo proprietário/global, retenção ou hospedagem requer nova decisão antes de implementar.
