package br.com.CadernoDeReceitas.model;

import java.util.List;

public record ReceitaCulinariaPage(List<ReceitaCulinariaInput> items, int page, int size, long totalElements, int totalPages) {
}
