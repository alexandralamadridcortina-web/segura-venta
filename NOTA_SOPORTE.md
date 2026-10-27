NOTA DE SOPORTE TÉCNICO — SEGURAVENTA
Proyecto: SeguraVenta
Tecnologías: Java, Spring Boot, Maven y Python
Fecha: 27 de octubre de 2026
Alcance: T-01, T-02 y T-03


1. Objetivo
   Documentar el análisis, las hipótesis, las soluciones técnicas y las evidencias de verificación de tres incidentes relacionados con el registro y la consulta de ventas de seguros.

La aplicación SeguraVenta utiliza almacenamiento en memoria y expone endpoints HTTP para registrar y consultar ventas.

Los incidentes abordados son:
T-01: Prevención de ventas duplicadas por reintentos.
T-02: Control de concurrencia para evitar sobreventa de cupos.
T-03: Consulta de ventas y análisis de rendimiento.
2. T-01 — Prevención de ventas duplicadas
   Síntoma
   Una solicitud de venta puede ser enviada nuevamente cuando el usuario no recibe una respuesta o experimenta una interrupción de comunicación. Sin un control adecuado, esto puede ocasionar ventas duplicadas y descuentos adicionales de cupos.
   Hipótesis
   El sistema podría estar tratando cada petición POST como una venta nueva, sin verificar previamente si la referencia ya fue procesada.
   Análisis técnico
   Se identificó la necesidad de implementar idempotencia mediante el campo referenciaSolicitud.

Una referencia previamente registrada debe permitir identificar la venta original y comprobar que la campaña y el documento coincidan con los datos de la nueva solicitud.
Solución aplicada
En el servicio de ventas se utiliza una estructura de datos para consultar las referencias existentes.

Cuando llega una solicitud:

Se comprueba que los campos obligatorios sean válidos.

Se verifica si la referencia ya existe.

Si existe y los datos coinciden, se devuelve la venta original.

Si existe con datos incompatibles, se rechaza con HTTP 409.

Si no existe, se valida la campaña y la disponibilidad de cupos.

Si la operación es válida, se registra una nueva venta y se descuenta un solo cupo.

La prima de la campaña VIDA-01 es de 50.000 COP.
Verificación
Se deben comprobar los siguientes escenarios:

Primera solicitud válida: se registra una venta.

Reintento idéntico: se conserva el identificador y la fecha de la venta original.

Referencia incompatible: se devuelve HTTP 409.

Datos obligatorios ausentes: se devuelve HTTP 400.

Campaña inexistente: se rechaza sin reservar la referencia.

Reintento de una venta existente con cupos agotados: se devuelve la venta original.
Estado: lógica identificada en el código revisado; pendiente confirmar y adjuntar la evidencia final de ejecución de todas las pruebas.
3. T-02 — Control de concurrencia y cupos
   Síntoma
   Cuando dos solicitudes llegan simultáneamente y queda un solo cupo, existe el riesgo de que ambas operaciones confirmen una venta.
   Hipótesis
   Las operaciones de consultar cupos, registrar la venta y descontar disponibilidad podrían ejecutarse simultáneamente sin protección.
   Análisis técnico
   Este escenario representa una condición de carrera, en la que el resultado depende del orden en que se ejecutan varios hilos.
   Solución aplicada
   En el método de registro del servicio se utiliza synchronized para proteger la operación sobre el estado compartido.

Esta sincronización permite que una solicitud complete la operación protegida antes de que otra la ejecute sobre la misma instancia del servicio.
Verificación
El escenario principal consiste en preparar un solo cupo disponible y ejecutar dos solicitudes diferentes de manera concurrente.
Resultado esperado:

Exactamente una venta confirmada.

Una solicitud rechazada por falta de cupos.

Cero cupos restantes.

Ninguna sobreventa.

