package br.com.CadernoDeReceitas.Repository;

import br.com.CadernoDeReceitas.model.ReceitaCulinaria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceitaCulinariaRepository extends JpaRepository<ReceitaCulinaria, Long> {
    // O JpaRepository já herda save() findAll() findById() deleteById() etc
    // Você só declara aqui consultas personalizadas extras se precisa

    Page<ReceitaCulinaria> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}