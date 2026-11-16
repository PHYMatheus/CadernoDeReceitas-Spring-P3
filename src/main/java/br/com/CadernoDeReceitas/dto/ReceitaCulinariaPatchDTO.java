package br.com.CadernoDeReceitas.dto;

import java.util.List;

public record ReceitaCulinariaPatchDTO(
        String nome,
        Float duracaoDoPreparoMinutos,
        List<String> ingredientes,
        String tutorialPreparo
) {}
