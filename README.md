![Banner Caderno de Receitas](./assets/banner.svg)

# Caderno de Receitas — API REST (Migração para Spring Boot)

API REST desenvolvida em **Java 21** com **Spring Boot** para gerenciamento de um **Caderno de Receitas Culinárias** (Tema 3), refatorada a partir da versão original construída sobre o framework acadêmico *Mini Spring* na disciplina de **Programação 3**.

A migração preserva integralmente o domínio, o armazenamento em memória *thread-safe*, o CRUD completo, os filtros de busca, as validações estruturais, a regra de negócio (limite de 1.440 minutos) e o contrato padronizado de respostas e erros HTTP.

---

## 1. Integrantes da Equipe e Detalhamento das Responsabilidades

| Integrante | Papel Principal | Responsabilidades Detalhadas no Projeto |
| :--- | :--- | :--- |
| **Vinicius de Sousa Silva** | Model, DTOs & Contrato do Repositório | Criação da entidade `ReceitaCulinaria`, dos DTOs `ReceitaCulinariaInput` (`@NotBlank` e `@Positive`), `ReceitaCulinariaPage` (paginação) e `ReceitaCulinariaPatchInput` (atualização parcial via `JsonNode`), além da interface `ReceitaCulinariaRepository`. |
| **Emanuel Vinícius de Souza** | Repositório em Memória & Concorrência | Implementação de `InMemoryReceitaCulinariaRepository` anotado com `@Repository` (Spring), utilizando `ConcurrentHashMap`, geração sequencial de ID com `AtomicLong`, atualização atômica com `computeIfPresent` e ordenação crescente por ID. |
| **Matheus Guilherme de Paula Santos** | Service & Regra de Negócio Principal | Implementação de `ReceitaCulinariaService` (`@Service`) com injeção de `ReceitaCulinariaRepository` via construtor, validação de duração positiva (`> 0` → `400`), aplicação da regra de negócio do tema (limite máximo de 1.440 minutos → `422`) e tratamento de registro inexistente (`404`). |
| **Pedro Henrique Pereira da Silva** | Validações Estruturais, Higienização & Filtro | Implementação das validações de campos textuais (`nome` e `TutorialPreparo`), higienização com `.trim()`, validação da lista de `Ingredientes` (mínimo de 1 item e sem itens em branco → `400`) e filtro parcial por nome *case-insensitive*. |
| **Tainon Coutinho** | Controller & Mapeamento de Rotas Spring MVC | Implementação de `ReceitaCulinariaController` (`@RestController` e `@RequestMapping("/receitas")`) com injeção via construtor, mapeando `POST` (`201 Created` com cabeçalho `Location`), `GET` geral/filtrado (`200 OK`), `GET /{id}` (`200 OK`), `PUT /{id}` (`200 OK`), `PATCH /{id}` (`200 OK`) e `DELETE /{id}` (`204 No Content`). |
| **João Felipe Falcão de Lima Salsa** | Camada Web, Tratamento Global de Erros & Filtros | Implementação da camada `web` (`ApiException`, `ApiError`, `ApiExceptionHandler` com `@RestControllerAdvice`, `ApiResponses` e `RequestIdFilter` com `OncePerRequestFilter`) e configuração do bootstrap em `CadernoDeReceitasApplication`. |
| **Igor Gustavo de Brito Rocha** | Documentação & Suíte de Testes | Estruturação do repositório Spring Boot, elaboração dos testes automatizados (`ReceitaCulinariaControllerTest` e `ReceitaCulinariaServiceTest`), manutenção de `requests.http` e `receitas_postman_collection.json`, e redação final do `README.md`. |

---

## 2. Descrição do Domínio (Tema 3 — Caderno de Receitas)

Conforme a especificação do tema sorteado (**Tema 3: Caderno de receitas — Receitas culinárias**), o sistema permite cadastrar, listar, paginar, filtrar por parte do nome (desconsiderando maiúsculas e minúsculas), consultar por identificador (`id`), atualizar total ou parcialmente preservando o ID original e excluir receitas culinárias, mantendo todos os registros em memória.

### Especificação do Tema Sorteado vs. Implementação no Spring Boot

