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

Esta sección permite arrancar el proyecto **desde un checkout limpio**.

### Prerrequisitos

- **JDK 21**
- **Maven 3.8+**
- **Docker** (con `docker compose`) para levantar el entorno de dependencias (mock del servicio externo y Redis)

Todos los comandos se ejecutan desde el path del código: `~/devtest/code`.

### 1. Build

Compila y ejecuta la suite de tests de todos los módulos:

```bash
mvn clean install
```

### 2. Levantar el entorno de dependencias

La aplicación necesita **un servicio externo de producto** (del que obtiene los IDs de similares y el detalle) y, opcionalmente, **Redis** como caché. Ambos se levantan con el `docker compose` incluido:

```bash
docker compose -f src/test/resources/compose/docker-compose.yaml up -d simulado redis
```

Esto arranca:

| Servicio | Imagen | Puerto | Rol |
| -------- | ------ | ------ | --- |
| `simulado` | `ldabiralai/simulado` | **3001** | **Mock del servicio externo de producto** (respuestas definidas en `src/test/resources/compose/shared/simulado/mocks.json`). Es la dependencia que la app consume en `rest-clients.product.base-url`. |
| `redis` | `redis:8-alpine` | **31110** | Caché de los IDs de similares. |

> **Redis es opcional:** si no está disponible, la aplicación **degrada de forma controlada** y sigue funcionando sin caché (cada petición va directa al servicio externo, vía `LoggingCacheErrorHandler`). El **mock `simulado`**, en cambio, **sí es necesario** para obtener respuestas: sin él, las llamadas al servicio externo fallan y se traducen en errores de integración (`502`).

> El `docker-compose.yaml` incluye además `k6`, `grafana` e `influxdb`, usados solo para las pruebas de carga (no necesarios para arrancar la aplicación). Para levantar **todo** el entorno: `docker compose -f src/test/resources/compose/docker-compose.yaml up -d`.

### 3. Ejecutar la aplicación

```bash
cd boot
mvn spring-boot:run
```

> **Profiles:** la aplicación no está configurada por profiles, por lo que **no es necesario especificar ninguno**. Arranca por defecto en el puerto **5000**.

### 4. Probar el endpoint

Con el entorno levantado y la app arrancada, el único endpoint expuesto es:

```bash
curl http://localhost:5000/product/1/similar
```

> **TTL e invalidación de caché:** solo se cachean los IDs de productos similares (datos estables), con un **TTL de 24h**. El detalle del producto (precio y disponibilidad) es volátil y **no se cachea**, por lo que siempre se sirve fresco.

### Variables configurables

Se configuran en `boot/src/main/resources/application.yaml` (o se sobreescriben por variables de entorno / argumentos de Spring Boot):

| Propiedad | Valor por defecto | Descripción |
| --------- | ----------------- | ----------- |
| `server.port` | `5000` | Puerto de la aplicación. |
| `rest-clients.product.base-url` | `http://localhost:3001` | URL base del servicio externo de producto (el mock `simulado`). |
| `rest-clients.product.connect-timeout` | `5s` | Timeout de conexión contra el servicio externo. |
| `rest-clients.product.read-timeout` | `5s` | Timeout de lectura contra el servicio externo. |
| `rest-clients.product.global-timeout` | `6s` | Timeout global por petición para la resolución paralela del detalle. |
| `rest-clients.product.max-concurrent-calls` | `50` | Límite de llamadas concurrentes contra el servicio externo (backpressure). |
| `spring.data.redis.host` | `localhost` | Host de Redis. |
| `spring.data.redis.port` | `31110` | Puerto de Redis. |

---

## 🤖 Testing

Desde el path `~/devtest/code`, la suite completa (tests unitarios + tests de integración) se ejecuta con:

```bash
mvn clean verify
```

- Los **tests unitarios** (`*Test`) cubren cada clase con lógica de forma aislada (casos de uso, adapter, mappers, executor paralelo, traducción de errores y manejo de excepciones).
- Los **tests de integración** (`*IT`, en el módulo `boot`, ejecutados por `failsafe` en la fase `verify`) arrancan el flujo completo con el servicio externo simulado mediante **WireMock**, reproduciendo los escenarios de carga (normal, `404`, error `5xx`, lento y muy lento) y validando la política de respuestas parciales, la ejecución paralela del detalle y el contrato OpenAPI.

> Los tests de integración **no requieren** levantar el `docker compose`: WireMock simula el servicio externo dentro del propio test.

### Alternativas no implementadas

| Alternativa | Estado |
| ----------- | ------ |
| **e2e de Karate** para validar la integración entre capas | ❌ No implementado |
| Cobertura por **tests de mutación (pitest)** | ❌ No implementado |

---

## 📖 Decisiones técnicas

### Arquitectura general

Se ha optado por una **arquitectura hexagonal (`ports & adapters`) multimódulo**, para separar responsabilidades por capas y mantener dependencias claras entre ellas.

---

### Capa de infraestructura — Controllers

Partiendo de la capa más exterior, los controllers están construidos sobre **Spring Web**, usando las anotaciones RESTful. Residen en el módulo `infrastructure`, bajo el package `input/rest`. Actualmente se expone **un único endpoint GET**.

**Mejoras aplicadas:**

