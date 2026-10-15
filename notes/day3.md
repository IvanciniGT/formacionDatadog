
# Ejemplo completo de un Dashboard para nuestra TIENDA

Pestaña 1: NEGOCIO

PREGUNTAS:

- ¿Cuánto vendemos?  MACROS (SUMAS)
    - ¿Cuántos pedidos están realizándose?
        - DATO NUMERICO (TOTAL PEDIDOS REALIZADOS)
        - HISTOGRAMA (SERIE DE TIEMPO)
    - ¿Cuánto ingresamos por los pedidos?
    - ¿Vendo más o menos que ayer? (número de pedidos o en importe)
- ¿Qué vendemos? ¿Cómo son las ventas?
    - ¿Cuánto vendo de cada categoria? ¿Qué categoría vende más?
      - Tipo de variable?
        - CUALITATIVA:
    - ¿Cuánto importe genero con cada categoria? ¿Qué categoría genera más ingresos?
    - Evolución de las ventas por categoría
    - Ticket Medio por venta
- ¿A quién le vendemos?   (Tengo clientes personas, tenemos robots)
   - ¿Ticket promedio por cliente?
   - ¿De qué paises son mis clientes?
- ¿Si esto funciona? ¿ Si cumplimos promesa? (robots)
  - SLO


Pestaña 2: OPERACIONES / GUARDIA
- ¿Funciona o está roto?
- ¿Dónde hay problemas?
  - Rendimiento
- ¿Por qué?

Los dashboards deben ir de lo general a lo específico.

---

# Markdown:

Lenguaje para documentar, con formatos, pero usando caracteres.

# Titulo
## Subtitulo
### Subsubtitulo
###### Encabezado de nivel6

En los textos podemos usar:
- Cursiva: *texto en cursiva*
- Negrita: **texto en negrita**
- Negrita y cursiva: ***texto en negrita y cursiva***
- Tachado: ~~texto tachado~~ ~
- Código... Nombre estandarizado `nombre_codigo`

Listas:
- No numeradas
- Numeradas
  1. Primer elemento
  1. Segundo elemento
  1. Tercer elemento

## Tablas:

| Encabezado 1 | Encabezado 2 | Encabezado 3 |
|--------------|-------------:|:------------:|
| Fila 1 Col 1 | Fila 1  | Col 3 |
| Fila 2 Col 1 | Fila 2  | Col 3 |

