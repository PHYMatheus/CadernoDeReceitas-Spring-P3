package br.com.CadernoDeReceitas.service;

import br.com.CadernoDeReceitas.dto.ReceitaCulinariaPatchDTO;
import br.com.CadernoDeReceitas.dto.ReceitaCulinariaRequestDTO;
import br.com.CadernoDeReceitas.dto.ReceitaCulinariaResponseDTO;
import br.com.CadernoDeReceitas.model.ReceitaCulinaria;
import br.com.CadernoDeReceitas.Repository.ReceitaCulinariaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ReceitaCulinariaService {

    private static final int DURACAO_MAXIMA = 1440;
    private final ReceitaCulinariaRepository repository;

    public ReceitaCulinariaService(ReceitaCulinariaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ReceitaCulinariaResponseDTO create(ReceitaCulinariaRequestDTO input) {
        validateReceita(input);

        ReceitaCulinaria receita = ReceitaCulinaria.builder()
                .nome(input.nome().trim())
                .duracaoDoPreparoMinutos(input.duracaoDoPreparoMinutos())
                .ingredientes(cleanIngredients(input.ingredientes()))
                .tutorialPreparo(input.tutorialPreparo().trim())
                .build();

        ReceitaCulinaria salva = repository.save(receita);
        return converterParaResponse(salva);
    }

    // Listar paginado (com ou sem filtro de nome)
    public Page<ReceitaCulinariaResponseDTO> listarPaginado(String nome, Pageable pageable) {
        Page<ReceitaCulinaria> paginaEntities;

        if (nome != null && !nome.isBlank()) {
            String filter = nome.trim().toLowerCase(Locale.ROOT);
            paginaEntities = repository.findByNomeContainingIgnoreCase(filter, pageable);
        } else {
            paginaEntities = repository.findAll(pageable);
        }

        return paginaEntities.map(this::converterParaResponse);
    }

    // Sobrecarga caso queira chamar apenas passando o pageable
    public Page<ReceitaCulinariaResponseDTO> listarPaginado(Pageable pageable) {
        return listarPaginado(null, pageable);
    }

    public ReceitaCulinariaResponseDTO findById(Long id) {
        validateID(id);
        ReceitaCulinaria receita = repository.findById(id).orElseThrow(this::notFound);
        return converterParaResponse(receita);
    }

    @Transactional
    public ReceitaCulinariaResponseDTO update(Long id, ReceitaCulinariaPatchDTO input) {
        validateID(id);

        ReceitaCulinaria receita = repository.findById(id).orElseThrow(this::notFound);

        if (input.nome() != null) {
            validateText(input.nome(), "nome");
            receita.setNome(input.nome().trim());
        }
        if (input.duracaoDoPreparoMinutos() != null) {
            validatePositive(input.duracaoDoPreparoMinutos());
            validateBusinessRule(input.duracaoDoPreparoMinutos());
            receita.setDuracaoDoPreparoMinutos(input.duracaoDoPreparoMinutos());
        }
        if (input.ingredientes() != null) {
            validateIngredients(input.ingredientes());
            // Limpa os ingredientes antigos mantendo a referência da coleção do Hibernate
            receita.getIngredientes().clear();
            // Adiciona os novos ingredientes limpos
            receita.getIngredientes().addAll(cleanIngredients(input.ingredientes()));
        }
        if (input.tutorialPreparo() != null) {
            validateText(input.tutorialPreparo(), "tutorialPreparo");
            receita.setTutorialPreparo(input.tutorialPreparo().trim());
        }

        ReceitaCulinaria atualizada = repository.save(receita);
        return converterParaResponse(atualizada);
    }

    @Transactional
    public void deleteById(Long id) {
        validateID(id);
        if (!repository.existsById(id)) {
            throw notFound();
        }
        repository.deleteById(id);
    }

    // ==== Validações e Regras de Negócio ====

    private void validateBusinessRule(float duracao) {
        if (duracao > DURACAO_MAXIMA) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Uma Receita pode ter no Máximo 1.440 Minutos!");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Receita culinária não encontrada!");
    }

    private void validateID(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID inválido!");
        }
    }

    private void validateNull(ReceitaCulinariaRequestDTO input) {
        if (input == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da Receita são obrigatórios!");
        }
    }

    private List<String> cleanIngredients(List<String> ingredientes) {
        return ingredientes.stream().map(String::trim).toList();
    }

    private void validatePositive(float duracao) {
        if (!(duracao > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duracaoDoPreparoMinutos deve ser maior que zero!");
        }
    }

    private void validateText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " não pode estar em branco!");
        }
    }

    private void validateIngredients(List<String> ingredientes) {
        if (ingredientes == null || ingredientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredientes deve ter pelo menos um item!");
        }
        if (ingredientes.stream().anyMatch(i -> i == null || i.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredientes não pode ter item em branco!");
        }
    }

    private void validateReceita(ReceitaCulinariaRequestDTO input) {
        validateNull(input);
        validateText(input.nome(), "nome");
        validateText(input.tutorialPreparo(), "tutorialPreparo");
        validateIngredients(input.ingredientes());
        validatePositive(input.duracaoDoPreparoMinutos());
        validateBusinessRule(input.duracaoDoPreparoMinutos());
    }

    private ReceitaCulinariaResponseDTO converterParaResponse(ReceitaCulinaria receita) {
        return new ReceitaCulinariaResponseDTO(
                receita.getId(),
                receita.getNome(),
                receita.getDuracaoDoPreparoMinutos(),
                receita.getIngredientes(),
                receita.getTutorialPreparo()
        );
    }
}