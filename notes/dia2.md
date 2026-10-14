# Día 2

## Repaso: etiquetas

Ayer: una etiqueta es un dato CUALITATIVO (casi siempre NOMINAL) que acompaña a cada dato.
    clave:valor
    host:worker3   status:500   category:audio   kube_namespace:datadog-curso

Sirve para CLASIFICAR (tabla de frecuencias), FILTRAR y AGRUPAR.

Hoy: hay 3 etiquetas que, además de clasificar, sirven para ENLAZAR.

---

# Unified Service Tagging

Datadog recibe los datos por caminos distintos:

    Métricas   <- el agente (CPU, RAM, contenedores) y la app (métricas custom)
    Trazas     <- el TRACER dentro de la app Java
    Logs       <- el agente, leyendo lo que escribe cada contenedor
    RUM        <- el navegador del usuario, directo a Datadog

Para Datadog son 4 montones de datos que NO SE CONOCEN ENTRE SÍ.
Lo único que los une: que lleven LAS MISMAS ETIQUETAS CON LOS MISMOS VALORES.

Datadog reserva 3 etiquetas para ese papel. Lo llama UNIFIED SERVICE TAGGING:

    env       En qué ENTORNO ocurre                 prod, pre, dev ... (nosotros: course)
    service   Qué COMPONENTE LÓGICO lo produce      shop-api, shop-web, shop-db
    version   Qué VERSIÓN del código corría         1.0.0, 1.0.5 ...

## Qué consigo si las 4 señales las llevan iguales

    service:shop-api  env:course
         |
         +--> métricas del contenedor de shop-api
         +--> trazas de shop-api
         +--> logs de shop-api
         +--> sesiones RUM que llamaron a shop-api
         +--> dashboards y monitores de shop-api

- Abro una traza lenta de POST /api/orders -> Datadog me enseña SUS logs y la CPU del contenedor EN ESE MOMENTO.
- Desde la sesión de un usuario (RUM) salto al backend.
- La página del servicio shop-api junta todo en una vista.

TODO ESTO NO LO HACE NADIE A MANO. Funciona porque Datadog busca service:shop-api env:course en cada fuente.

## Qué pasa si NO coinciden

    Tracer:  env:prod        Logs:  env:production
    Tracer:  service:shop-api       Logs:  service:shopapi

Para Datadog son COSAS DISTINTAS.
- Las trazas se quedan sin logs.
- La página del servicio sale medio vacía.
- NADIE AVISA. Simplemente los enlaces no funcionan.

Esto se decide EL PRIMER DÍA. Una convención escrita (una página en la wiki) y se revisa.

## De dónde salen en nuestra tienda

    Etiqueta   Dónde se pone
    --------   ----------------------------------------------------------------------
    env        label del pod  tags.datadoghq.com/env      + variable DD_ENV en la app
    service    label del pod  tags.datadoghq.com/service  + variable DD_SERVICE
    version    label del pod  tags.datadoghq.com/version  + variable DD_VERSION
    (RUM)      en el código JS de la página:  DD_RUM.init({ service, env, version, ... })

El resto de etiquetas (host, pod_name, kube_namespace, kube_cluster_name...) las pone EL AGENTE solo.

## Etiqueta vs identificador (OJO)

    Etiqueta  (env:course, status:500)       -> CLASIFICA. Pocos valores, muchos datos en cada valor.
                                                Sirve para AGRUPAR y FILTRAR.
    Trace id  (6acb4173...c691)              -> IDENTIFICA. Un valor por petición.
                                                Sirve para ENLAZAR: "dame todo lo de ESTA petición".

El trace id viaja en las TRAZAS y en los LOGS (como atributo).
NUNCA como etiqueta de una MÉTRICA: tendría una serie por petición (cardinalidad -> factura).

    Las etiquetas responden: ¿de qué TIPO es esto?
    Los ids responden:       ¿CUÁL de ellos es?

---

# La etiqueta VERSION y los despliegues

version es la que conecta EL DESPLIEGUE con EL COMPORTAMIENTO del producto.

