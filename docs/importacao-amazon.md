# Cadastrar livro ou HQ pelo link Amazon

Em **Minha coleção → Catálogo de edições → Nova edição**, use **Importar pelo link da Amazon**:

1. Cole o endereço completo do produto na Amazon Brasil.
2. Clique em **Buscar dados pelo link**.
3. Confira a prévia, a fonte, ISBN, editora e demais campos disponíveis.
4. Clique em **Aplicar sugestões nos campos vazios**.
5. Revise o formulário e clique em salvar para realmente cadastrar a edição.

Buscar/aplicar não cria uma edição no banco. Se um campo já estiver preenchido, ele não será substituído. As sugestões expiram após dez minutos e ficam disponíveis somente para a conta que fez a consulta.

## O que pode ser preenchido

Título, ISBN, ano, páginas, formato e sinopse (no campo observações, apenas se estiver vazio). Editoras e criadores só são associados se já existirem nos seus cadastros e houver correspondência única de nome; os nomes não encontrados continuam visíveis na prévia. Não são criados cadastros auxiliares nem alteradas funções dos criadores.

Tipo de publicação (Livro/Quadrinho), suporte (físico/digital), preço, data da compra, número editorial, personagens, avaliação e progresso de leitura continuam manuais. O preço anunciado não representa o valor pago e não entra no financeiro.

A capa, quando identificada, aparece como **Consultar capa**. Não é baixada ou salva automaticamente. Depois de salvar a edição, o envio de capa continua disponível pelos mecanismos existentes.

## Links aceitos

- `https://www.amazon.com.br/dp/655448244X`
- `https://www.amazon.com.br/Thorgal-Clássica-Jean-Van-Hamme/dp/655448244X`
- `https://www.amazon.com.br/gp/product/655448244X`

Links longos com parâmetros de campanha também são aceitos: o sistema descarta os parâmetros e acessa somente a URL canônica do produto. Endereços curtos como `amzn.to` precisam ser abertos por você para copiar o endereço completo. Outros países/domínios e endereços sem HTTPS não são suportados nesta entrega.

## Quando faltarem dados

A consulta pública é de melhor esforço e pode falhar por bloqueio, mudança de layout, indisponibilidade ou ficha incompleta. Não usamos conta Amazon, credenciais, APIs pagas, proxies ou solução de CAPTCHA. Não há tentativa de contornar restrições de acesso.

Quando possível, o sistema identifica um ISBN válido no endereço e tenta as fontes bibliográficas já existentes: Open Library e Google Books, se este último estiver configurado. A prévia informa se os dados vieram dessas fontes, e não da Amazon. Um ASIN de dez caracteres não é necessariamente um ISBN: ASINs Kindle não são convertidos sem checksum válido.

Se nenhuma fonte retornar dados, o ISBN identificado ainda pode ser aplicado, ou você pode continuar manualmente. Isso não significa que o livro ou HQ não exista. Nenhuma chave Google Books foi adicionada por esta entrega.

## Limites técnicos e privacidade

Somente a URL canônica/ASIN e, no fallback, o ISBN são enviados aos serviços; seu rascunho, preços, observações e identidade da conta não são enviados. HTTP sem redirects ou cookies de conta, com timeout e resposta HTML limitada a 2 MiB descompactados. Consultas novas têm intervalo mínimo de dois segundos; até 200 prévias ficam em memória por dez minutos. Reiniciar a aplicação invalida as prévias, não os cadastros salvos.

O HTML externo nunca é exibido diretamente. Capas só recebem link para hosts HTTPS permitidos da Amazon; nenhuma imagem é requisitada automaticamente pelo navegador na prévia. O recurso não garante a presença de todos os campos nem a estabilidade do serviço da loja.
