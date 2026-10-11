package br.com.minhagibiteca.dashboard.application;

public enum HomeShelf {
    RECENT("recentes", "Adicionados recentemente", "As últimas edições que chegaram ao seu catálogo.", "/edicoes?order=NEWEST"),
    UNREAD("nao-lidos", "Sua próxima leitura", "Histórias da sua coleção que você ainda não marcou como lidas.", "/edicoes"),
    PHYSICAL("fisicos", "Na sua estante", "Suas edições cadastradas com suporte físico.", "/edicoes?mediaType=PHYSICAL"),
    DIGITAL("digitais", "Biblioteca digital", "Seus livros e quadrinhos cadastrados com suporte digital.", "/edicoes?mediaType=DIGITAL"),
    READ("lidos", "Já lidos para revisitar", "Histórias que você já marcou como lidas.", "/edicoes");

    private final String key, title, description, catalogUrl;
    HomeShelf(String key, String title, String description, String catalogUrl) {
        this.key = key; this.title = title; this.description = description; this.catalogUrl = catalogUrl;
    }
    public String key() { return key; }
    public String title() { return title; }
    public String description() { return description; }
    public String catalogUrl() { return catalogUrl; }
}