Si cada despliegue pone la versión nueva:

    Errores %
       |                       despliegue 1.0.5
       |                             |
    13 |  ███ ███ ███ ███ ███        |
       |  ███ ███ ███ ███ ███        |
     2 |  ███ ███ ███ ███ ███        |   ▂▂▂ ▂▂▂ ▂▂▂ ▂▂▂
       +-----------------------------+------------------------> tiempo
              versión 1.0.4          |      versión 1.0.5

- Se ve CUÁNDO se desplegó (la raya vertical).
- Se compara una versión con otra: "la 1.0.5 tiene un 2 % de errores; la 1.0.4 tenía un 13 %".

Pregunta de PRODUCT OWNER: ¿el último despliegue ha mejorado o empeorado el producto? ¿Hacemos rollback?
Ahora se contesta con un dato.

## ¿De dónde sale la raya vertical?

Depende de DÓNDE miro:

    En las páginas de APM            Sale SOLA cuando cambia la etiqueta version.
    (página del servicio shop-api)   APM ve trazas con una versión nueva y marca el cambio.
                                     No hace falta nada más.

    En MIS dashboards                Sale SOLO si hay un EVENTO.
    (widgets timeseries)             En el widget configuro "Event Overlays":
                                        tags:service:shop-api deployment
                                     Cada evento que encaje = una raya.
                                     Un cambio de etiqueta, sin evento, aquí NO pinta nada.

OJO: solo en gráficas con EJE DE TIEMPO (timeseries: línea, barras, área).
En un histograma / distribution no hay eje de tiempo: no hay dónde pintar la raya.
Si quiero comparar versiones en un histograma: AGRUPO por la etiqueta  -> ... by {version}
    = cuantitativa (latencia) x cualitativa (versión). Dos curvas superpuestas.

## ¿Quién crea el EVENTO de despliegue?

    1. A MANO: alguien hace una llamada al API de Datadog (o lo crea en la web).
       Vale para una demo. En el día a día no lo mantiene nadie.

    2. EL PIPELINE (automático): un paso al final del pipeline de despliegue
       hace esa misma llamada al API, o ejecuta:
           datadog-ci deployment mark --service shop-api --env course --revision 1.0.5
       Es la misma llamada, pero la lanza el pipeline EN CADA DESPLIEGUE. Es lo correcto.

    3. Sin evento: si el pipeline pone bien la etiqueta version, APM marca el cambio
       en SUS gráficas (pero no aparece como evento en mis dashboards).

En una empresa el pipeline hace LAS DOS COSAS:
    - poner la version (para APM y para comparar versiones)
    - mandar el evento (para la raya en cualquier dashboard)

LA VERSIÓN LA PONE EL PIPELINE, igual que pone la etiqueta de la imagen.
Si se escribe a mano, NADIE LA ACTUALIZA.

---

# El apartado CI/CD de Datadog (Software Delivery)

Es OTRA COSA, relacionada:

    Pieza                     De dónde sale                        Qué me da                                   Producto
    -----------------------   ----------------------------------   -----------------------------------------   ------------------
    Etiqueta version          el despliegue la pone en la app      rayas en APM + comparar versiones           APM (incluido)
    Evento de despliegue      el pipeline llama al API             raya en cualquier dashboard                 Events (incluido)
    Software Delivery         GitLab / GitHub / Jenkins mandan     CI VISIBILITY: cada ejecución del pipeline  De pago, aparte
                              cada ejecución del pipeline            como una traza: etapas, duración, fallos
                              + datadog-ci deployment mark         DEPLOYMENTS / DORA:
                                                                     - cada cuánto desplegamos
                                                                     - tiempo de un commit a producción
                                                                     - % de despliegues que fallan
                                                                     - tiempo en recuperar el servicio

Conectar GitLab con Datadog (webhook) -> Datadog ve LAS EJECUCIONES de los pipelines.
Pero NO SABE cuál acabó en un despliegue a producción hasta que el pipeline se lo dice (datadog-ci deployment mark).

Y como ese registro lleva service y env, Datadog sigue la cadena entera:

    commit -> pipeline -> despliegue de la 1.0.5 -> errores de la 1.0.5 en producción

Que es lo que preguntaban ayer los perfiles de "Control de releases" y "Tech lead".

