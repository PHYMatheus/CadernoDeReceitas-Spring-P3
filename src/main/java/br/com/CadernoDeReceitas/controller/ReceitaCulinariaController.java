package br.com.CadernoDeReceitas.controller;

import br.com.CadernoDeReceitas.dto.ReceitaCulinariaPatchDTO;
import br.com.CadernoDeReceitas.dto.ReceitaCulinariaRequestDTO;
import br.com.CadernoDeReceitas.dto.ReceitaCulinariaResponseDTO;
import br.com.CadernoDeReceitas.service.ReceitaCulinariaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/receitas")
public class ReceitaCulinariaController {

    private final ReceitaCulinariaService service;

    public ReceitaCulinariaController(ReceitaCulinariaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReceitaCulinariaResponseDTO> criar(
            @RequestBody @Valid ReceitaCulinariaRequestDTO dto,
            UriComponentsBuilder uriBuilder) {
        ReceitaCulinariaResponseDTO response = service.create(dto);
        URI uri = uriBuilder.path("/receitas/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    // Único endpoint GET para listar (com suporte opcional a filtro por nome e paginação)
    @GetMapping
    public ResponseEntity<Page<ReceitaCulinariaResponseDTO>> listar(
            @RequestParam(required = false) String nome,
            @PageableDefault(page = 0, size = 10, sort = "nome") Pageable pageable) {

        Page<ReceitaCulinariaResponseDTO> pagina = service.listarPaginado(nome, pageable);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceitaCulinariaResponseDTO> buscarPorId(@PathVariable Long id) {
        ReceitaCulinariaResponseDTO response = service.findById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReceitaCulinariaResponseDTO> atualizarParcialmente(
            @PathVariable Long id,
            @RequestBody ReceitaCulinariaPatchDTO dto) {
        ReceitaCulinariaResponseDTO response = service.update(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}