# Similar Products Dev Test

<details>
  <summary><strong>Contenido</strong></summary>

* [ 📦 Similar product microservice ](#-similar-product-microservice)
* [ 🏃 Arranque ](#-arranque)
* [ 🤖 Testing ](#-testing)
* [ 📖 Decisiones técnicas ](#-decisiones-técnicas)
* 
</details>

## 📦 Similar product microservice
El enlace al repositorio publico es https://github.com/rasrov/devtest.

## 🏃 Arranque
Una vez tengas el repositorio descargado, es tan simple como lanzar este comando desde el path "~/devtest/code" de para levantar la aplicación:
> cd boot ; mvn spring-boot:run

La aplicación no está configurada por profiles, asi que no es necesario especificarlo. Levanta por defecto con el profile de "local"

## 🤖 Testing
Para lanzar la suit de test es muy parecido, desde el mismo path "~/devtest/code" y con la aplicación levantada, podemos lanzar el comando:
> mvn clean package

Hay tanto tests unitarios como de integración.

Los de integración están más enfocados en validar la `response` entity de la API que exponemos más que una validación de la integracion entre todas las capas del proyecto.

Si los test de integración no se enfocaran en la validación de la `response`, podríamos hacerlo mediante los e2e de karate, que no están implementados en esta aplicación.

Por otro lado, también se puede agregar pitest para tener un control de la cobertura más robusta mediante los tests de mutación, que tampoco está implementado en esta aplicación.

## 📖 Decisiones técnicas

* Se ha optado por una arquitectura hexagonal (`ports & adapters`) multi-módulo, para poder separar las responsabilidades por capas y tener las dependencias claras.

* Partiendo de la capa más exterior (controllers), están construidos bajo spring-web haciendo uso de las anotaciones de Restful y que está en nuestro módulo infrastructure bajo el package de
  `input/rest`. Actualmente, solo contamos con un endpoint GET.
  [*] Mejoras aplicadas
  - Se pidió exponer `/product/{id}/similar`, pero según las convenciones REST, es más adecuado que sea `/products/{id}/similar`. Asi que se ha cambiado tanto en el path del controller
        como en el test.js.
  - Por otro lado, sería adecuado versionar el `endpoint` estilo `/v1/products/{id}/similar`. Es una mejora, pero no se ha aplicado.
  - Tenemos también una capa "handler exception" que captura las excepciones de la aplicación que las redirecciona y encapsula en su verbo HTTP correspondiente.

* Siguiendo con la capa exterior, tenemos un módulo adicional para las llamadas a `APIs` externas.
  [*] Mejoras aplicadas
  - Esta capa es exclusiva para definir los beans de configuración de los diferentes RestClients que podamos tener e implementación de las llamadas externas. Los RestClients están
      parametrizados por configuración de los yaml a nivel de application.yaml de boot. Pero, por otro lado, los paths, están dentro de las implementaciones de los Port. Se podrían sacar
      a configuración de properties si fuera necesario.
  - Aquí también hemos aplicado una mejora de resiliencia con los circuit breakers. Tanto los Retry como los CircuitBreaker están configurados en un properties dentro del módulo de cada
      API externa que configuremos. La gracia de tener esta resiliencia es que podemos abrir el circuit breaker contra una API externa, si vemos que está inestable o las llamadas están
      tardando demasiado. La configuración del RestClient es básica, pero especificamos un timeout de 3s. Estos valores se podrían parametrizar por properties también si fuera necesario.
  - Por otro lado, también hay un sistema de fallback para cuando se abre el circuit breaker y dejamos de hacer peticiones a la API externa.
  - Otra cosa importante que se ha agregado como mejora, es un sistema de Caché con Redis para evitar relanzar peticiones identicas al servicio externo. Tiene una configuración bastante
      básica y practicamente por defecto.
  [*] Servicios adicionales para tener una mejor gestión de nuestro Adapter.
  - Tenemos un servicio Invoker estilo wrapper, que nos permite hacer una traducción de exceptions externas a excepciones de nuestro dominio, añadiendo además decoraciones. Al ser un
      servicio, toda esta funcionalidad está centralizada y expuesta al uso dentro de la aplicación.
  - La "traducción de excepciones" que he mencionado antes, es básicamente una clase mapper adhoc, que gestiona de manera transparente los diferentes tipos de verbos HTTP que nos pueden 
      devolver los servicios externos y ser transformados a nuestras excepciones de dominio.
  - Por último, también estamos usando mapstruct para el mapeo automático de `DTOs/Entities externos a entidades de nuestro dominio.

* Ahora veamos la capa de application, que es donde tenemos los casos de uso. En este caso es sencillo, aquí se implementan los use case, que hacen de orquestadores y decoradores del "negocio"
  y "dominio" de la API externa que se está consumiendo.
  [*] Mejoras aplicadas
  - En esta capa la principal mejora es la utilización de un executor definido que nos permite paralelizar llamadas mediante `CumputableFuture`.
    Este executor se ha definido como servicio para poder ser reutilizado y que no esté encapsulado dentro de flujos a los que no deberia de pertenecer.

* Repaso rápido de la capa de boot/domain/common.
    - Son capas definidas por defecto excepto /common, que esta se está usando como módulo de configuración de dependencias y servicios que van a nivel global en la aplicación.
    - [*] Aclaración sobre la función de nuestra capa de dominio
          en la capa de dominio tendremos las interfaces como los casos de uso, ports, exceptions, service y el modelo de datos
          al consumir una API externa, damos por hecho que no somos dueños del dato, pero sí que vamos a tener nuestro propio dominio de presentacion. Ya que es una capa que debe de contar con su propio dominio y no ser una réplica de `APIs` externas. Debemos de adecuarnos al nivel de presentación.