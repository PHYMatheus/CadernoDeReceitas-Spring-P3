package br.com.CadernoDeReceitas.dto;

import jakarta.persistence.Column;

import java.util.ArrayList;

public record ReceitaCulinariaResponse( Long id, String nome, Float duracaoDoPreparoMinutos, ArrayList<String> ingredientes, ArrayList<String> tutorialPreparo) {
}
