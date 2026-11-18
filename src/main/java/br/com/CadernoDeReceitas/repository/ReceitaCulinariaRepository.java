package br.com.CadernoDeReceitas.repository;

import br.com.CadernoDeReceitas.model.ReceitaCulinaria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface ReceitaCulinariaRepository {
    ReceitaCulinaria create(String nome, float duracaoDoPreparoMinutos, ArrayList<String> ingredientes, String tutorialPreparo);

    List<ReceitaCulinaria> findAll();

    Optional<ReceitaCulinaria> findById(long id);

    Optional<ReceitaCulinaria> update(long id, String nome, float duracaoDoPreparoMinutos, ArrayList<String> ingredientes, String tutorialPreparo);

    Optional<ReceitaCulinaria> patch(long id, UnaryOperator<ReceitaCulinaria> update);

    boolean deleteById(long id);
}