## Jenkins / GitLab CI / GitHub Actions / SonarQube vs Datadog

    Jenkins, GitLab CI...   EJECUTAN el pipeline (la fábrica)
    SonarQube               analiza el CÓDIGO parado (control de calidad de la pieza)
    Datadog                 OBSERVA: tendencias de los pipelines, tests que fallan a ratos,
                            y lo enlaza con lo que pasa en producción (la carretera).
                            No ejecuta nada.

---

# En nuestra tienda, HOY

    env       course           OK en todas las señales
    service   shop-api ...     OK en todas las señales
    version   1.0.0            FIJA A MANO en el manifiesto.
                               Hemos desplegado hasta la 1.0.5 y Datadog sigue diciendo 1.0.0.
                               -> APM solo ve una versión: no hay comparación posible.
    eventos de despliegue      el pipeline NO los manda
    Software Delivery          GitLab NO manda nada a Datadog

Es el ejemplo perfecto de lo que pasa cuando la versión no la pone el pipeline.

> Si fueras el PO de este producto, ¿qué le pedirías al equipo?
    1. Que el pipeline ponga la version real en cada despliegue.
    2. Que el pipeline mande el evento de despliegue.
    3. (Si se paga) conectar los pipelines a Datadog para tener las métricas DORA.

---

# MÉTRICAS EN DATADOG

## Serie temporal

En Datadog una métrica NO es un número suelto. Es una SERIE TEMPORAL:
    un NOMBRE + una COMBINACIÓN CONCRETA de etiquetas + puntos en el tiempo

    shop.orders.created {status:paid, category:audio}              10:00 -> 3   10:01 -> 5   10:02 -> 2 ...
    shop.orders.created {status:paid, category:peripherals}        10:00 -> 7   10:01 -> 4 ...
    shop.orders.created {status:payment_failed, category:audio}    ...

Cada COMBINACIÓN DISTINTA de valores de etiqueta = UNA SERIE.
Con la tabla de contingencia de ayer: CADA CASILLA DE LA TABLA ES UNA SERIE.

## Cardinalidad = dinero

CARDINALIDAD: número de valores distintos que puede tomar una etiqueta.

    shop.orders.created  lleva:  status   (2 valores: paid, payment_failed)
                                 category (4 valores)
                                 -> 2 x 4 = 8 series.  Barato y útil.

Datadog cobra las métricas propias (CUSTOM) POR NÚMERO DE SERIES.

> Pregunta: le añado la etiqueta customer_id. Tenemos 1.000.000 de clientes. ¿Cuántas series?
    8 x 1.000.000 = 8.000.000 de series.
    - La factura se dispara.
    - Y la tabla ya NO CLASIFICA NADA: cada fila es un cliente.

REGLA: en métricas, solo etiquetas con POCOS valores:
    status, categoría, país, endpoint (sin ids), versión, entorno...
NUNCA: id de cliente, id de pedido, trace id, email, URLs con ids, fechas, texto libre.
    Eso va en LOGS y TRAZAS (que se pagan de otra forma).
    -> Es lo de "etiqueta vs identificador" de esta mañana.

## Tipos de métrica: 2 preguntas

La app NO manda cada dato tal cual a Datadog. Lo RESUME antes de enviarlo.
El TIPO de métrica = CÓMO SE RESUME. Lo que se tira al resumir NO SE RECUPERA.

Ejemplo: en 10 segundos entran 3 pedidos: 20 €, 50 €, 200 €

    PREGUNTA 1: ¿Mando CADA DATO o lo CONSOLIDO antes?

        Mando cada dato       -> DISTRIBUTION      mando 20, 50, 200
                                 Datadog puede hacer EL HISTOGRAMA y todos los indicadores:
                                 mediana, p95, p99, media, máximo, suma (ingresos), recuento (pedidos)
                                 El más completo. El más caro.

        Consolido             -> COUNT o GAUGE     solo sobrevive el resumen

    PREGUNTA 2: si consolido... ¿el dato es ADITIVO?

        SÍ  -> COUNT    "cuántas veces ha pasado algo en este intervalo"
                        mando: pedidos = 3
                        Los pedidos de las 10:00 + los de las 10:01 = los de esos 2 minutos.
                        Es la FRECUENCIA de la tabla de frecuencias.
                        No sé de cuánto fueron los pedidos.

        NO  -> GAUGE    "cuánto vale AHORA" (una foto)
                        mando: stock_teclados = 487   (dentro de 1 minuto: 486)
                        CPU, memoria, conexiones abiertas, tamaño de una cola, temperatura...
                        El stock de las 10:00 + el de las 10:01 = NADA.

    Lo que pasó:       20 €   50 €   200 €

    count         ->   "3"                    solo cuántos
    gauge         ->   "200" (el último)      solo una foto
    distribution  ->   20, 50, 200            todo: histograma y percentiles