También se debe comprobar que dos solicitudes idénticas concurrentes devuelvan la misma venta.
Estado: mecanismo de sincronización revisado; pendiente confirmar el resultado final de la prueba automatizada de concurrencia.
4. T-03 — Consultas y rendimiento
   Síntoma
   Las consultas del historial de ventas pueden tardar más cuando aumenta la cantidad de registros almacenados.
   Hipótesis
   El recorrido de colecciones, la búsqueda de referencias y el ordenamiento podrían incrementar el tiempo necesario para responder a las peticiones GET.
   Análisis técnico
   El endpoint GET /ventas debe permitir:

Consultar ventas mediante una referencia exacta.

Listar las ventas más recientes.

Ordenar por fecha descendente y, en caso de empate, por identificador ascendente.

Aplicar un límite predeterminado de 20.

Aceptar límites entre 1 y 100.

Devolver una lista vacía cuando no existan coincidencias.

Para evaluar el rendimiento se utilizaron registros históricos ficticios, separados de los cupos de la campaña VIDA-01.
Herramienta utilizada
Se creó el archivo benchmark.py como herramienta auxiliar para medir los tiempos de respuesta HTTP.

El script realiza consultas GET y calcula estadísticas como la mediana y el percentil 95.
Procedimiento
Se realizaron dos escenarios:
Escenario 1: configuración de 1.000 registros históricos.
Escenario 2: configuración de 20.000 registros históricos.

Para cada escenario se reinició Spring Boot y se ejecutó:

py benchmark.py 100

El número 100 representa la cantidad de mediciones por tipo de consulta.
Evidencia de rendimiento
Métrica
1.000 registros
20.000 registros
Mediana del listado

5.258 ms

5.312 ms

P95 del listado

10.190 ms

6.862 ms

Mediana de búsqueda exacta

4.909 ms

4.691 ms

P95 de búsqueda exacta

17.155 ms

5.904 ms
Interpretación de los resultados
Las medianas obtenidas fueron similares en los dos escenarios.

El listado presentó una mediana de 5.258 ms con 1.000 registros y de 5.312 ms con 20.000 registros.

La búsqueda exacta presentó una mediana de 4.909 ms con 1.000 registros y de 4.691 ms con 20.000 registros.

Durante estas mediciones locales no se observó una degradación importante de las medianas.

Estos resultados describen el comportamiento observado y no constituyen una garantía de rendimiento en producción.

Tampoco representan una comparación controlada entre una versión anterior y otra optimizada.
Limitaciones de la medición
Las mediciones se realizaron en un entorno local.

No se ejecutó una prueba de carga concurrente mediante el benchmark.

Los tiempos pueden variar según el equipo, la JVM y otros procesos.

Se debe verificar que cada escenario haya cargado realmente la cantidad de registros configurada.

No se dispone de evidencia confirmada de una comparación antes y después de una optimización.
5. Protección de información sensible
   El campo documento se utiliza para validar la compatibilidad de solicitudes repetidas.

Este dato no debe aparecer en las respuestas HTTP, los mensajes de error ni los registros de diagnóstico.

Para investigar incidentes se recomienda registrar información técnica como identificadores de correlación, método HTTP, ruta, código de respuesta y duración de la petición, evitando almacenar información personal.

La protección efectiva de los registros debe comprobarse en la implementación final.
6. Plan de pruebas y verificación
   Las pruebas necesarias para validar el sistema incluyen:

Registro correcto de una venta nueva.

Reintento idéntico sin consumo adicional de cupos.

Conflicto por referencia incompatible.

Rechazo de solicitudes inválidas.

Rechazo de campañas inexistentes.

Control de concurrencia sobre el último cupo.

Consulta de ventas por referencia.

Listado ordenado de ventas.

Validación de límites permitidos.

Protección del documento en respuestas y registros.

Medición del rendimiento con 1.000 y 20.000 registros.

Para ejecutar las pruebas automatizadas desde la carpeta principal del proyecto en Windows:

.\mvnw.cmd clean test
Estado de verificación: las mediciones de rendimiento se ejecutaron y sus resultados están documentados. La salida final de las pruebas Maven y las evidencias completas de los escenarios funcionales están pendientes de confirmación.
7. Plan de recuperación y rollback
   Si después de aplicar cambios aparecen ventas duplicadas, sobreventa, errores inesperados o degradación del rendimiento, se debe:

