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