OJO con la aditividad del gauge: depende de EN QUÉ DIRECCIÓN sumo.
    - En el TIEMPO: un gauge NUNCA se suma.
    - Entre SERIES, en el MISMO instante: A VECES SÍ.
        Memoria de los 3 pods de shop-api a las 10:00 -> se suman = memoria total del servicio.   (bytes: RAZÓN)
        % de CPU de 3 máquinas: 90 + 80 + 70 = 240 % ... de NADA. Media o máximo.             (porcentaje)
    -> Es lo de ayer de RAZÓN vs INTERVALO, aplicado aquí.

EL TIPO LO DECIDE QUIEN ENVÍA EL DATO (desarrollo, o la integración del agente).

> Pregunta de PO: mañana quiero el p95 del tiempo de respuesta, o el ticket medio.
    El equipo tiene que mandar ESE DATO COMO DISTRIBUTION DESDE HOY.
    Si lo mandan como count... ese análisis NO EXISTIRÁ NUNCA.
    "Los cuadros de mando condicionan los datos que hay que capturar en origen."

## Las operaciones SÍ las elijo yo (en cada gráfica)

    El TIPO es lo que se manda: no lo cambio en la gráfica.
    La OPERACIÓN la elijo yo en cada consulta. El tipo solo decide:
        - el valor POR DEFECTO que me propone el editor (¡no siempre el que tiene sentido!)
        - QUÉ operaciones existen (percentiles: solo si es distribution)

En cada consulta hay DOS operaciones:

    1. Entre SERIES (desplegable a la izquierda de la métrica: avg / sum / min / max)
       Cómo combino las series que cumplen el filtro.
           sum:shop.orders.created{*}                  -> todos los pedidos juntos
           sum:shop.orders.created{*} by {status}      -> sumo, pero uno por estado
                                                          = TABLA DE FRECUENCIAS en el tiempo

    2. En el TIEMPO (rollup: avg / sum / min / max / count)
       Una gráfica de 1 día no puede pintar un punto cada 15 segundos.
       Agrupa los puntos en intervalos de varios minutos. ¿Cómo? Lo elijo yo.
           .rollup(sum, 300)    -> suma por intervalos de 5 minutos

    Para los contadores además:
           .as_count()   -> total por intervalo
           .as_rate()    -> por segundo

    Y encima: fórmulas (a / b * 100), timeshift (comparar con ayer), percentiles, anomalías...

Un COUNT se suma en las dos direcciones.
Un GAUGE se suma como mucho entre series, y solo si la magnitud lo permite (bytes sí, % no).

## El error de todo el mundo: un contador con avg

Metrics -> Explorer -> shop.orders.created
El editor pone avg por defecto -> una línea casi plana en 0,3.
    "¿No vendemos?"
Está PROMEDIANDO RECUENTOS: intervalos con 1 pedido, con 0... da 0,3. No significa nada.
    sum + .as_count()          -> los pedidos reales por intervalo
    ... by {status}            -> la tabla de frecuencias de ayer, en el tiempo

LA OPERACIÓN DEPENDE DEL TIPO DE DATO. (Los hijos y la temperatura de ayer.)

## Metrics Summary y Metrics without Limits

METRICS SUMMARY (Metrics -> Summary): el INVENTARIO de todas las métricas.
    De cada una: tipo, unidad, descripción, qué etiquetas lleva, quién la manda
    y CUÁNTAS SERIES TIENE  -> LA FACTURA ANTES DE QUE LLEGUE.
    Si una métrica tiene 200.000 series... alguien ha metido un id.
    Aquí también se corrigen unidad, descripción y tipo (si alguien lo declaró mal;
    eso no recupera lo que no se mandó).