| Item do Tema Sorteado | Especificação da Atividade | Implementação no Código (`br.com.CadernoDeReceitas`) |
| :--- | :--- | :--- |
| **Tema / Domínio** | 3 — Caderno de receitas (Receitas culinárias) | Entidade `ReceitaCulinaria` e DTOs de entrada/saída |
| **Rota Base** | `/receitas` | `@RestController` + `@RequestMapping("/receitas")` em `ReceitaCulinariaController` |
| **Campo Textual Obrigatório** | `nome` | `@NotBlank String nome` (somado a `ArrayList<String> Ingredientes` e `@NotBlank String TutorialPreparo`) |
| **Campo Numérico Positivo** | `tempoPreparoMinutos` | `@Positive float duracaoDoPreparoMinutos` (`> 0`) |
| **Regra de Negócio (`422`)** | Máximo de 1.440 minutos de preparo | Validação do limite de `1440` minutos em `ReceitaCulinariaService` |

### Modelos de Dados (`Records`)

A separação entre a entidade persistida e os DTOs de entrada/paginação impede que o cliente envie ou altere o `id` manualmente nas requisições de escrita:

| Classe (`Record`) | Finalidade | Campos |
| :--- | :--- | :--- |
| **`ReceitaCulinaria`** | Entidade principal armazenada em memória e retornada nas respostas | `long id`, `String nome`, `Float duracaoDoPreparoMinutos`, `ArrayList<String> ingredientes`, `String tutorialPreparo` |
| **`ReceitaCulinariaInput`** | DTO de entrada (`@RequestBody @Valid`) para `POST` e `PUT` | `@NotBlank String nome`, `@Positive float duracaoDoPreparoMinutos`, `ArrayList<String> Ingredientes`, `@NotBlank String TutorialPreparo` |
| **`ReceitaCulinariaPatchInput`** | DTO para atualização parcial (`PATCH`), validando propriedades via `JsonNode` | `String nome`, `Float duracaoDoPreparoMinutos`, `ArrayList<String> Ingredientes`, `String TutorialPreparo` |
| **`ReceitaCulinariaPage`** | DTO de resposta paginada para listagens | `List<ReceitaCulinariaInput> items`, `int page`, `int size`, `long totalElements`, `int totalPages` |

---

## 3. Principais Mudanças na Migração (Mini Spring → Spring Boot)

Na versão anterior, a infraestrutura web, o container de injeção de dependências e o validador eram implementados manualmente dentro do pacote `br.com.minispring.framework`. Na migração para o **Spring Boot**, o framework assumiu essas responsabilidades de forma nativa:

| Aspecto Arquitetural | Como era no Mini Spring | Como ficou no Spring Boot |
| :--- | :--- | :--- |
| **Bootstrap e Servidor Web** | `MiniApplication.run(Application.class)` subindo um servidor **Jetty** configurado manualmente. | `@SpringBootApplication` em `CadernoDeReceitasApplication` com **Apache Tomcat** embutido e autoconfiguração. |
| **Container IoC e DI** | `ComponentScanner` e `ApplicationContext` próprios varrendo classes por *Reflection* com anotações customizadas (`@Service`, `@Repository`, `@RestController`). | Container oficial do **Spring Framework** (`org.springframework.stereotype.*`), mantendo a injeção de dependências exclusivamente via construtor. |
| **Roteamento e Respostas HTTP** | `DispatcherServlet` e `Router` próprios com `@Route`, `@PathVariable`, `@RequestParam` customizados e retorno via `HttpResult`. | **Spring Web MVC** (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`) e respostas tipadas com `ResponseEntity<T>`. |
| **Validação de Dados** | Classe `Validator` artesanal via *Reflection* processando anotações próprias `@NotBlank` e `@Positive`. | **Jakarta Bean Validation** (`jakarta.validation.constraints.*`) integrado ao Spring via `@Valid`, somado à validação estrutural de `PATCH` com `JsonNode`. |
| **Tratamento Global de Erros** | `try/catch` centralizado no `DispatcherServlet` capturando `HttpException`. | Camada dedicada `br.com.CadernoDeReceitas.web` com `ApiExceptionHandler` (`@RestControllerAdvice` + `@ExceptionHandler`) e `ApiException`, padronizando respostas `ApiError`. |
| **Interceptação e Logs** | `LoggingInterceptor` próprio do Mini Spring gerando `X-Request-Id` e medindo latência. | `RequestIdFilter` estendendo `OncePerRequestFilter` (`@Component`), mantendo o cabeçalho `X-Request-Id`, o log de tempo em ms e a trava de payload máximo de **1 MiB** (`413 Payload Too Large`). |
| **Testes Automatizados** | Apenas testes manuais via `requests.http`. | Inclusão de testes unitários e de integração da camada web com **JUnit 5** e **Spring Boot Test** (`ReceitaCulinariaServiceTest` e `ReceitaCulinariaControllerTest`), além de `requests.http` e coleção Postman. |

---

## 4. Regra de Negócio e Validações HTTP

Todas as validações estruturais (`400`) e a regra de negócio (`422`) são aplicadas tanto no cadastro (`POST /receitas`) quanto nas atualizações (`PUT` e `PATCH`). Caso qualquer validação falhe, uma exceção (`ApiException` ou `MethodArgumentNotValidException`) interrompe o fluxo antes de qualquer escrita no repositório, garantindo que operações rejeitadas não modifiquem os dados em memória.

| Regra / Validação | Critério Aplicado | Status HTTP | Mensagem / Comportamento |
| :--- | :--- | :--- | :--- |
| **Regra de Negócio (Tempo Máximo)** | `duracaoDoPreparoMinutos` não pode ultrapassar **1.440 minutos** (24 horas de preparo). | `422 Unprocessable Content` | `"Uma Receita pode ter no Máximo 1.440 Minutos!"` |
| **Nome obrigatório** | `nome` não pode ser nulo, vazio (`""`) ou conter apenas espaços. É salvo com `.trim()`. | `400 Bad Request` | `"Falha de validação"` (detalhe no campo `nome`) |
| **Tutorial de preparo obrigatório** | `TutorialPreparo` não pode ser nulo, vazio ou apenas espaços. É salvo com `.trim()`. | `400 Bad Request` | `"Falha de validação"` (detalhe no campo `TutorialPreparo`) |
| **Duração positiva** | `duracaoDoPreparoMinutos` deve ser estritamente maior que zero (`> 0`). | `400 Bad Request` | `"Falha de validação"` (detalhe no campo `duracaoDoPreparoMinutos`) |
| **Ingredientes obrigatórios** | A lista `Ingredientes` deve ter pelo menos 1 item e nenhum item nulo ou em branco. Cada item recebe `.trim()`. | `400 Bad Request` | `"Ingredientes deve ter pelo menos um item!"` ou `"Ingredientes não pode ter item em branco!"` |
| **Validação de ID e Parâmetros** | O `{id}` na URL deve ser um número inteiro positivo (`> 0`). | `400 Bad Request` | `"Parâmetro inválido: id"` ou mensagem de ID inválido |
| **Registro ou Rota Inexistente** | Consultas (`GET`), atualizações (`PUT`/`PATCH`) e exclusões (`DELETE`) exigem que o `id` exista; rotas desconhecidas também retornam `404`. | `404 Not Found` | `"Receita culinária não encontrada!"` ou `"Rota não encontrada"` |
| **Método HTTP não permitido** | Uso de verbo HTTP não suportado em uma rota existente. | `405 Method Not Allowed` | `"Método HTTP não permitido"` + cabeçalho `Allow` |
| **Limite de Tamanho do Corpo** | Requisições `POST`, `PUT` ou `PATCH` com payload superior a **1 MiB** (`1.048.576 bytes`). | `413 Payload Too Large` | `"Corpo excede 1 MiB"` |
| **Tipo de Mídia / JSON Inválido** | Envio sem `Content-Type: application/json` (`415`) ou JSON malformado / tipos incompatíveis (`400`). | `415` / `400` | `"Tipo de mídia não suportado"` / `"JSON inválido ou incompatível com os campos esperados"` |

---

## 5. Rotas da API (Endpoints)

Todas as rotas estão mapeadas sob `/receitas` na classe `ReceitaCulinariaController`:

| Método | Rota | Descrição | Status de Sucesso | Status de Erro Possíveis |
| :--- | :--- | :--- | :--- | :--- |
| **`POST`** | `/receitas` | Cadastra uma nova receita culinária gerando o `id` sequencial automaticamente. | `201 Created` (com cabeçalho `Location: /receitas/{id}`) | `400`, `413`, `415`, `422` |
| **`GET`** | `/receitas` | Lista todas as receitas cadastradas em ordem crescente de `id`. | `200 OK` | `405` |
| **`GET`** | `/receitas?nome={texto}` | Filtra receitas por parte do nome, desconsiderando maiúsculas e minúsculas (*case-insensitive*). | `200 OK` | `405` |
| **`GET`** | `/receitas/{id}` | Consulta uma receita culinária específica pelo seu `id`. | `200 OK` | `400`, `404`, `405` |
| **`PUT`** | `/receitas/{id}` | Atualiza todos os dados de uma receita existente preservando seu `id`. | `200 OK` | `400`, `404`, `405`, `413`, `415`, `422` |
| **`PATCH`** | `/receitas/{id}` | Atualiza parcialmente apenas os campos enviados no JSON de uma receita existente. | `200 OK` | `400`, `404`, `405`, `413`, `415`, `422` |
| **`DELETE`** | `/receitas/{id}` | Exclui uma receita existente pelo seu `id`, sem corpo na resposta. | `204 No Content` | `400`, `404`, `405` |

### Exemplo de Corpo JSON (`POST /receitas` e `PUT /receitas/{id}`)

```json
{
  "nome": "Bolo de Cenoura com Cobertura de Chocolate",
  "duracaoDoPreparoMinutos": 50.0,
  "Ingredientes": [
    "3 cenouras médias",
    "4 ovos",
    "2 xícaras de açúcar",
    "2 e 1/2 xícaras de farinha de trigo",
    "1 colher de sopa de fermento em pó"
  ],
  "TutorialPreparo": "Bata as cenouras, os ovos e o óleo no liquidificador, misture os secos e asse a 180°C por 40 minutos."
}
```

### Exemplo de Resposta de Erro Padronizada (`ApiError`)

```json
{
  "timestamp": "2026-11-19T13:00:00Z",
  "status": 422,
  "message": "Uma Receita pode ter no Máximo 1.440 Minutos!",
  "path": "/receitas",
  "details": [],
  "requestId": "c8f1d4e2-9a3b-4c12-8e7f-123456789abc"
}
```

---

## 6. Arquitetura em Camadas, IoC/DI e Caminho da Requisição

### 6.1 Responsabilidades das Camadas

* **`controller` (`ReceitaCulinariaController`)**: Camada de apresentação REST. Recebe requisições HTTP, extrai `@RequestBody`, `@PathVariable` e `@RequestParam`, delega a execução ao `ReceitaCulinariaService` e monta a resposta HTTP via `ResponseEntity` (incluindo o cabeçalho `Location` na criação).
* **`service` (`ReceitaCulinariaService`)**: Camada de regras de negócio. Valida restrições de domínio (`400` e `422`), higieniza strings com `.trim()`, executa o filtro *case-insensitive* por nome e verifica existência de registros (`404`).
* **`repository` (`ReceitaCulinariaRepository` e `InMemoryReceitaCulinariaRepository`)**: Camada de acesso a dados em memória. Define o contrato via interface e armazena os registros em `ConcurrentHashMap<Long, ReceitaCulinaria>`, gerando IDs com `AtomicLong` e preservando o ID original nas atualizações com `computeIfPresent`.
* **`web` (`RequestIdFilter`, `ApiExceptionHandler`, `ApiException`, `ApiError`, `ApiResponses`)**: Camada transversal de infraestrutura HTTP responsável por rastreabilidade (`X-Request-Id`), limite de tamanho de corpo (1 MiB), logs de latência e conversão global de exceções em JSON padronizado.

### 6.2 Inversão de Controle (IoC) e Injeção de Dependências (DI)

1. Ao executar `CadernoDeReceitasApplication.main`, o `@SpringBootApplication` ativa o *Component Scan* automático a partir do pacote `br.com.CadernoDeReceitas`.
2. O container IoC do Spring instancia e gerencia os *beans* anotados com `@Repository` (`InMemoryReceitaCulinariaRepository`), `@Service` (`ReceitaCulinariaService`), `@RestController` (`ReceitaCulinariaController`), `@RestControllerAdvice` (`ApiExceptionHandler`) e `@Component` (`RequestIdFilter`).
3. As dependências são resolvidas exclusivamente **via construtor** (sem uso de `@Autowired` em atributos), garantindo imutabilidade (`final`), baixo acoplamento (o `Service` depende da interface `ReceitaCulinariaRepository`) e facilidade para testes unitários.

### 6.3 Caminho Completo de uma Requisição HTTP

1. **Tomcat Embutido & `RequestIdFilter`**: Recebe a requisição HTTP, gera um `UUID` (`X-Request-Id`), verifica se o corpo não ultrapassa 1 MiB (`413` caso exceda) e armazena os bytes em `CachedBodyRequest` para leitura segura.
2. **`DispatcherServlet` (Spring MVC)**: Localiza o método correspondente em `ReceitaCulinariaController` (retornando `404` se a rota não existir ou `405` com header `Allow` se o verbo HTTP não for permitido).
3. **Desserialização e Bean Validation**: O Jackson converte o JSON em `ReceitaCulinariaInput` (ou `JsonNode` no `PATCH`) e o `@Valid` executa `@NotBlank` e `@Positive`.
4. **Camadas de Negócio e Dados (`Controller → Service → Repository`)**: O `ReceitaCulinariaService` valida a lista de ingredientes e o limite máximo de 1.440 minutos, interagindo com o `InMemoryReceitaCulinariaRepository`.
5. **Resposta ou Tratamento Global (`ApiExceptionHandler`)**: Em caso de sucesso, o `Controller` devolve o `ResponseEntity` (`200`, `201` ou `204`). Em caso de erro, o `@RestControllerAdvice` intercepta a exceção e retorna o `ApiError` correspondente. Por fim, o bloco `finally` do `RequestIdFilter` registra no log o método, URI, status HTTP e tempo de execução em milissegundos.

---

## 7. Estrutura de Diretórios

```text
CadernoDeReceitas-Spring-P3/
├── assets/
│   └── banner.svg
├── src/
│   ├── main/
│   │   ├── java/br/com/CadernoDeReceitas/
│   │   │   ├── CadernoDeReceitasApplication.java
│   │   │   ├── controller/
│   │   │   │   └── ReceitaCulinariaController.java
│   │   │   ├── model/
│   │   │   │   ├── ReceitaCulinaria.java
│   │   │   │   ├── ReceitaCulinariaInput.java
│   │   │   │   ├── ReceitaCulinariaPage.java
│   │   │   │   └── ReceitaCulinariaPatchInput.java
│   │   │   ├── repository/
│   │   │   │   ├── ReceitaCulinariaRepository.java
│   │   │   │   └── InMemoryReceitaCulinariaRepository.java
│   │   │   ├── service/
│   │   │   │   └── ReceitaCulinariaService.java
│   │   │   └── web/
│   │   │       ├── ApiError.java
│   │   │       ├── ApiException.java
│   │   │       ├── ApiExceptionHandler.java
│   │   │       ├── ApiResponses.java
│   │   │       └── RequestIdFilter.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/br/com/CadernoDeReceitas/
│           ├── CadernoDeReceitasApplicationTests.java
│           ├── controller/
│           │   └── ReceitaCulinariaControllerTest.java
│           └── service/
│               └── ReceitaCulinariaServiceTest.java
├── mvnw
├── mvnw.cmd
├── pom.xml
├── receitas_postman_collection.json
├── requests.http
└── README.md
```

---

## 8. Instruções de Execução e Testes

### 8.1 Pré-requisitos

* **Java 21** (JDK 21) instalado e configurado no `PATH`
* **Maven 3.8+** (opcional, pois o projeto já inclui o **Maven Wrapper** `./mvnw` e `mvnw.cmd`)

### 8.2 Como Executar a Aplicação

No terminal, dentro da pasta raiz do projeto (onde está localizado o `pom.xml`), execute:

**Linux / macOS:**
```bash
./mvnw spring-boot:run
```

**Windows (PowerShell / CMD):**
```bash
mvnw.cmd spring-boot:run
```

*(Alternativamente, caso tenha o Maven instalado globalmente, execute `mvn spring-boot:run`.)*

A API estará disponível em: **`http://localhost:8080/receitas`**

### 8.3 Como Executar os Testes Automatizados

Para rodar toda a suíte de testes unitários e de integração (`ReceitaCulinariaServiceTest` e `ReceitaCulinariaControllerTest`):

```bash
# Linux / macOS
./mvnw test

# Windows
mvnw.cmd test
```

### 8.4 Testes Manuais via `requests.http` e Postman

O repositório disponibiliza dois recursos prontos para testar todos os cenários de sucesso (`201`, `200`, `204`) e erro (`400`, `404`, `405`, `413`, `415`, `422`):
* **`requests.http`**: Arquivo executável diretamente no IntelliJ IDEA ou VS Code (extensão *REST Client*).
* **`receitas_postman_collection.json`**: Coleção completa pronta para importação no **Postman**.

---

## 9. Declaração de Uso de Inteligência Artificial

Em conformidade com as regras da avaliação, declaramos que ferramentas de Inteligência Artificial Generativa foram utilizadas como apoio para:

* Estruturação e revisão da documentação deste `README.md` de acordo com os requisitos da atividade;
* Revisão das validações e respostas HTTP do domínio.

O projeto foi desenvolvido com o apoio de ferramentas de Inteligência Artificial (IA) para esclarecimento de dúvidas, auxílio nos estudos e compreensão do código, servindo como suporte ao processo de aprendizagem e desenvolvimento, sem substituir o estudo e o entendimento dos conceitos utilizados.

Todos os integrantes do grupo revisaram e compreendem integralmente o código entregue, a divisão de responsabilidades em camadas, a injeção de dependências e o caminho percorrido por cada requisição HTTP.