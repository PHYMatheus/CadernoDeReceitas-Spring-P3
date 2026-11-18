<p align="center">
  <img src="assets/banner.svg" alt="Caderno de Receitas — Projeto acadêmico de Programação III" width="100%"/>
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange?style=flat-square"/>
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?style=flat-square"/>
  <img alt="Persistência em memória" src="https://img.shields.io/badge/persist%C3%AAncia-em%20mem%C3%B3ria-4479A1?style=flat-square"/>
  <img alt="Maven" src="https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square"/>
  <img alt="Status" src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square"/>
</p>

# Caderno de Receitas

Projeto acadêmico da disciplina de **Programação III**, do curso de **Sistemas de Informação** (Unifacol).

Este repositório é a **evolução** do projeto [CadernoDeReceitas-Project-P3](https://github.com/PHYMatheus/CadernoDeReceitas-Project-P3), que foi construído sobre o **Mini Spring**, um framework próprio com IoC, DI e roteamento por anotações. Agora a mesma API foi refatorada para o **Spring Boot**, mantendo as mesmas regras, as mesmas mensagens e a mesma persistência **em memória** do projeto original, como pedido pelo professor.

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
| Perder ou não achar uma receita | Um lugar único para guardar e consultar tudo |
| Lembrar só de parte do nome | Busca por trecho do nome, sem se preocupar com maiúsculas e minúsculas |
| Receitas escritas sem padrão | Um formato padronizado: nome, tempo, ingredientes e modo de preparo |

## 2. A solução

Uma **API REST** em Java com Spring Boot que permite cadastrar, consultar, atualizar, excluir e buscar receitas. Os dados ficam **em memória** (`ConcurrentHashMap`), como no projeto original, e são perdidos quando a aplicação é reiniciada. As regras de negócio ficam concentradas na camada Service, e a API devolve respostas HTTP previsíveis (201, 200, 204, 400, 404 e 422).

## 3. Funcionalidades

- Cadastrar, listar, consultar por ID, atualizar e excluir receitas.
- Filtrar por parte do nome, ignorando maiúsculas e minúsculas.
- ID gerado automaticamente em sequência (`AtomicLong`) e preservado nas atualizações.
- Validação de entrada com mensagens claras.
- Formato de erro padronizado, com identificador da requisição (`X-Request-Id`).
- Limite de 1 MiB no corpo das requisições (`413`).

## 4. Regras de negócio

As regras ficam no `ReceitaCulinariaService` e são aplicadas da mesma forma no cadastro e na atualização. Toda validação acontece **antes** de chamar o repositório, então uma operação rejeitada não altera os dados armazenados.

| Regra | Critério | Status | Mensagem |
|---|---|---|---|
| Nome obrigatório | Não aceita nulo, vazio ou só espaços. É salvo sem espaços nas pontas (`trim`). | `400` | `nome não pode estar em branco!` |
| Modo de preparo obrigatório | `TutorialPreparo` não aceita nulo, vazio ou só espaços. É salvo com `trim`. | `400` | `TutorialPreparo não pode estar em branco!` |
| Ingredientes | A lista deve ter pelo menos um item, e nenhum item pode ser vazio. Cada item é salvo com `trim`. | `400` | `Ingredientes deve ter pelo menos um item!` ou `Ingredientes não pode ter item em branco!` |
| Tempo positivo | `duracaoDoPreparoMinutos` deve ser maior que zero. | `400` | `duracaoDoPreparoMinutos deve ser maior que zero!` |
| **Tempo máximo (regra do tema)** | `duracaoDoPreparoMinutos` pode ser no máximo **1.440 minutos** (24 horas). O valor 1.440 é aceito. | `422` | `Uma Receita pode ter no Máximo 1.440 Minutos!` |
| ID válido | O ID deve ser maior que zero. | `400` | `ID inválido!` |
| Receita existente | Consulta, atualização e exclusão exigem que o ID exista. | `404` | `Receita culinária não encontrada!` |

As validações são feitas nesta ordem: nome, TutorialPreparo, Ingredientes, tempo positivo e tempo máximo.

## 5. Arquitetura

Arquitetura em camadas, cada uma com uma responsabilidade:

```
Cliente HTTP (navegador / Postman / curl)
                │
                ▼
┌────────────────────────────────┐
│  RequestIdFilter               │  gera X-Request-Id, registra log, limita corpo a 1 MiB
└───────────────┬────────────────┘
                ▼
┌────────────────────────────────┐
│  Controller  (@RestController) │  recebe HTTP, delega, devolve status
└───────────────┬────────────────┘
                ▼
┌────────────────────────────────┐
│  Service     (@Service)        │  regras de negócio, validações e normalização
└───────────────┬────────────────┘
                ▼
┌────────────────────────────────┐
│  Repository  (@Repository)     │  armazenamento em memória
└───────────────┬────────────────┘
                ▼
     ConcurrentHashMap + AtomicLong

Erros em qualquer camada → ApiExceptionHandler (@RestControllerAdvice) → JSON padronizado
```

- **Injeção de dependências por construtor**: o Spring injeta `InMemoryReceitaCulinariaRepository` no Service pela interface `ReceitaCulinariaRepository`.
- O Controller não tem regra de negócio. O Service concentra as validações e lança `ApiException` com o status (400, 404 ou 422).
- Os erros são tratados em um único ponto (`ApiExceptionHandler`), sempre com o mesmo formato:

```json
{
  "timestamp": "2026-11-18T14:00:00Z",
  "status": 422,
  "message": "Uma Receita pode ter no Máximo 1.440 Minutos!",
  "path": "/receitas",
  "details": [],
  "requestId": "c8f1d4e2-9a3b-4c12-8e7f-123456789abc"
}
```

## 6. Do Mini Spring para o Spring Boot

A refatoração trocou as peças do framework próprio pelas equivalentes do Spring Boot, mantendo as regras e o comportamento da API:

| Mini Spring | Spring Boot |
|---|---|
| `ApplicationContext` e `ComponentScanner` | Container IoC e component scan do Spring |
| `@Service`, `@Repository`, `@RestController` | Mesmas anotações, agora do Spring |
| `@Route(method, path)` | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` |
| `@PathVariable`, `@RequestParam`, `@RequestBody` | Mesmas anotações, agora do Spring |
| `Validator`, `@NotBlank`, `@Positive` | Bean Validation (`jakarta.validation`: `@NotBlank`, `@Positive`) + validações no Service |
| `HttpException(status, mensagem)` + tratamento no `DispatcherServlet` | `ApiException(status, mensagem)` + `ApiExceptionHandler` (`@RestControllerAdvice`) |
| `LoggingInterceptor` (`X-Request-Id`) | `RequestIdFilter` (`OncePerRequestFilter`) |
| Jetty embutido | Servidor embutido do Spring Boot |
| `InMemoryReceitaCulinariaRepository` com `ConcurrentHashMap` | **Mantido**: `InMemoryReceitaCulinariaRepository` com `ConcurrentHashMap` e `AtomicLong` |
| Testes com JUnit 5 e Jetty real | JUnit 5 + Spring Boot Test |

## 7. Tecnologias

- **Java 21**
- **Spring Boot 4.1.2**: Web MVC, Validation, Actuator e DevTools
- **Lombok**
- **Maven**, com Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **JUnit 5** e **Spring Boot Test**

## 8. Estrutura do projeto

```
CadernoDeReceitas-SpringBoot/
├── assets/
│   └── banner.svg
├── src/main/java/br/com/CadernoDeReceitas/
│   ├── CadernoDeReceitasApplication.java
│   ├── controller/
│   │   └── ReceitaCulinariaController.java
│   ├── service/
│   │   └── ReceitaCulinariaService.java          # regras de negócio
│   ├── repository/
│   │   ├── ReceitaCulinariaRepository.java       # interface
│   │   └── InMemoryReceitaCulinariaRepository.java
│   ├── model/
│   │   ├── ReceitaCulinaria.java                 # entidade (record)
│   │   ├── ReceitaCulinariaInput.java            # entrada do POST e do PUT
│   │   ├── ReceitaCulinariaPatchInput.java       # entrada da atualização parcial
│   │   └── ReceitaCulinariaPage.java
│   └── web/
│       ├── ApiException.java
│       ├── ApiError.java                         # formato padrão de erro
│       ├── ApiExceptionHandler.java              # @RestControllerAdvice
│       ├── ApiResponses.java
│       └── RequestIdFilter.java                  # X-Request-Id, log e limite de 1 MiB
├── src/main/resources/
│   └── application.properties
├── src/test/java/br/com/CadernoDeReceitas/
│   ├── CadernoDeReceitasApplicationTests.java
│   └── service/
│       └── ReceitaCulinariaServiceTest.java
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw / mvnw.cmd
├── pom.xml
└── README.md
```

## 9. Rotas da API

> **Situação atual:** o `ReceitaCulinariaController` ainda está sendo implementado. As rotas abaixo são o contrato da API, o mesmo do projeto original, e o Service já tem um método pronto para cada uma.

Rota base: `/receitas`

| Método | Rota | Descrição | Método do Service | Sucesso | Erros possíveis |
|---|---|---|---|---|---|
| `POST` | `/receitas` | Cadastra uma receita | `create` | `201` + header `Location: /receitas/{id}` | `400`, `413`, `415`, `422` |
| `GET` | `/receitas` | Lista todas as receitas, em ordem de ID | `listAll` | `200` | — |
| `GET` | `/receitas?nome={trecho}` | Filtra por parte do nome | `listByName` | `200` | — |
| `GET` | `/receitas/{id}` | Consulta por ID | `findById` | `200` | `400`, `404` |
| `PUT` | `/receitas/{id}` | Atualiza uma receita, preservando o ID | `update` | `200` | `400`, `404`, `413`, `415`, `422` |
| `DELETE` | `/receitas/{id}` | Exclui uma receita | `deleteById` | `204` | `400`, `404` |

O Service também tem o método `patch`, pronto para uma atualização parcial (só os campos enviados), com as mesmas validações e a mesma regra dos 1.440 minutos.

### Códigos de erro

| Status | Quando acontece |
|---|---|
| `400` | Entrada inválida (campo vazio, tempo menor ou igual a zero, lista de ingredientes vazia, ID inválido, JSON malformado) |
| `404` | Receita inexistente ou rota inexistente |
| `405` | Método HTTP não permitido na rota (com header `Allow`) |
| `413` | Corpo da requisição maior que 1 MiB |
| `415` | `Content-Type` diferente de `application/json` |
| `422` | Regra de negócio violada (tempo acima de 1.440 minutos) |
| `500` | Erro inesperado (sem expor detalhes internos) |

### Exemplo de cadastro

```http
POST /receitas
Content-Type: application/json

{
  "nome": "Brigadeiro de colher",
  "duracaoDoPreparoMinutos": 15,
  "Ingredientes": ["1 lata de leite condensado", "3 colheres de chocolate em pó", "1 colher de manteiga"],
  "TutorialPreparo": "Misture tudo em uma panela e cozinhe em fogo baixo, mexendo sempre, até desgrudar do fundo."
}
```

> Os campos `Ingredientes` e `TutorialPreparo` começam com letra maiúscula, igual ao projeto original.

## 10. Como executar

### Pré-requisitos

- Java 21
- Não é preciso instalar o Maven: o projeto usa o Maven Wrapper (`mvnw`), que baixa a versão correta (3.9.16) sozinho.
- Não é preciso banco de dados.

### Passo a passo

```bash
# 1. Clonar o repositório
git clone https://github.com/PHYMatheus/CadernoDeReceitas-Spring-P3.git
cd CadernoDeReceitas-Spring-P3

# 2. Executar a aplicação (Linux, macOS ou Git Bash)
./mvnw spring-boot:run

# No Windows (CMD ou PowerShell)
mvnw.cmd spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. A saúde da aplicação pode ser verificada em `http://localhost:8080/actuator/health`.

As receitas ficam em memória e são apagadas quando a aplicação é reiniciada.

## 11. Testes

```bash
./mvnw test
```

| Teste | O que verifica |
|---|---|
| `ReceitaCulinariaServiceTest` | As regras do Service: cadastro com `trim` e ID sequencial, limite exato de 1.440 minutos, erros `400`, `404` e `422` com as mensagens corretas, filtro por nome sem diferenciar maiúsculas, preservação do ID na atualização, atualização parcial (`patch`) e a garantia de que uma operação rejeitada não altera os dados. |
| `CadernoDeReceitasApplicationTests` | Se a aplicação Spring sobe corretamente (`contextLoads`). |

## 12. Uso de IA

Foi utilizada IA (Claude, da Anthropic) como apoio no projeto: explicações de conceitos, revisão de arquitetura, rascunho de documentação e sugestões de código. Todo o código entregue foi revisado, e cada integrante é responsável por compreender e explicar o que foi desenvolvido.

## 13. Equipe

| Nome | Responsabilidade |
|---|---|
| Matheus | Desenvolvimento e Arquitetura |
| Igor | Desenvolvimento e Arquitetura |
| Vinicius | Desenvolvimento e Arquitetura |
| Pedro Henrique | Desenvolvimento e Arquitetura |
| Tainon | Desenvolvimento e Arquitetura |
| João Felipe | Desenvolvimento e Arquitetura |
| Emanuel Vinicius | Desenvolvimento e Arquitetura |

**Disciplina:** Programação III
**Curso:** Sistemas de Informação
**Professor:** Alisson Ferreira
**Instituição:** Unifacol
**Período/Ano:** *(semestre/ano)*
