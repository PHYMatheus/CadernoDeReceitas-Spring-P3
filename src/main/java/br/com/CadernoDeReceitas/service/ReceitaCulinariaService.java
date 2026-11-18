package br.com.CadernoDeReceitas.service;

import br.com.CadernoDeReceitas.model.ReceitaCulinaria;
import br.com.CadernoDeReceitas.model.ReceitaCulinariaInput;
import br.com.CadernoDeReceitas.model.ReceitaCulinariaPatchInput;
import br.com.CadernoDeReceitas.repository.ReceitaCulinariaRepository;
import br.com.CadernoDeReceitas.web.ApiException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public final class ReceitaCulinariaService {
    private static final int DuracaoMaxima = 1440;
    private final ReceitaCulinariaRepository receitaCulinariaRepository;

    public ReceitaCulinariaService(ReceitaCulinariaRepository receitaCulinariaRepository) {
        this.receitaCulinariaRepository = receitaCulinariaRepository;
    }

    public ReceitaCulinaria create(ReceitaCulinariaInput input) {
        validateReceita(input);
        return receitaCulinariaRepository.create(input.nome().trim(), input.duracaoDoPreparoMinutos(),
                cleanIngredients(input.Ingredientes()), input.TutorialPreparo().trim());
    }

    public List<ReceitaCulinaria> listByName(String nome) {
        String filter = nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
        return receitaCulinariaRepository.findAll().stream().filter(c -> c.nome()
                .toLowerCase(Locale.ROOT).contains(filter)).toList();
    }

    public List<ReceitaCulinaria> listAll() {
        return receitaCulinariaRepository.findAll();
    }

    public ReceitaCulinaria findById(Long id) {
        validateID(id);
        return receitaCulinariaRepository.findById(id).orElseThrow(this::notFound);
    }

    public ReceitaCulinaria update(Long id, ReceitaCulinariaInput input) {
        validateID(id);
        validateReceita(input);
        return receitaCulinariaRepository.update(id, input.nome().trim(), input.duracaoDoPreparoMinutos(),
                cleanIngredients(input.Ingredientes()), input.TutorialPreparo().trim()).orElseThrow(this::notFound);
    }

    public ReceitaCulinaria patch(Long id, ReceitaCulinariaPatchInput input) {
        validateID(id);
        if (input == null) {
            throw new ApiException(400, "Dados da Receita são obrigatórios!");
        }
        if (input.duracaoDoPreparoMinutos() != null) {
            validatePositive(input.duracaoDoPreparoMinutos());
            validateBusinessRule(input.duracaoDoPreparoMinutos());
        }
        if (input.nome() != null) validateText(input.nome(), "nome");
        if (input.TutorialPreparo() != null) validateText(input.TutorialPreparo(), "TutorialPreparo");
        if (input.Ingredientes() != null) validateIngredients(input.Ingredientes());

        return receitaCulinariaRepository.patch(id, old -> new ReceitaCulinaria(
                old.id(),
                input.nome() != null ? input.nome().trim() : old.nome(),
                input.duracaoDoPreparoMinutos() != null ? input.duracaoDoPreparoMinutos() : old.duracaoDoPreparoMinutos(),
                input.Ingredientes() != null ? cleanIngredients(input.Ingredientes()) : old.ingredientes(),
                input.TutorialPreparo() != null ? input.TutorialPreparo().trim() : old.tutorialPreparo()
        )).orElseThrow(this::notFound);
    }

    public void deleteById(Long id) {
        validateID(id);
        if (!receitaCulinariaRepository.deleteById(id)) {
            throw notFound();
        }
    }

    private void validateBusinessRule(float duracaoDoPreparoMinutos) {
        if (duracaoDoPreparoMinutos > DuracaoMaxima) {
            throw new ApiException(422, "Uma Receita pode ter no Máximo 1.440 Minutos!");
        }
    }

    private ApiException notFound() {
        return new ApiException(404, "Receita culinária não encontrada!");
    }

    private void validateID(Long id) {
        if (id == null || id <= 0) {
            throw new ApiException(400, "ID inválido!");
        }
    }

    private void validateNull(ReceitaCulinariaInput input) {
        if (input == null) {
            throw new ApiException(400, "Dados da Receita são obrigatórios!");
        }
    }

    private ArrayList<String> cleanIngredients(List<String> ingredientes) {
        return new ArrayList<>(ingredientes.stream().map(String::trim).toList());
    }

    private void validatePositive(float duracaoDoPreparoMinutos) {
        if (!(duracaoDoPreparoMinutos > 0)) {
            throw new ApiException(400, "duracaoDoPreparoMinutos deve ser maior que zero!");
        }
    }

    private void validateText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApiException(400, field + " não pode estar em branco!");
        }
    }

    private void validateIngredients(List<String> ingredientes) {
        if (ingredientes == null || ingredientes.isEmpty()) {
            throw new ApiException(400, "Ingredientes deve ter pelo menos um item!");
        }
        if (ingredientes.stream().anyMatch(i -> i == null || i.isBlank())) {
            throw new ApiException(400, "Ingredientes não pode ter item em branco!");
        }
    }

    private void validateReceita(ReceitaCulinariaInput input) {
        validateNull(input);
        validateText(input.nome(), "nome");
        validateText(input.TutorialPreparo(), "TutorialPreparo");
        validateIngredients(input.Ingredientes());
        validatePositive(input.duracaoDoPreparoMinutos());
        validateBusinessRule(input.duracaoDoPreparoMinutos());
    }
}
