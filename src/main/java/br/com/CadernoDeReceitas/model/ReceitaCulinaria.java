package br.com.CadernoDeReceitas.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;

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
    @Column(nullable = false)
    private ArrayList<String> ingredientes;
    @Column(nullable = false)
    private ArrayList<String> tutorialPreparo;
}

