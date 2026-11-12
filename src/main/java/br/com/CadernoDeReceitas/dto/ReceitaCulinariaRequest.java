package br.com.CadernoDeReceitas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;

public record ReceitaCulinariaRequest(@NotBlank(message = "não pode estar em branco") String nome, @Positive(message = "deve ser positivo")  float duracaoDoPreparoMinutos, @NotEmpty(message = "não pode estar vazio") ArrayList<String> ingredientes,@NotBlank(message = "não pode estar em branco")  String tutorialPreparo) {
}
