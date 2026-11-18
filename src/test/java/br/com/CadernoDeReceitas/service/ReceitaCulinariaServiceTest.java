package br.com.CadernoDeReceitas.service;

import br.com.CadernoDeReceitas.model.ReceitaCulinaria;
import br.com.CadernoDeReceitas.model.ReceitaCulinariaInput;
import br.com.CadernoDeReceitas.model.ReceitaCulinariaPatchInput;
import br.com.CadernoDeReceitas.repository.InMemoryReceitaCulinariaRepository;
import br.com.CadernoDeReceitas.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReceitaCulinariaServiceTest {
    private ReceitaCulinariaService service;

    @BeforeEach
    void setUp() {
        service = new ReceitaCulinariaService(new InMemoryReceitaCulinariaRepository());
    }

    private static ReceitaCulinariaInput input(String nome, float duracao, List<String> ingredientes, String tutorial) {
        return new ReceitaCulinariaInput(nome, duracao, ingredientes == null ? null : new ArrayList<>(ingredientes), tutorial);
    }

    private static ReceitaCulinariaInput valida() {
        return input("  Bolo de Cenoura  ", 50, List.of(" 3 cenouras ", "4 ovos"), "  Bata e asse.  ");
    }

    private static void assertStatus(int status, String message, Runnable action) {
        ApiException ex = assertThrows(ApiException.class, action::run);
        assertEquals(status, ex.status());
        if (message != null) assertEquals(message, ex.getMessage());
    }

    @Test
    void createGeraIdSequencialEAplicaTrim() {
        ReceitaCulinaria criada = service.create(valida());
        assertEquals(1, criada.id());
        assertEquals("Bolo de Cenoura", criada.nome());
        assertEquals(List.of("3 cenouras", "4 ovos"), criada.ingredientes());
        assertEquals("Bata e asse.", criada.tutorialPreparo());
        assertEquals(2, service.create(valida()).id());
    }

    @Test
    void aceitaLimiteExatoDe1440() {
        assertDoesNotThrow(() -> service.create(input("Pernil", 1440, List.of("pernil"), "Asse.")));
    }

    @Test
    void validacoesEstruturaisRetornam400() {
        assertStatus(400, "nome não pode estar em branco!", () -> service.create(input("   ", 30, List.of("ovo"), "Frite.")));
        assertStatus(400, "TutorialPreparo não pode estar em branco!", () -> service.create(input("Omelete", 15, List.of("ovo"), "  ")));
        assertStatus(400, "duracaoDoPreparoMinutos deve ser maior que zero!", () -> service.create(input("X", 0, List.of("água"), "Sirva.")));
        assertStatus(400, "duracaoDoPreparoMinutos deve ser maior que zero!", () -> service.create(input("X", -15, List.of("água"), "Sirva.")));
        assertStatus(400, "Ingredientes deve ter pelo menos um item!", () -> service.create(input("X", 20, List.of(), "Nada.")));
        assertStatus(400, "Ingredientes deve ter pelo menos um item!", () -> service.create(input("X", 20, null, "Nada.")));
        assertStatus(400, "Ingredientes não pode ter item em branco!", () -> service.create(input("X", 10, List.of("tomate", "  "), "Corte.")));
        assertStatus(400, "Dados da Receita são obrigatórios!", () -> service.create(null));
        assertTrue(service.listAll().isEmpty(), "operações rejeitadas não podem gravar nada");
    }

    @Test
    void regraDeNegocioRetorna422NoCreateENoUpdate() {
        String msg = "Uma Receita pode ter no Máximo 1.440 Minutos!";
        assertStatus(422, msg, () -> service.create(input("Carne", 1441, List.of("alcatra"), "Mature.")));
        ReceitaCulinaria criada = service.create(valida());
        assertStatus(422, msg, () -> service.update(criada.id(), input("Bolo", 2000, List.of("cenoura"), "Asse.")));
        assertEquals(criada, service.findById(criada.id()), "update rejeitado não altera os dados");
    }

    @Test
    void listaEFiltraPorNomeSemDiferenciarMaiusculas() {
        service.create(valida());
        service.create(input("Lasanha à Bolonhesa", 90, List.of("massa"), "Monte."));
        assertEquals(2, service.listAll().size());
        assertEquals(1, service.listByName("CENOURA").size());
        assertEquals(1, service.listByName("cenoura").size());
        assertEquals(2, service.listByName("bolo").size()); // "Bolonhesa" também contém "bolo"
        assertTrue(service.listByName("sushi").isEmpty());
    }

    @Test
    void findUpdateDeletePreservamIdETratam404E400() {
        ReceitaCulinaria criada = service.create(valida());
        ReceitaCulinaria atualizada = service.update(criada.id(), input("Bolo Especial", 65, List.of("cenoura"), "Asse."));
        assertEquals(criada.id(), atualizada.id());
        assertEquals("Bolo Especial", service.findById(criada.id()).nome());

        String notFound = "Receita culinária não encontrada!";
        assertStatus(404, notFound, () -> service.findById(9999L));
        assertStatus(404, notFound, () -> service.update(9999L, valida()));
        assertStatus(404, notFound, () -> service.deleteById(9999L));
        assertStatus(400, "ID inválido!", () -> service.findById(0L));
        assertStatus(400, "ID inválido!", () -> service.findById(-5L));
        assertStatus(400, "ID inválido!", () -> service.findById(null));

        service.deleteById(criada.id());
        assertStatus(404, notFound, () -> service.findById(criada.id()));
    }

    @Test
    void patchAlteraSoOsCamposEnviados() {
        ReceitaCulinaria criada = service.create(valida());
        ReceitaCulinaria alterada = service.patch(criada.id(), new ReceitaCulinariaPatchInput("  Bolo Novo ", null, null, null));
        assertEquals("Bolo Novo", alterada.nome());
        assertEquals(criada.duracaoDoPreparoMinutos(), alterada.duracaoDoPreparoMinutos());
        assertEquals(criada.ingredientes(), alterada.ingredientes());
        assertStatus(422, null, () -> service.patch(criada.id(), new ReceitaCulinariaPatchInput(null, 1441f, null, null)));
        assertStatus(404, null, () -> service.patch(9999L, new ReceitaCulinariaPatchInput("X", null, null, null)));
    }
}
