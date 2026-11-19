package br.com.CadernoDeReceitas.model;

import br.com.CadernoDeReceitas.web.ApiException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Set;

public record ReceitaCulinariaPatchInput(
        String nome,
        Float duracaoDoPreparoMinutos,
        ArrayList<String> Ingredientes,
        String TutorialPreparo
) {
    private static final Set<String> FIELDS = Set.of(
            "nome", "duracaoDoPreparoMinutos", "Ingredientes", "TutorialPreparo"
    );

    public static ReceitaCulinariaPatchInput from(JsonNode patch) {
        if (patch == null || !patch.isObject()) throw invalid("O patch deve ser um objeto JSON");
        patch.propertyNames().forEach(field -> {
            if (!FIELDS.contains(field)) throw invalid("Campo desconhecido: " + field);
        });

        String nome = null;
        if (patch.has("nome")) {
            JsonNode value = patch.get("nome");
            if (value.isNull() || !value.isTextual() || value.textValue().isBlank())
                throw invalid("nome deve ser uma string não vazia");
            nome = value.textValue();
        }

        Float duracao = null;
        if (patch.has("duracaoDoPreparoMinutos")) {
            JsonNode value = patch.get("duracaoDoPreparoMinutos");
            if (value.isNull() || !value.isNumber() || value.floatValue() <= 0)
                throw invalid("duracaoDoPreparoMinutos deve ser um número positivo");
            duracao = value.floatValue();
        }

        ArrayList<String> ingredientes = null;
        if (patch.has("Ingredientes")) {
            JsonNode value = patch.get("Ingredientes");
            if (value.isNull() || !value.isArray() || value.isEmpty())
                throw invalid("Ingredientes deve ser uma lista não vazia de strings");
            ArrayList<String> parsed = new ArrayList<>();
            for (JsonNode item : value) {
                if (item == null || !item.isTextual() || item.textValue().isBlank())
                    throw invalid("Ingredientes não pode conter item em branco");
                parsed.add(item.textValue());
            }
            ingredientes = parsed;
        }

        String tutorial = null;
        if (patch.has("TutorialPreparo")) {
            JsonNode value = patch.get("TutorialPreparo");
            if (value.isNull() || !value.isTextual() || value.textValue().isBlank())
                throw invalid("TutorialPreparo deve ser uma string não vazia");
            tutorial = value.textValue();
        }

        return new ReceitaCulinariaPatchInput(nome, duracao, ingredientes, tutorial);
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest(message);
    }
}
