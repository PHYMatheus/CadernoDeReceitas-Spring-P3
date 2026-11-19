package br.com.CadernoDeReceitas.controller;

import br.com.CadernoDeReceitas.model.ReceitaCulinaria;
import br.com.CadernoDeReceitas.model.ReceitaCulinariaInput;
import br.com.CadernoDeReceitas.model.ReceitaCulinariaPatchInput;
import br.com.CadernoDeReceitas.service.ReceitaCulinariaService;
import br.com.CadernoDeReceitas.web.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/receitas")
public final class ReceitaCulinariaController {
    private final ReceitaCulinariaService service;

    public ReceitaCulinariaController(ReceitaCulinariaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReceitaCulinaria> listar(@RequestParam(defaultValue = "") String nome) {
        if (nome.isBlank()) {
            return service.listAll();
        }
        return service.listByName(nome);
    }

    @GetMapping("/{id}")
    public ReceitaCulinaria buscarPorId(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReceitaCulinaria> criar(@Valid @RequestBody ReceitaCulinariaInput input) {
        ReceitaCulinaria criada = service.create(input);
        return ResponseEntity.created(URI.create("/receitas/" + criada.id())).body(criada);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReceitaCulinaria atualizar(@PathVariable long id, @Valid @RequestBody ReceitaCulinariaInput input) {
        return service.update(id, input);
    }

    @PatchMapping(path = "/{id}", consumes = "application/merge-patch+json")
    public ReceitaCulinaria atualizarParcial(@PathVariable long id, @RequestBody JsonNode body) {
        return service.patch(id, ReceitaCulinariaPatchInput.from(body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> opcoes() {
        return ApiResponses.options("GET, HEAD, OPTIONS, POST");
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> opcoesDaReceita() {
        return ApiResponses.options("DELETE, GET, HEAD, OPTIONS, PATCH, PUT");
    }
}
