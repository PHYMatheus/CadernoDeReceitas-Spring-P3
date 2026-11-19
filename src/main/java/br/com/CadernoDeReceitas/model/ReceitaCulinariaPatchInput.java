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
    // Conjunto imutável com os únicos nomes de campos aceitos no JSON do “PATCH”
    private static final Set<String> FIELDS = Set.of(
            "nome", "duracaoDoPreparoMinutos", "Ingredientes", "TutorialPreparo"
    );

    // Método fábrica estático que valida e converte o JsonNode no record
    public static ReceitaCulinariaPatchInput from(JsonNode patch) {
        // Verifica se o corpo é nulo ou não é um objeto JSON (ex: veio um array [] ou texto)
        if (patch == null || !patch.isObject()) throw invalid("O patch deve ser um objeto JSON");

        // Percorre todas as chaves enviadas no JSON; se houver alguma fora do “set” FIELDS, lança 400
        patch.propertyNames().forEach(field -> {
            if (!FIELDS.contains(field)) throw invalid("Campo desconhecido: " + field);
        });

        String nome = null;
        if (patch.has("nome")) { // Só valida se a chave "nome" estiver presente no JSON
            JsonNode value = patch.get("nome");
            // Rejeita se for null explícito, se não for texto (ex: número) ou se for vazio/espaços
            if (value.isNull() || !value.isTextual() || value.textValue().isBlank())
                throw invalid("nome deve ser uma string não vazia");
            nome = value.textValue(); // Extrai a “String” validada
        }

        Float duracao = null;
        if (patch.has("duracaoDoPreparoMinutos")) {
            JsonNode value = patch.get("duracaoDoPreparoMinutos");
            // Rejeita null, não numéricos e valores <= 0
            if (value.isNull() || !value.isNumber() || value.floatValue() <= 0)
                throw invalid("duracaoDoPreparoMinutos deve ser um número positivo");
            duracao = value.floatValue();
        }

        ArrayList<String> ingredientes = null;
        if (patch.has("Ingredientes")) {
            JsonNode value = patch.get("Ingredientes");
            // Exige que seja um array JSON e que não esteja vazio []
            if (value.isNull() || !value.isArray() || value.isEmpty())
                throw invalid("Ingredientes deve ser uma lista não vazia de strings");
            ArrayList<String> parsed = new ArrayList<>();
            for (JsonNode item : value) { // Valida cada “item” dentro do array
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

        // Retorna o record preenchido apenas com os campos enviados (os omitidos ficam null)
        return new ReceitaCulinariaPatchInput(nome, duracao, ingredientes, tutorial);
    }

    // Método auxiliar para criar uma ApiException com status 400 (Bad Request)
    private static ApiException invalid(String message) {
        return ApiException.badRequest(message);
    }
}