Registrar el incidente y conservar las evidencias técnicas.

Identificar la versión del código involucrada.

Suspender las operaciones de prueba que puedan afectar el estado.

Recuperar la última versión estable mediante Git, cuando corresponda.

Ejecutar nuevamente las pruebas de regresión.

Verificar que los incidentes no se reproduzcan.

Documentar el resultado y escalar cualquier problema pendiente.

Debido al almacenamiento en memoria, reiniciar la aplicación elimina el estado de ventas y cupos de la ejecución anterior.
8. Limitaciones conocidas
   Persistencia: los registros se pierden al reiniciar la aplicación.
   Concurrencia: synchronized protege una instancia del servicio, pero no coordina varias instancias ejecutándose en servidores diferentes.
   Rendimiento: el benchmark local no representa una prueba de carga ni garantiza los mismos tiempos en producción.
   Evidencias pendientes: es necesario confirmar la ejecución final de las pruebas automatizadas y contrastar esta documentación con el código definitivo.
9. Recomendaciones para un entorno productivo
   Para evolucionar SeguraVenta hacia una aplicación productiva se recomienda:

Incorporar una base de datos transaccional.

Implementar restricciones de unicidad para las referencias.

Controlar los cupos mediante operaciones atómicas en almacenamiento compartido.

Incorporar monitoreo de errores y tiempos de respuesta.

Ejecutar pruebas de carga y concurrencia.

Definir mecanismos de recuperación y respaldo.

Fortalecer la trazabilidad y la protección de información personal.
10. Conclusión
    El proyecto SeguraVenta aborda tres problemas principales: la duplicación de ventas por reintentos, el riesgo de sobreventa por concurrencia y el rendimiento de las consultas HTTP.

El diseño revisado incorpora controles de idempotencia y sincronización, así como consultas y mediciones de rendimiento.

Las pruebas locales realizadas con configuraciones de 1.000 y 20.000 registros históricos mostraron medianas de respuesta similares.

Antes de declarar completamente cerrados los tres tickets, se deben confirmar las pruebas automatizadas, los resultados funcionales y la correspondencia entre esta documentación y la versión final del código.
Estado: documentación preparada con evidencias de rendimiento confirmadas y verificaciones funcionales pendientes.

1. Objetivo
   Documentar el análisis, las hipótesis, las soluciones técnicas y las evidencias de verificación de tres incidentes relacionados con el registro y la consulta de ventas de seguros.

La aplicación SeguraVenta utiliza almacenamiento en memoria y expone endpoints HTTP para registrar y consultar ventas.

Los incidentes abordados son:
T-01: Prevención de ventas duplicadas por reintentos.
T-02: Control de concurrencia para evitar sobreventa de cupos.
T-03: Consulta de ventas y análisis de rendimiento.
2. T-01 — Prevención de ventas duplicadas
   Síntoma
   Una solicitud de venta puede ser enviada nuevamente cuando el usuario no recibe una respuesta o experimenta una interrupción de comunicación. Sin un control adecuado, esto puede ocasionar ventas duplicadas y descuentos adicionales de cupos.
   Hipótesis
   El sistema podría estar tratando cada petición POST como una venta nueva, sin verificar previamente si la referencia ya fue procesada.
   Análisis técnico
   Se identificó la necesidad de implementar idempotencia mediante el campo referenciaSolicitud.

Una referencia previamente registrada debe permitir identificar la venta original y comprobar que la campaña y el documento coincidan con los datos de la nueva solicitud.
Solución aplicada
En el servicio de ventas se utiliza una estructura de datos para consultar las referencias existentes.

Cuando llega una solicitud:

Se comprueba que los campos obligatorios sean válidos.

Se verifica si la referencia ya existe.

Si existe y los datos coinciden, se devuelve la venta original.

Si existe con datos incompatibles, se rechaza con HTTP 409.

Si no existe, se valida la campaña y la disponibilidad de cupos.

Si la operación es válida, se registra una nueva venta y se descuenta un solo cupo.

La prima de la campaña VIDA-01 es de 50.000 COP.
Verificación
Se deben comprobar los siguientes escenarios:

Primera solicitud válida: se registra una venta.

Reintento idéntico: se conserva el identificador y la fecha de la venta original.

Referencia incompatible: se devuelve HTTP 409.

Datos obligatorios ausentes: se devuelve HTTP 400.

Campaña inexistente: se rechaza sin reservar la referencia.

Reintento de una venta existente con cupos agotados: se devuelve la venta original.
Estado: lógica identificada en el código revisado; pendiente confirmar y adjuntar la evidencia final de ejecución de todas las pruebas.
3. T-02 — Control de concurrencia y cupos
   Síntoma
   Cuando dos solicitudes llegan simultáneamente y queda un solo cupo, existe el riesgo de que ambas operaciones confirmen una venta.
   Hipótesis
   Las operaciones de consultar cupos, registrar la venta y descontar disponibilidad podrían ejecutarse simultáneamente sin protección.
   Análisis técnico
   Este escenario representa una condición de carrera, en la que el resultado depende del orden en que se ejecutan varios hilos.
   Solución aplicada
   En el método de registro del servicio se utiliza synchronized para proteger la operación sobre el estado compartido.

Esta sincronización permite que una solicitud complete la operación protegida antes de que otra la ejecute sobre la misma instancia del servicio.
Verificación
El escenario principal consiste en preparar un solo cupo disponible y ejecutar dos solicitudes diferentes de manera concurrente.
Resultado esperado:

Exactamente una venta confirmada.

Una solicitud rechazada por falta de cupos.

Cero cupos restantes.

Ninguna sobreventa.

También se debe comprobar que dos solicitudes idénticas concurrentes devuelvan la misma venta.
Estado: mecanismo de sincronización revisado; pendiente confirmar el resultado final de la prueba automatizada de concurrencia.
4. T-03 — Consultas y rendimiento
   Síntoma
   Las consultas del historial de ventas pueden tardar más cuando aumenta la cantidad de registros almacenados.
   Hipótesis
   El recorrido de colecciones, la búsqueda de referencias y el ordenamiento podrían incrementar el tiempo necesario para responder a las peticiones GET.
   Análisis técnico
   El endpoint GET /ventas debe permitir:

Consultar ventas mediante una referencia exacta.

Listar las ventas más recientes.

Ordenar por fecha descendente y, en caso de empate, por identificador ascendente.

Aplicar un límite predeterminado de 20.

Aceptar límites entre 1 y 100.

Devolver una lista vacía cuando no existan coincidencias.

Para evaluar el rendimiento se utilizaron registros históricos ficticios, separados de los cupos de la campaña VIDA-01.
Herramienta utilizada
Se creó el archivo benchmark.py como herramienta auxiliar para medir los tiempos de respuesta HTTP.

El script realiza consultas GET y calcula estadísticas como la mediana y el percentil 95.
Procedimiento
Se realizaron dos escenarios:
Escenario 1: configuración de 1.000 registros históricos.
Escenario 2: configuración de 20.000 registros históricos.

Para cada escenario se reinició Spring Boot y se ejecutó:

py benchmark.py 100

El número 100 representa la cantidad de mediciones por tipo de consulta.
Evidencia de rendimiento
Métrica
1.000 registros
20.000 registros
Mediana del listado

5.258 ms

5.312 ms

P95 del listado

10.190 ms

6.862 ms

Mediana de búsqueda exacta

4.909 ms

4.691 ms

P95 de búsqueda exacta

17.155 ms

5.904 ms
Interpretación de los resultados
Las medianas obtenidas fueron similares en los dos escenarios.

El listado presentó una mediana de 5.258 ms con 1.000 registros y de 5.312 ms con 20.000 registros.

La búsqueda exacta presentó una mediana de 4.909 ms con 1.000 registros y de 4.691 ms con 20.000 registros.

Durante estas mediciones locales no se observó una degradación importante de las medianas.

Estos resultados describen el comportamiento observado y no constituyen una garantía de rendimiento en producción.

Tampoco representan una comparación controlada entre una versión anterior y otra optimizada.
Limitaciones de la medición
Las mediciones se realizaron en un entorno local.

