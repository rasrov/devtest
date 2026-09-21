# Similar Products — Dev Test

> Microservicio de recomendación de productos similares, construido con arquitectura hexagonal multimódulo sobre Spring Boot.

**Repositorio público:** [github.com/rasrov/devtest](https://github.com/rasrov/devtest)

---

## Tabla de contenidos

- [📦 Descripción](#-descripción)
- [🏃 Arranque](#-arranque)
- [🤖 Testing](#-testing)
- [📖 Decisiones técnicas](#-decisiones-técnicas)
  - [Arquitectura general](#arquitectura-general)
  - [Capa de infraestructura — Controllers](#capa-de-infraestructura--controllers)
  - [Capa de infraestructura — APIs externas](#capa-de-infraestructura--apis-externas)
  - [Capa de aplicación — Casos de uso](#capa-de-aplicación--casos-de-uso)
  - [Capas boot / domain / common](#capas-boot--domain--common)
- [🚀 Mejoras identificadas](#-mejoras-identificadas)

---

## 📦 Descripción

`Similar Products` es un microservicio que expone la información de productos similares a partir de un identificador de producto. Orquesta y adapta los datos obtenidos de una API externa hacia un dominio de presentación propio.

---

## 🏃 Arranque

Con el repositorio ya descargado, levanta la aplicación desde el path `~/devtest/code`:

```bash
cd boot
mvn spring-boot:run
```

> **Nota sobre profiles:** la aplicación no está configurada por profiles, por lo que **no es necesario especificar ninguno**. Arranca por defecto con el profile `local`.

---

## 🤖 Testing

Con la aplicación levantada, desde el mismo path `~/devtest/code`, ejecuta la suite completa de tests:

```bash
mvn clean package
```

El proyecto incluye **tests unitarios** y **tests de integración**.

### Enfoque de los tests de integración

Los tests de integración están orientados a **validar la `response` entity de la API expuesta**, más que a validar la integración entre todas las capas del proyecto.

| Alternativa | Estado |
| ----------- | ------ |
| Validación de integración entre capas vía **e2e de Karate** | ❌ No implementado |
| Cobertura robusta mediante **tests de mutación (pitest)** | ❌ No implementado |

---

## 📖 Decisiones técnicas

### Arquitectura general

Se ha optado por una **arquitectura hexagonal (`ports & adapters`) multimódulo**, para separar responsabilidades por capas y mantener dependencias claras entre ellas.

---

### Capa de infraestructura — Controllers

Partiendo de la capa más exterior, los controllers están construidos sobre **Spring Web**, usando las anotaciones RESTful. Residen en el módulo `infrastructure`, bajo el package `input/rest`. Actualmente se expone **un único endpoint GET**.

**Mejoras aplicadas:**

- **Convención de nombrado REST:** el endpoint solicitado era `/product/{id}/similar`, pero siguiendo las convenciones REST se ha cambiado a `/products/{id}/similar` (en plural), tanto en el path del controller como en el `test.js`.
- **Manejo de excepciones:** existe una capa *handler exception* que captura las excepciones de la aplicación, las redirecciona y las encapsula en su verbo HTTP correspondiente.

**Mejora identificada (no aplicada):**

- **Versionado del endpoint** estilo `/v1/products/{id}/similar`.

---

### Capa de infraestructura — APIs externas

Módulo adicional dedicado exclusivamente a las llamadas a **APIs externas**: definición de los beans de configuración de los distintos `RestClients` e implementación de las llamadas.

**Mejoras aplicadas:**

- **Configuración de RestClients:** parametrizados vía `application.yaml` del módulo `boot`. Los *paths* residen dentro de las implementaciones de los `Port` (podrían externalizarse a properties si fuera necesario). La configuración del `RestClient` es básica, con un **timeout de 3s**.
- **Resiliencia con Circuit Breakers:** tanto los `Retry` como los `CircuitBreaker` se configuran vía properties dentro del módulo de cada API externa. Esto permite abrir el circuit breaker contra una API externa si se detecta inestabilidad o latencia excesiva.
- **Sistema de fallback:** cuando el circuit breaker se abre y se dejan de enviar peticiones a la API externa, entra en juego un mecanismo de fallback.
- **Caché con Redis:** para evitar relanzar peticiones idénticas al servicio externo. Configuración básica y prácticamente por defecto.

**Servicios adicionales para la gestión del Adapter:**

- **Servicio `Invoker` (wrapper):** centraliza la traducción de excepciones externas a excepciones de dominio, añadiendo además decoraciones. Al ser un servicio, la funcionalidad queda centralizada y disponible para toda la aplicación.
- **Traducción de excepciones (mapper adhoc):** clase mapper que gestiona de forma transparente los distintos verbos HTTP devueltos por los servicios externos y los transforma en excepciones de dominio.
- **Mapeo con MapStruct:** para el mapeo automático de DTOs/Entities externos a entidades del dominio propio.

---

### Capa de aplicación — Casos de uso

Contiene los **casos de uso**, que actúan como orquestadores y decoradores del "negocio" y "dominio" sobre la API externa consumida. En este caso la lógica es sencilla.

**Mejoras aplicadas:**

- **Executor para paralelización:** se ha definido un executor que permite paralelizar llamadas mediante `CompletableFuture`. Se ha expuesto como **servicio reutilizable**, evitando que quede encapsulado dentro de flujos a los que no pertenece.

---

### Capas boot / domain / common

Son capas definidas por defecto, con una excepción:

- **`common`:** se usa como módulo de **configuración de dependencias y servicios globales** de la aplicación.

**Aclaración sobre la capa de dominio:**

La capa de dominio contiene las interfaces (casos de uso, ports, exceptions, services) y el modelo de datos.

> Al consumir una API externa, se asume que **no somos dueños del dato**, pero sí contamos con un **dominio de presentación propio**. Esta capa no debe ser una réplica de las APIs externas, sino adecuarse al nivel de presentación que necesita el consumidor.

---

## 🚀 Mejoras identificadas

Resumen de mejoras identificadas y aún no aplicadas:

- Versionar el endpoint: `/v1/products/{id}/similar`.
- Externalizar los *paths* de las APIs externas a properties.
- Parametrizar por properties los valores del `RestClient` (timeouts, etc.).
- Implementar tests **e2e con Karate** para validar la integración entre capas.
- Añadir **pitest** para cobertura mediante tests de mutación.
