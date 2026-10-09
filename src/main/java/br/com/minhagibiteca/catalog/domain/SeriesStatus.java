package br.com.minhagibiteca.catalog.domain;

public enum SeriesStatus {
    ONGOING("Em andamento"),
    FINISHED("Concluída"),
    PAUSED("Pausada"),
    UNKNOWN("Não informada");

    private final String label;

    SeriesStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
