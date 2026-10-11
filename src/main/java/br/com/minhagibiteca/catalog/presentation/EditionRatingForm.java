package br.com.minhagibiteca.catalog.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class EditionRatingForm {
    @NotNull @Min(0) @Max(5)
    private Integer rating;
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
