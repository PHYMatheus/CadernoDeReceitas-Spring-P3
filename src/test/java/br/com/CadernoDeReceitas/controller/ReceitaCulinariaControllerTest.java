package br.com.CadernoDeReceitas.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReceitaCulinariaControllerTest {
    private static final String BOLO = "{\"nome\":\" Bolo de Cenoura \",\"duracaoDoPreparoMinutos\":50,"
            + "\"Ingredientes\":[\"cenoura\",\"ovo\"],\"TutorialPreparo\":\"Bata e asse\"}";
    private static final String MERGE_PATCH = "application/merge-patch+json";

    private final ObjectMapper json = new ObjectMapper();

    @LocalServerPort
    private int port;

    private HttpClient client;

    @BeforeEach
    void start() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void stop() {
        client.close();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return send(method, path, body, body == null ? null : "application/json");
    }

    private HttpResponse<String> send(String method, String path, String body, String contentType) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (contentType != null) builder.header("Content-Type", contentType);
        var request = builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode body(HttpResponse<String> response) {
        return json.readTree(response.body());
    }

    private static String receita(String nome, double duracao) {
        return "{\"nome\":\"" + nome + "\",\"duracaoDoPreparoMinutos\":" + duracao
                + ",\"Ingredientes\":[\"farinha\"],\"TutorialPreparo\":\"Asse\"}";
    }

    @Test
    void crudCompletoComStatusCorretos() throws Exception {
        var criada = send("POST", "/receitas", BOLO);
        assertEquals(201, criada.statusCode());
        String location = criada.headers().firstValue("Location").orElseThrow();
        assertEquals("/receitas/1", location);
        assertEquals("Bolo de Cenoura", body(criada).get("nome").asText());

        var buscada = send("GET", location, null);
        assertEquals(200, buscada.statusCode());
        assertEquals(50, body(buscada).get("duracaoDoPreparoMinutos").asInt());

        var atualizada = send("PUT", location, receita("Bolo de Chocolate", 60));
        assertEquals(200, atualizada.statusCode());
        assertEquals("Bolo de Chocolate", body(atualizada).get("nome").asText());

        var removida = send("DELETE", location, null);
        assertEquals(204, removida.statusCode());
        assertEquals("", removida.body());
        assertEquals(404, send("GET", location, null).statusCode());
        assertEquals(404, send("DELETE", location, null).statusCode());
        assertEquals(404, send("PUT", location, receita("Pão", 30)).statusCode());
    }

    @Test
    void listaTudoOuFiltraPorNomeSemDiferenciarMaiusculas() throws Exception {
        send("POST", "/receitas", BOLO);
        send("POST", "/receitas", receita("Pão de Queijo", 40));

        var todas = send("GET", "/receitas", null);
        assertEquals(200, todas.statusCode());
        assertEquals(2, body(todas).size());
        assertEquals(1, body(send("GET", "/receitas?nome=BOLO", null)).size());
        assertEquals(0, body(send("GET", "/receitas?nome=lasanha", null)).size());
    }

    @Test
    void errosDeValidacaoERegraDeNegocio() throws Exception {
        assertEquals(400, send("POST", "/receitas", receita(" ", 30)).statusCode());
        assertEquals(400, send("POST", "/receitas", receita("Bolo", 0)).statusCode());
        assertEquals(422, send("POST", "/receitas", receita("Feijoada", 1441)).statusCode());
        assertEquals(201, send("POST", "/receitas", receita("Pernil", 1440)).statusCode());

        for (String invalido : List.of("{quebrado", "null", "{}", "{} {}")) {
            assertEquals(400, send("POST", "/receitas", invalido).statusCode(), invalido);
        }
        assertEquals(400, send("GET", "/receitas/abc", null).statusCode());
        assertEquals(415, send("POST", "/receitas", BOLO, "text/plain").statusCode());
    }

    @Test
    void patchAlteraSoOsCamposEnviados() throws Exception {
        String location = send("POST", "/receitas", BOLO).headers().firstValue("Location").orElseThrow();

        var parcial = send("PATCH", location, "{\"nome\":\"Bolo Fofinho\"}", MERGE_PATCH);
        assertEquals(200, parcial.statusCode());
        assertEquals("Bolo Fofinho", body(parcial).get("nome").asText());
        assertEquals(50, body(parcial).get("duracaoDoPreparoMinutos").asInt());

        assertEquals(422, send("PATCH", location, "{\"duracaoDoPreparoMinutos\":2000}", MERGE_PATCH).statusCode());
        assertEquals(50, body(send("GET", location, null)).get("duracaoDoPreparoMinutos").asInt());
        assertEquals(400, send("PATCH", location, "{\"nome\":\" \"}", MERGE_PATCH).statusCode());
        assertEquals(404, send("PATCH", "/receitas/999", "{\"nome\":\"X\"}", MERGE_PATCH).statusCode());
    }

    @Test
    void rotasInexistentesEMetodoNaoPermitido() throws Exception {
        assertEquals(404, send("GET", "/inexistente", null).statusCode());
        var naoPermitido = send("POST", "/receitas/1", BOLO);
        assertEquals(405, naoPermitido.statusCode());
        assertEquals("DELETE, GET, HEAD, OPTIONS, PATCH, PUT",
                naoPermitido.headers().firstValue("Allow").orElseThrow());
    }
}