No se ejecutó una prueba de carga concurrente mediante el benchmark.

Los tiempos pueden variar según el equipo, la JVM y otros procesos.

Se debe verificar que cada escenario haya cargado realmente la cantidad de registros configurada.

No se dispone de evidencia confirmada de una comparación antes y después de una optimización.
5. Protección de información sensible
   El campo documento se utiliza para validar la compatibilidad de solicitudes repetidas.

Este dato no debe aparecer en las respuestas HTTP, los mensajes de error ni los registros de diagnóstico.

Para investigar incidentes se recomienda registrar información técnica como identificadores de correlación, método HTTP, ruta, código de respuesta y duración de la petición, evitando almacenar información personal.

La protección efectiva de los registros debe comprobarse en la implementación final.
6. Plan de pruebas y verificación
   Las pruebas necesarias para validar el sistema incluyen:

Registro correcto de una venta nueva.

Reintento idéntico sin consumo adicional de cupos.

Conflicto por referencia incompatible.

Rechazo de solicitudes inválidas.

Rechazo de campañas inexistentes.

Control de concurrencia sobre el último cupo.

Consulta de ventas por referencia.

Listado ordenado de ventas.

Validación de límites permitidos.

Protección del documento en respuestas y registros.

Medición del rendimiento con 1.000 y 20.000 registros.

Para ejecutar las pruebas automatizadas desde la carpeta principal del proyecto en Windows:

.\mvnw.cmd clean test
Estado de verificación: las mediciones de rendimiento se ejecutaron y sus resultados están documentados. La salida final de las pruebas Maven y las evidencias completas de los escenarios funcionales están pendientes de confirmación.
7. Plan de recuperación y rollback
   Si después de aplicar cambios aparecen ventas duplicadas, sobreventa, errores inesperados o degradación del rendimiento, se debe:

Registrar el incidente y conservar las evidencias técnicas.

Identificar la versión del código involucrada.

Suspender las operaciones de prueba que puedan afectar el estado.

Recuperar la última versión estable mediante Git, cuando corresponda.

Ejecutar nuevamente las pruebas de regresión.

Verificar que los incidentes no se reproduzcan.

Documentar el resultado y escalar cualquier problema pendiente.

Debido al almacenamiento en memoria, reiniciar la aplicación elimina el estado de ventas y cupos de la ejecución anterior.
8. Limitaciones conocidas
   Persistencia: los registros se pierden al reiniciar la aplicación.
   Concurrencia: synchronized protege una instancia del servicio, pero no coordina varias instancias ejecutándose en servidores diferentes.
   Rendimiento: el benchmark local no representa una prueba de carga ni garantiza los mismos tiempos en producción.
   Evidencias pendientes: es necesario confirmar la ejecución final de las pruebas automatizadas y contrastar esta documentación con el código definitivo.
9. Recomendaciones para un entorno productivo
   Para evolucionar SeguraVenta hacia una aplicación productiva se recomienda:

Incorporar una base de datos transaccional.

Implementar restricciones de unicidad para las referencias.

Controlar los cupos mediante operaciones atómicas en almacenamiento compartido.

Incorporar monitoreo de errores y tiempos de respuesta.

Ejecutar pruebas de carga y concurrencia.

Definir mecanismos de recuperación y respaldo.

Fortalecer la trazabilidad y la protección de información personal.
10. Conclusión
    El proyecto SeguraVenta aborda tres problemas principales: la duplicación de ventas por reintentos, el riesgo de sobreventa por concurrencia y el rendimiento de las consultas HTTP.

El diseño revisado incorpora controles de idempotencia y sincronización, así como consultas y mediciones de rendimiento.

Las pruebas locales realizadas con configuraciones de 1.000 y 20.000 registros históricos mostraron medianas de respuesta similares.

Antes de declarar completamente cerrados los tres tickets, se deben confirmar las pruebas automatizadas, los resultados funcionales y la correspondencia entre esta documentación y la versión final del código.
Estado: documentación preparada con evidencias de rendimiento confirmadas y verificaciones funcionales pendientes.
 