METRICS WITHOUT LIMITS (en cada métrica: botón "Manage Tags"):
    Elijo QUÉ ETIQUETAS SE CONSERVAN. Las demás se descartan al llegar.
    Pago solo por las que conservo.
    Alguien despliega a las 3 am una métrica con customer_id:
        -> se arregla aquí en 1 minuto, SIN TOCAR CÓDIGO, mientras el equipo corrige la app.
    EL FRENO DE EMERGENCIA DE LA FACTURA.

## En pantalla (30 min)

    1. Metrics -> Explorer: shop.orders.created tal cual -> línea plana. "¿No vendemos?"
       Cambiar a sum + .as_count() -> aparecen los pedidos.
    2. Añadir  by {status}  -> "vuestra tabla de frecuencias de ayer, en el tiempo".
    3. shop.orders.value (distribution): p50, p95, sum (ingresos), count (pedidos). Un dato, todo.
    4. Metrics -> Summary -> shop.orders.created: sus etiquetas y su nº de series.
       La pregunta del millón de clientes.
    5. Botón "Manage Tags": el freno de emergencia.

---

# SEÑALES DORADAS (Golden signals)

Las 4 PREGUNTAS MÍNIMAS que tengo que poder contestar de CUALQUIER servicio.
Vienen del libro de SRE de Google: si solo puedo vigilar 4 cosas... estas.

    Señal        Pregunta                      En la tienda (shop-api)                          Para un PO
    ----------   ---------------------------   ----------------------------------------------   ------------------------------------------
    LATENCIA     ¿Cuánto tardan?               p50 / p95 / p99 de POST /api/orders              ¿Cuánto espera un cliente para comprar?
    TRÁFICO      ¿Cuánta demanda hay?          peticiones/segundo, pedidos/minuto               ¿Cuánta gente lo está usando?
    ERRORES      ¿Cuántas fallan?              % de peticiones 5xx, % de pagos fallidos         ¿Cuántos intentos de compra salen mal?
    SATURACIÓN   ¿Cómo de lleno está?          CPU y RAM del pod vs su límite,                  ¿Cuánto margen queda antes de que
                                               conexiones a Postgres, colas                     todo vaya lento?

Matices (casi todo dado ayer):

    LATENCIA     con PERCENTILES. NUNCA la media (ayer: mediana y p99).
                 Y la latencia de las peticiones CON ERROR, aparte:
                 un error rápido "mejora" la media y engaña.

    ERRORES      como TASA, no como número.
                 100 errores sobre 10.000 peticiones   =  1 %
                 100 errores sobre    200 peticiones   = 50 %
                 Es un count / otro count:   errores / peticiones * 100

    TRÁFICO      es el CONTEXTO de las demás.
                 Latencia alta a las 3 am con 2 peticiones   !=   con 2.000.

    SATURACIÓN   es el AVISO ADELANTADO.
                 Las otras 3 miden el SÍNTOMA cuando ya pasa.
                 La saturación avisa de que VA a pasar.

## El ORDEN importa: el orden de mirar en un incidente

    1. ERRORES       ¿Está roto?
    2. LATENCIA      ¿Va lento?
    3. TRÁFICO       ¿Hay más demanda de lo normal?... ¿o ninguna?
    4. SATURACIÓN    ¿Por qué? ¿Nos hemos quedado sin recursos?

    1, 2 y 3  ->  lo que NOTA EL USUARIO  (síntomas)
    4         ->  la CAUSA probable

Y es el orden de arriba a abajo de un dashboard de guardia.

## Variantes con nombre (les sonarán)

    RED   (Rate, Errors, Duration)              las 3 primeras. Para SERVICIOS que reciben peticiones (shop-api)
    USE   (Utilization, Saturation, Errors)     para RECURSOS: CPU, disco, red. La 4ª señal desglosada.

## Y esto es la puerta de entrada a los MONITORES

¿Sobre qué pongo alertas?
    ALERTAR SOBRE SÍNTOMAS, NO SOBRE CAUSAS.

    Despertar a alguien  ->  suben los ERRORES o la LATENCIA. Eso lo sufre el cliente.
    CPU al 90 % con los clientes contentos  ->  a un dashboard. NO se despierta a nadie.

> La alerta de las 3 am que luego no era nada... casi siempre era una alerta sobre una CAUSA.
