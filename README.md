<p align="center">
  <img src="assets/banner.svg" alt="Caderno de Receitas — Projeto acadêmico de Programação III" width="100%"/>
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange?style=flat-square"/>
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-API%20REST-6DB33F?style=flat-square"/>
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-banco%20de%20dados-4479A1?style=flat-square"/>
  <img alt="Maven" src="https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square"/>
  <img alt="Status" src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square"/>
</p>

# Caderno de Receitas

Projeto acadêmico da disciplina de **Programação III**, do curso de **Sistemas de Informação** (Unifacol).

Este repositório é a **evolução** do projeto [CadernoDeReceitas-Project-P3](https://github.com/PHYMatheus/CadernoDeReceitas-Project-P3), que foi construído sobre o **Mini Spring**, um framework próprio com IoC, DI e roteamento por anotações. Agora a mesma API foi refatorada para o **Spring Boot**, com persistência em **MySQL**.

---

## Sumário

1. [O problema](#1-o-problema)
2. [A solução](#2-a-solução)
3. [Funcionalidades](#3-funcionalidades)
4. [Regras de negócio](#4-regras-de-negócio)
5. [Arquitetura](#5-arquitetura)
6. [Do Mini Spring para o Spring Boot](#6-do-mini-spring-para-o-spring-boot)
7. [Tecnologias](#7-tecnologias)
8. [Estrutura do projeto](#8-estrutura-do-projeto)
9. [Rotas da API](#9-rotas-da-api)
10. [Como executar](#10-como-executar)
11. [Testes](#11-testes)
12. [Uso de IA](#12-uso-de-ia)
13. [Equipe](#13-equipe)

---

## 1. O problema

Receitas costumam ficar espalhadas: caderno de papel que estraga, print no WhatsApp, aba do navegador que some, ou só na memória de quem cozinha. Resultado:

| Dificuldade | O que o usuário precisa |
|---|---|
| Perder ou não achar uma receita | Um lugar único e persistente para guardar tudo |
| Lembrar só de parte do nome | Busca por trecho do nome, sem se preocupar com maiúsculas e minúsculas |
| Receitas escritas sem padrão | Um formato padronizado: nome, tempo, ingredientes e modo de preparo |
| Dados que somem ao reiniciar o sistema | Persistência real em banco de dados |

## 2. A solução

Uma **API REST** em Java com Spring Boot que permite cadastrar, consultar, atualizar, excluir e buscar receitas, guardando tudo em um banco **MySQL**. As regras de negócio ficam concentradas na camada Service, e a API devolve respostas HTTP previsíveis (201, 200, 204, 400, 404 e 422).

## 3. Funcionalidades

- Cadastrar, listar, consultar por ID, atualizar e excluir receitas.
- Filtrar por parte do nome, ignorando maiúsculas e minúsculas.
- ID gerado automaticamente pelo banco e preservado nas atualizações.
- Validação de entrada com mensagens claras.
- Formato de erro padronizado, com identificador da requisição (`X-Request-Id`).

## 4. Regras de negócio

| Regra | Descrição |
|---|---|
| Nome | Campo textual obrigatório, sem aceitar vazio ou apenas espaços. O valor é salvo sem espaços nas pontas. |
| Tempo de preparo | Informado em **minutos**, deve ser **maior que zero** e no **máximo 1.440 minutos** (24 horas). |
| Ingredientes | A lista deve ter pelo menos um item, e nenhum item pode ser vazio. |
| Modo de preparo | Texto obrigatório, sem aceitar apenas espaços. |
| ID | Gerado pelo banco. O cliente não controla o ID, e ele é preservado na atualização. |
| Atualização | Usa as mesmas validações do cadastro. Uma operação rejeitada não altera os dados armazenados. |

Violação das regras estruturais (campo vazio, número inválido) resulta em **400**. Violação da regra de negócio (mais de 1.440 minutos) resulta em **422**.

## 5. Arquitetura

Arquitetura em camadas, cada uma com uma responsabilidade:

```
Cliente HTTP (navegador / Postman / curl)
                │
                ▼
┌───────────────────────────────┐
│  Controller  (@RestController) │  recebe HTTP, delega, devolve status
└───────────────┬───────────────┘
                ▼
┌───────────────────────────────┐
│  Service     (@Service)        │  regras de negócio e normalização
└───────────────┬───────────────┘
                ▼
┌───────────────────────────────┐
│  Repository  (Spring Data JPA) │  acesso ao banco
└───────────────┬───────────────┘
                ▼
        MySQL (receitas)
```

- **Injeção de dependências por construtor**, com interface de repositório e DTO de entrada.
- O Controller não tem regra de negócio; o Service não conhece HTTP.
- Erros tratados em um único ponto (`@RestControllerAdvice`), com corpo JSON padronizado.

## 6. Do Mini Spring para o Spring Boot

A refatoração trocou as peças do framework próprio pelas equivalentes do Spring Boot, mantendo as regras e o comportamento da API:

| Mini Spring | Spring Boot |
|---|---|
| `ApplicationContext` e `ComponentScanner` | Container IoC e component scan do Spring |
| `@Service`, `@Repository`, `@RestController` | Mesmas anotações, agora do Spring |
| `@Route(method, path)` | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` |
| `@PathVariable`, `@RequestParam`, `@RequestBody` | Mesmas anotações, agora do Spring |
| `@Value` + `AppConfig` | `application.properties` e `@Value` |
| `Validator`, `@NotBlank`, `@Positive` | Bean Validation (`@NotBlank`, `@Positive`, `@Max`, `@NotEmpty`) |
| `HttpException` + tratamento no `DispatcherServlet` | Exceções de domínio + `@RestControllerAdvice` |
| `LoggingInterceptor` (`X-Request-Id`) | Filtro (`OncePerRequestFilter`) |
| Jetty embutido | Servidor embutido do Spring Boot |
| `InMemory` com `ConcurrentHashMap` | Spring Data JPA + MySQL |
| `ApiIntegrationTest` com Jetty real | `@SpringBootTest` / `MockMvc` |

## 7. Tecnologias

- **Java 21**
- **Spring Boot** (Web, Data JPA, Validation, Actuator)
- **MySQL**
- **Maven**
- **JUnit 5** e **Spring Boot Test**
- **Docker Compose** (opcional, para subir o banco)

## 8. Estrutura do projeto

```
caderno-receitas-spring/
├── assets/
│   └── banner.svg
├── src/main/java/br/com/minispring/cadernoreceitas/
│   ├── CadernoReceitasApplication.java
│   ├── controller/      # ReceitaController
│   ├── service/         # ReceitaService (regras de negócio)
│   ├── repository/      # ReceitaRepository (JPA)
│   ├── model/           # Receita (entidade)
│   ├── dto/             # ReceitaInput, ReceitaResponse
│   └── exception/       # exceções de domínio e handler global
├── src/main/resources/
│   └── application.properties
├── src/test/java/       # testes de service e de integração
├── docker-compose.yml
├── requests.http
├── pom.xml
└── README.md
```

> A estrutura acima é a planejada e pode ser ajustada durante a refatoração.

## 9. Rotas da API

Rota base: `/receitas`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| `POST` | `/receitas` | Cadastra uma receita | `201` + header `Location` |
| `GET` | `/receitas` | Lista todas as receitas | `200` |
| `GET` | `/receitas?nome={trecho}` | Filtra por parte do nome | `200` |
| `GET` | `/receitas/{id}` | Consulta por ID | `200` |
| `PUT` | `/receitas/{id}` | Atualiza uma receita | `200` |
| `DELETE` | `/receitas/{id}` | Exclui uma receita | `204` |

### Códigos de erro

| Status | Quando acontece |
|---|---|
| `400` | Entrada inválida (nome vazio, tempo menor ou igual a zero, JSON malformado) |
| `404` | Receita inexistente |
| `422` | Regra de negócio violada (tempo acima de 1.440 minutos) |

### Exemplo de cadastro

```http
POST /receitas
Content-Type: application/json

{
  "nome": "Brigadeiro de colher",
  "duracaoDoPreparoMinutos": 15,
  "ingredientes": ["1 lata de leite condensado", "3 colheres de chocolate em pó", "1 colher de manteiga"],
  "tutorialPreparo": "Misture tudo em uma panela e cozinhe em fogo baixo, mexendo sempre, até desgrudar do fundo."
}
```

Mais exemplos de sucesso e de erro estão no arquivo [`requests.http`](requests.http).

## 10. Como executar

### Pré-requisitos

- Java 21
- Maven 3.9 ou superior
- MySQL (local ou via Docker)

### Passo a passo

```bash
# 1. Clonar o repositório
git clone <url-do-repositorio>
cd caderno-receitas-spring

# 2. Subir o MySQL (opção com Docker)
docker compose up -d

# 3. Executar a aplicação
mvn spring-boot:run
```

A API ficará disponível em `http://localhost:8080/receitas`.

### Configuração do banco

As credenciais ficam em `src/main/resources/application.properties` e podem ser sobrescritas por variáveis de ambiente (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

## 11. Testes

```bash
mvn test
```

Os testes cobrem as regras do Service (casos de sucesso e de erro) e o fluxo completo da API, verificando os status HTTP.

## 12. Uso de IA

Foi utilizada IA (Claude, da Anthropic) como apoio no projeto: explicações de conceitos, revisão de arquitetura, rascunho de documentação e sugestões de código. Todo o código entregue foi revisado, e cada integrante é responsável por compreender e explicar o que foi desenvolvido.

## 13. Equipe

| Nome | Responsabilidade |
|---|---|
| Matheus | Desenvolvimento e arquitetura |
| Igor | Desenvolvimento e arquitetura |
| Vinicius | *(responsabilidade)* |

**Disciplina:** Programação III
**Curso:** Sistemas de Informação
**Professor:** Alisson Ferreira
**Instituição:** Unifacol
**Período/Ano:** *(semestre/ano)*