# Enlaces:
[Texto del enlace](http://url_del_enlace.com)

# Imagenes:
![Texto alternativo de la imagen](https://corp.dd-static.net/img/about/presskit/logo-v/dd_vertical_purple.png?auto=format&fit=max&w=847&dpr=2)


---

# Monitores en detalle

Los dashboards están guay, pero son pasivos! Alguien debe estar mirando.
Los monitores son ACTIVOS! Es Datadog el que los está mirando y me avisa!

  MONITOR = UNA CONSULTA!
            + Cada cuanto se hace y sobre qué ventana de tiempo
            + Unos umbrales (aviso, alerta, recuperación)
            + Un mensaje
            + A quién se lo mando!

## Estados en los que puede estar un monitor

- OK
- WARN
- ALERT
- NO DATA

Datadog nos avisa en los cambios de estado:

    OK -> WARN (aviso: warn)
    WARN -> ALERT (aviso: alert)
    ALERT -> OK (aviso: recovery)

Hay opción de solicitar a datadog que NOS TOQUE LAS NARICES en acaso de que algo SIGA MAL

Por defecto, datadog solo nos avisa en los cambios de estado, pero no insiste si el problema persiste.
Podemos configurar un "renotify" para que Datadog nos vuelva a avisar si el problema persiste.

## Tipos de monitor

| Tipo de monitor | Descripción                                   | Ejemplo en nuestra tienda                 |
|-----------------|-----------------------------------------------|-------------------------------------------|
| Metricas        | Se mira una métrica o una fórmula de varias   | Una rotura de stock, % de errores de pago |
| APM             | Monitoreo de rendimiento/errors de aplicaciones | Latencia de las solicitudes, tasa de errores en los servicios |
| SLO             | Fallos en las operaciones                     | % errores en als operaciones en la tienda |
| Logs            | Monitoreo de registros                        | Más de 20 payment rejected en un periodo de tiempo determinado |
| Synthetic       | Monitoreo de disponibilidad y rendimiento desde la perspectiva del usuario | Tiempo de carga de la página, disponibilidad del checkout |
| Eventos         | Monitoreo de eventos del sistema o de la aplicación | Despliegues, cambios de configuración, errores críticos |
| Hosts           | Monitoreo de los servidores físicos o virtuales | Uso de CPU, memoria, disco, estado del host |
...

Básicamente TODOS LOS DATOS que son recopilados por TODOS LOS PRODUCTOS que tengo dispoibles en DATADOG pueden ser objeto de un MONITOR.

## Vamos definiendo mensajes

La sintaxis para definir mensajes es muy especial!

---

# Monitores para la pestaña de GUARDIA!

  TRUCO a la hora de definir todos estos monitores... Solo los queremos apra el dashboard... por lo que pondremos: @NoBody

- MONITOR [METRICA]: Tasa de peticiones con error
  En este caso (desde el punto de vista de guardía) no interesa el budget, sino el rate:
  - % de peticiones que acaban en error, en los últimos 10 minutos. > 4%   WARN
  - % de peticiones que acaban en error, en los últimos 10 minutos. > 10%. ALERT

- MONITOR [LOGS]: picos de pago rechazado
  Buscamos el número de lineas de log en las que aparece "error en el pago"  > 20 en 10 minutos   ALERT
  Buscamos el número de lineas de log en las que aparece "error en el pago"  > 10 en 10 minutos    WARN

- MONITOR [ANOMALIA]: Volumen de pedidos fuera de lo normal [alza o baja]
  El problema con este es definir el umbral.
  Segñun lo he definido en el nombre, cuál sería el umbral? Comparativa con el históric (el de ayer? antes de ayer? el mismo día de la semana pasada? el mismo día del año pasado?) 
    Dejo que el propio Datadog aprenda de los datos históricos.
    Diré que si hay una desviación del 25%-30% con respecto al histórico, se dispare una alerta.

- MONITOR [COMPOSITE] Latencia ALTA (tarda mucho) y hay mucho tráfico
  - MONITOR: [APM]     Latencia ALTA
  - MONITOR: [METRICA] Tráfico ALTO
   


---

# Notebooks

Los dashboards son paneles que muestran el estado y las métricas de nuestros sistemas y aplicaciones en tiempo real.

Un notebook es un documento que incorpora WIDGETS y textos (markdown), analizando esos widgets.

Dentro de markdown podemos usar mermaid.
Mermaid es un lenguaje para escribir GRAFICO (no para pintar gráficos... para ESCRIBIRLOS)

Los notebooks nos vienen de maravilla para documentar los procedimientos de trabajo.

Que tenga un panel (DASHBOARD) Está muy bien... la pregunta es QUE HAGO CON EL?
El dashboard arroja datos!
¿Como impactan esos datos en los flujos de trabajo o en lasa decisiones que tomamos?
Eso suele ser conocimiento que tienen ciertas personas, pero que cuesta transmitir en la empresa.

> Imaginad el dashboard de GUARDIA!
Eso arrija datos.. un huevo de datos.
Qué hago con esos datos.
Si la cpu pasa del 80% QUE?
Oa lo mejor solo importa si además la memoria está al 20%

YA no es que un MONITOR SE PONGA WARNING... QUE HAGO?

Yo me lo sé... Pero me voy de vacaciones. Me ascienden. Me voy a otra empresa.
Y el que viene ahora?

El nombre CUADRENO DE TRABAJO.
Son mis apuntes acerca de las cosas que me interesan... en el día a día.


---

# Reports

Es una foto de un dashboard. Realmente es una PLANIFICACION de fotos de un dashboard que voy haciendo a lo largo del tiempo.









---

# APM 

Es el producto de datadog que nos permite hacer MONITORIZACIÓN DE SERVICIOS.
Google definió un conjunto de métricas que sería el MINIMO necesario para monitorizar un servicio correctamente.
A ese conjunto de métricas se le conoce como el "Golden Signals" de Google:
- Latencia: tiempo de respuesta de las solicitudes.
- Tráfico: cantidad de solicitudes.
- Errores: tasa de errores.
- Saturación: uso de recursos del sistema.

