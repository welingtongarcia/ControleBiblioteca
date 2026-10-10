# Ações das séries como botões — 2026-10-10

## Constitution → specify → clarify

Constituição mantida. Escopo restrito às cinco ações de cada série na listagem.

- SAB-001: Edições e leituras, Checklist, Faltantes e Editar têm aparência de botão secundário, mas mantêm semântica de link e URLs atuais. Excluir permanece botão POST com CSRF e tem destaque visual de perigo.
- SAB-002: alinhamento, quebra de linha entre botões (não dentro dos rótulos), foco visível e alvo mínimo 44 px; sem overflow horizontal em 320/390/768/1024/1440 px. Não alterar regras de exclusão, leitura ou checklist.

## Plan → tasks → analyze

- [ ] SAB-T1 Aplicar classes específicas no template e CSS compartilhado, sem modificar ações/controllers.
- [ ] SAB-T2 Validar HTML, navegação e exclusão em testes existentes; conferir aparência e responsividade no navegador.

Preservar anchors para navegação, sem role=button ou JavaScript artificial. Manter formulário de exclusão e retorno contextual. Estilos limitados às ações da listagem de séries.