- **Enfoque contract-first:** las interfaces y modelos del controller se **generan a partir del OpenAPI** (`similarProducts.yaml`) como fuente de verdad, mediante `openapi-generator`. El contrato acordado se respeta tal cual: el endpoint es `/product/{productId}/similar` (en **singular**, como especifica el contrato) y el `id` es `string`. El **test de integración** (`ProductSimilarIT`) valida además que la respuesta cumple el OpenAPI (`swagger-request-validator`), impidiendo divergencias entre contrato e implementación.
- **Manejo de excepciones:** existe una capa *handler exception* que captura las excepciones de la aplicación, las redirecciona y las encapsula en su verbo HTTP correspondiente.

**Mejora identificada (no aplicada):**

- **Versionado del endpoint** estilo `/v1/product/{productId}/similar`.

---

### Capa de infraestructura — APIs externas

Módulo adicional dedicado exclusivamente a las llamadas a **APIs externas**: definición de los beans de configuración de los distintos `RestClients` e implementación de las llamadas.

**Mejoras aplicadas:**

- **Cliente contract-first:** el cliente de la API externa se **genera desde su OpenAPI** (`existingApis.yaml`), evitando montar a mano los modelos y las llamadas HTTP.
- **Configuración de RestClients:** parametrizada por properties en el `application.yaml` del módulo `boot` (URL base, timeouts de conexión/lectura, timeout global y límite de concurrencia). Ver la tabla de [variables configurables](#variables-configurables).
- **Resiliencia con Circuit Breakers:** `Retry` y `CircuitBreaker` (Resilience4j) configurados por properties. Requieren `resilience4j-spring-boot3` + `spring-boot-starter-aop` para que los aspectos se apliquen, y el nombre de la instancia del código coincide con el de la configuración.
- **Sistema de fallback:** cuando el circuito se abre, el fallback normaliza el error (incluida la `CallNotPermittedException` del circuito abierto) a una excepción de dominio, sin ocultar la caída del upstream. Una **ausencia legítima** (un `404` del servicio externo) se propaga como tal (`404`) y no queda enmascarada como fallo de integración.
- **Traducción de errores y semántica HTTP:** los fallos de red (connection refused, timeouts) y de deserialización también se traducen; los errores del upstream (400/401/403/5xx) se tratan como fallos de integración internos (**502/503/504**), no como errores 4xx dirigidos al cliente, y no se expone el body remoto.
- **Caché con Redis:** solo se cachean los **IDs de similares** (estables) con **TTL de 24h**; el detalle (precio/disponibilidad) no se cachea. Si Redis no está disponible, la aplicación **degrada** y sigue sin caché (`LoggingCacheErrorHandler`).
- **Concurrencia y capacidad:** las llamadas de detalle se ejecutan en paralelo sobre **hilos virtuales**, con un **semáforo** que limita la concurrencia contra el upstream (backpressure) y un **timeout global** por petición coherente con los timeouts del cliente.

**Servicios adicionales para la gestión del Adapter:**

- **Servicio `Invoker` (wrapper):** centraliza la traducción de excepciones externas a excepciones de dominio, añadiendo además decoraciones. Al ser un servicio, la funcionalidad queda centralizada y disponible para toda la aplicación.
- **Traducción de excepciones (mapper adhoc):** clase mapper que gestiona de forma transparente los distintos verbos HTTP devueltos por los servicios externos y los transforma en excepciones de dominio.
- **Mapeo con MapStruct:** para el mapeo automático de DTOs/Entities externos a entidades del dominio propio.

---

### Capa de aplicación — Casos de uso

Contiene los **casos de uso**, que actúan como orquestadores y decoradores del "negocio" y "dominio" sobre la API externa consumida. En este caso la lógica es sencilla.

**Mejoras aplicadas:**

- **Executor para paralelización:** se ha definido un executor que paraleliza las llamadas de detalle mediante `CompletableFuture` sobre hilos virtuales. Se ha expuesto como **servicio reutilizable**, evitando que quede encapsulado dentro de flujos a los que no pertenece.
- **Política de resultados parciales:** el caso de uso aísla los fallos individuales y define explícitamente el comportamiento: si no hay similares devuelve vacío; ante un fallo parcial degrada a *best-effort* (devuelve los detalles obtenidos); un 404 se trata como ausencia legítima; y si **todos** los detalles fallan por un problema técnico se responde **5xx**, para no ocultar una caída del upstream tras un `200 []`. La clasificación de fallos se extrae a un `FailureClassifier` reutilizable.

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

- Versionar el endpoint: `/v1/product/{productId}/similar`.
- Externalizar los *paths* de las APIs externas a properties.
- **Securización del servicio:** actualmente el servicio se expone **sin autenticación** (decisión consciente para el alcance de esta prueba). Como mejora, añadir una capa de seguridad real con **Spring Security** (p. ej. *OAuth2 Resource Server* validando JWT), protegiendo los endpoints de negocio y dejando expuestos únicamente los de *actuator* necesarios (`health`, `prometheus`). Debe mantenerse la separación de responsabilidades: un fallo de auth del **usuario final** contra el servicio se responde como **401/403**, mientras que un 401/403 del **upstream** contra este servicio se sigue tratando como fallo de integración (**502**).
- Ampliar la batería de tests: **e2e con Karate** para validar la integración entre capas y **pitest** para cobertura por mutación.
- **Cobertura con JaCoCo:** incorporar **JaCoCo** con un umbral razonable para medir y vigilar la cobertura de tests del proyecto.
- Invalidación **activa** de caché (por evento) además de la expiración por TTL, si el catálogo de similares lo requiriera.
- Blindar las reglas de arquitectura modular con **ArchUnit** (p. ej. que `domain` no dependa de infraestructura o que los DTOs de APIs externas no vivan en el modelo de dominio), para prevenir regresiones.
