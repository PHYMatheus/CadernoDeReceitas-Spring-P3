package br.com.CadernoDeReceitas.model;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;

public record ReceitaCulinariaInput(@NotBlank String nome, @Positive float duracaoDoPreparoMinutos, ArrayList<String> Ingredientes, @NotBlank String TutorialPreparo) {
}