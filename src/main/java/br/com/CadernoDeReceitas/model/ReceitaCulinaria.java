package br.com.CadernoDeReceitas.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceitaCulinaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Float duracaoDoPreparoMinutos;

    @ElementCollection
    @CollectionTable(
            name = "receita_culinaria_ingredientes",
            joinColumns = @JoinColumn(name = "receita_culinaria_id")
    )
    @Column(name = "ingredientes", nullable = false)
    private List<String> ingredientes;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String tutorialPreparo;
}

