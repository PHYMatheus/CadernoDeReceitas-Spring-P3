package br.com.CadernoDeReceitas.model;

import java.util.ArrayList;

public record ReceitaCulinaria(long id,String nome, Float duracaoDoPreparoMinutos, ArrayList<String> ingredientes, String tutorialPreparo) {
}
