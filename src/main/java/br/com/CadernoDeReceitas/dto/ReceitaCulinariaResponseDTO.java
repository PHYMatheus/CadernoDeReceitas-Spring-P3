package br.com.CadernoDeReceitas.dto;

import java.util.List;

public record ReceitaCulinariaResponseDTO(Long id, String nome, Float duracaoDoPreparoMinutos, List<String> ingredientes, String tutorialPreparo) {
}
