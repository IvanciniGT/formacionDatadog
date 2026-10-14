

# Datadog

Es un producto de monitoreo y observabilidad que permite recopilar, analizar y visualizar:
- métricas
- logs
- trazas 
de aplicaciones y sistemas en tiempo real.

Hay muchos productos que hacen esas cosas por separado:
- Métricas: Prometheus/Graphana
- Logs: ELK (Elasticsearch, Logstash, Kibana) [hoy en día en modalidad opensource y gratuito: OpenSearch y Opensearch Dashboards], Loki
- Trazas: Jaeger/Zipkin

La gracia de datadog es que integra métricas, logs y trazas en una única plataforma, facilitando la observabilidad completa de aplicaciones y sistemas sin necesidad de combinar múltiples herramientas separadas.

# Cómo hace datadog esa integración?

Datadog usa etiquetas intensivamente (TAGS).
Toda métrica, traza, log lleva asociadas una serie de etiquetas (TAGS) que permiten cualificar y filtrar la información de manera granular.

PERO, hay un conjunto de etiquetas muy especial, que encuentro en todo elemento que recopila datadog: UNIFIED SERVICE TAGS (UST). Estas estiquetas son las que permiten saltar de métricas a logs a trazas de manera coherente, facilitando la correlación y el análisis completo de los datos dentro de la plataforma.

La integración se hace en base a un concepto muy simple.
Buscamos cosas que tengan las mismas UST y que ocurren en el mismo contexto temporal.

## ¿Cuales son?

Son 3:
- env       En qué entorno ocurre:  Valores típicos: prod, pre, dev (en nuestro caso: course)
- service   El componente LÓGICO de la aplicación o sistema que genera esa métrica, log o traza. (en nuestro caso: shop-web, shop-api, shop-db)
- version   La versión del componente lógico que genera la métrica, log o traza. (en nuestro caso: 1.0.0, 1.1.0, etc.)

COMPONENTE LOGICO, que no es COMPONENTE FISICO.
shop-api = COMPONENTE LOGICO:
  - código de java
  - se ejecuta en una máquina virtual java dentro de un contenedor
  - en el entorno de producción tenemos múltiples instancias de ese contenedor en varias máquinas físicas

## Version

En el mundo del software seguimos lo que se llama el esquema SEMANTICO de versiones: semver.

Una versión de un producto de software tiene eta pinta: vA.B.C
              
              Cuándo cambian?
A = MAJOR       Breaking changes: Cambios que rompen compatibilidad con versiones anteriores
                    Lo normal es que ocurran cuando QUITAMOS COSA (con o sin reemplazo)
B = MINOR       Nueva funcionalidad
                Marcar funcionalidad como obsoleta (Deprecated)
                    Adicionalmente pueden venir correcciones de defectos.
C = PATCH       Corrección de defectos: Bug-fixes

Hay un tema importante aquí.
Una cosa es la versión del producto, y otra la versión del despliegue!
Y es una decisión que debo tomar, cuál de las dos pongo aquí!

Mi versión 2.3.1 de mi producto la puedo desplegar 5 veces en producción, con cambios entre si... en la configuración.

Lo más habitual es usar la versión del producto... Cada vez más miramos a la versión del despliegue.

Tengo mi app... y en un momento dado mi app genera un email, y ese email se manda vía un servidor de emails.
Pero en la empresa cambian el servidor de emails. La versión de mi app no cambia, pero la despliego de nuevo (PASO A PRODUCCION) para apuntar al nuevo servidor de emails... Esto cambia la versión del despliegue, pero no la versión del producto.

En muchos casos, lo que más interesa es llevar (en los UST) la versión del despliegue, ya que refleja el estado real del sistema en producción.
Y tener un tag independiente para la versión del producto.

# Sobre los UST

La idea es que AUTOMATICEMOS la asignación de valores a los UST.
Lo alguien tiene que poner eso a mano NO SE HARÁ. Eso será parte de los pipelines de CI/CD.

---

# ¿De dónde salen todo esos datos?

Al desplegar un sistema, esa herramienta / sistema tiene varios COMPONENTES! Y asu vez cada componente tiene subcomponentes o subsistemas..

En nuestro caso: Tenemos el sistema de la tienda.
  - shop-web
      - Se ejecuta en una máquina física
      - Dentro de la máquina hay un pod/contenedor
      - Dentro del contenedor tenemos una JVM (Java Virtual Machine)
      - Dentro de la JVM tenemos un Servidor de aplciaciones (TOMCAT)
      - Dentro del Servidor de aplicaciones tenemos nuestra aplicación web (shop-web)
  - shop-api
  - shop-db

Centrándonos solo en el shop-web. De dónde quiero recoger METRICAS?

      - Se ejecuta en una máquina física
      - Dentro de la máquina hay un pod/contenedor
      - Dentro del contenedor tenemos una JVM (Java Virtual Machine)
      - Dentro de la JVM tenemos un Servidor de aplciaciones (TOMCAT)
      - Dentro del Servidor de aplicaciones tenemos nuestra aplicación web (shop-web)

La respuesta es: DE TODOS ELLOS!

      - Máquina física:
        - Métricas de hardware (CPU, memoria, disco, red)
      - Dentro de la máquina hay un pod/contenedor
        - Métricas del contenedor (CPU, memoria, disco, red)
      - Dentro del contenedor tenemos una JVM (Java Virtual Machine)
        - Métricas de la JVM (CPU, memoria asignada, memoria uso, GC, hilos-THREADS)
      - Dentro de la JVM tenemos un Servidor de aplciaciones (TOMCAT)
        - Métricas del servidor de aplicaciones (ejecutores, pool de conexiones a BBDD, queue de peticiones)
      - Dentro del Servidor de aplicaciones tenemos nuestra aplicación web (shop-web)
        - Métricas de NEGOCIO:  
          - Número de ventas
          - Ingresos generados
          - Número de usuarios activos
          - Tasa de conversión
          - Stock de un producto que vendo
          - ...  

Cuando configuro datadog y mi sistema (cada uno de esos componentes con sus subcomponentes) para que esté en conexión con datadog, hay que contarle todo eso.
- Seremos acaso las primeras personas del mundo que usan datadog para sacar métricas de hardware de una máquina? NO
  Y de hecho, el agente de datadog extrae esas métricas de hardware automáticamente.
- Seremos acaso las primeras personas del mundo que usan datadog para sacar métricas de los contenedores? NO
  Y de hecho, el agente de datadog extrae esas métricas de los contenedores automáticamente.
- Seremos acaso las primeras personas del mundo que usan datadog para sacar métricas de la JVM? NO
  Y de hecho, el agente de datadog extrae esas métricas de la JVM automáticamente SIEMPRE QUE SE LO PIDA!
  Pedirlo es fácil: OYE CAPTURA METRICAS DE LA JVM. Cuales? Pues las que son... si todo capturamos las mismas métricas.
- Seremos acaso las primeras personas del mundo que usan datadog apra sacar métricas del servidor de aplicaciones? NO
- Seremos acaso las primeras personas del mundo que usan datadog apra sacar métricas de nuestra aplciación web? SI
  Y aquí está el problema. Mis métricas de negocio las defino YO! Nadie en el mundo puede definirlas por mi.
  Datadog si me ofrece unas librerías parea poder exportar mis métricas de forma sencilla al agente. Para que a su vez el agente las envíe a la plataforma de Datadog.

  El problema es que eso va embebido en el códig (JAVA en nuestro caso)!
  Este código lo escriben los desarrolladores.
  Y si no les digo las métricas que quiero, ellos las van a adivinar? decidir por mi? CLARO QUE NO!

  El definir esas métricas es responsabilidad mía como propietario del negocio. Nadie más puede hacerlo por mi.
  Será parte de los requisitos del sistema!

  NO HABRA FORMA EN EL MUNDO DE SACAR LUEGO LA INFORMACIÓN QUE ME INTERESA SI NO ESTÁ REGISTRADA!

  Muchas veces tratamos de sacar estos datos de logs! ESTO ES HORRIBLE! Porque no están pensados para ello.

  La monitorización la activamos sobre métricas explícitas que hemos definido y registrado en nuestro código.

## Métricas en datadog:

```java
    public void orderCreated(String category, String status) {
        client.incrementCounter("orders.created", "category:" + category, "status:" + status);
    }

    public void orderValue(double euros, String category) {
        client.distribution("orders.value", euros, "category:" + category);
    }

    public void paymentDuration(long millis, boolean ok) {
        client.histogram("payment.duration", millis, "status:" + (ok ? "ok" : "failed"));
    } 
    // Esta que hay aqui no debería estar aquí!
    // No debería tratarse como una métrica... conceptualmente es una traza!
    // Pero necesitaba un ejemplo apra incluir los histogramas.

    public void stockLevel(String productName, int stock) {
        client.gauge("stock.units", stock, "product:" + productName);
    }
```

Hay 4 tipos principales de métricas en Datadog: 

1. Contadores (Counters): Algo que puedo contar: 
    Cúantos pedidos se han creado.
    Cuantos productos se han vendido.
    Son datos aditivos... los puedo sumar entre si!

    > En el minuto 1 vendí 5 productos
    > En el minuto 2 vendí 3 productos
    > En total he vendido 8 productos

2. Medidores (Gauges): Dan el valor actual de un determinado indicador en un momento dado.
    Stock actual del producto X.
    El problema con estos (o la diferencia) es que no son aditivos en todas las dimensiones... especalmente NO LO SON en la dimensión temporal.

    > En el minuto 1 el stock del producto X era 10
    > En el minuto 2 el stock del producto X era 8
    > En total el stock del producto X AHORA es 8 . NO SUMO NI RESTO NADA.

    OJO... puede tener sentido sumar en otras dimensiones!
    > Tienda 1 tiene AHORA un stock del producto X de 10 unidades.
    > Tienda 2 tiene AHORA un stock del producto X de 8 unidades.
    > En total el stock del producto X AHORA es 18 unidades.

3. Distribuciones (Distributions)
4. Histogramas (Histograms)

## Sobre distributions e histogramas

Ambos son similares.
Permiten analizar la distribución de valores de una métrica a lo largo del tiempo.
Podré sacar widgets de tipo distribution en los dashboards.. podré genera/calcular sus percentiles directamente desde Datadog.

La diferencia es quién ahce el trabajo de agregación:
- En los histogramas, el AGENTE que tengo localmente va haciendo la agregación de los datos antes de enviarlos a Datadog.
- En las distribuciones, los datos se envían a Datadog sin agregación previa, y la agregación se hace a posteriori bajo demanda.

El uso cambia:

Tengo el sistema rodando en varios HOSTS, y por ende cada uno tiene su AGENTE!
- Capturo el tiempo que necesito para hacer transacciones de un determinado tipo: PAGO DE UNA OPERACION DE VENTA
                                                                                  IMPORTE DE LA VENTA

Si la agregación se hace a nivel de host (histogramas), cada host enviará sus datos ya agregados a Datadog, y los percentiles calculados reflejarán la agregación local. >>>> Y no pueden combinarse entre si!

    HOST1: Las pagos tienen un P95 de 1000s
    HOST2: Las pagos tienen un P95 de 2000s

    >>>> No puedo combinar estos percentiles directamente para obtener un P95 global.

Si la agregación se hace a posteriori en Datadog si puedo combinar los datos de todos los hosts y calcular percentiles globales de manera precisa.

Lo que pasa es que los "distribution" son más CAROS (€€€), ya dijimos que me cobran por todo los de datadog. Por cada métricacustom me cobran adicionalmente. Y las distribution son más costosas que los histogramas.

# Sobre los pagos y los costes

De hecho... el modelo de pago es más complejo... se cobra por serie de datos dentro de una métrica activa por hora.

Si tengo por ejemplo 40 versiones históricas de mi app, cada una es una serie de datos.. pero ya no meto datos en las viejas... Por esas no me cobran.

Y cada combinación de etiquetas genera una serie de datos independiente que se cobra por separado.

Si tengo 3 entornos, 40 versiones de mi aplicación.. ahí ya vamos por 120 series de datos independientes que se cobran por separado.
Si se me ocurre meter el cliente como otra etiqueta, cada cliente generará una serie de datos independiente que se cobrará por separado.
Si tengo 10.000 clientes... flipas! La fatura se dispara hasta el infinito y más allá. Esos son los datos que dejamos en logs.

## Resumen sobre métricas custom:

- Debe ir como parte de los requisitos
- Debo darles un nombre, un tipo y etiquetas adecuadas (con baja cardinalidad).

---

# Logs

Los logs amplian información sobre las actividades que se realizan en el sistema, proporcionando contexto adicional que no siempre 

Ejemplo: METRICA: VENTA NUEVA, SUMA 1
Ejemplo: LOG: VENTA NUEVA, CLIENTE=ivan, ESTADO=PAID, PRODUCTO=camisa, CANTIDAD=1, TALLA=L

## Niveles de logs 

- Trace     Información muy detallada, generalmente solo útil para depuración profunda del sistema
- Debug     Información detallada para depuración del sistema
------------------------------------------------------------------- ^ Esto es lo que interesa a desarrollo (EJEMPLO: QUERY EXACTA QUE LANZA UNA OPERACION SOBRE LA BBDD)
- Info      Eventos propios del sistema (arranque, parada...) + Eventos de NEGOCIO que me interesa registrar.
                                                                        ^^^^^^^^
                                                                        ESTOS SON LOS QUE ME INTERESAN DESDE NEGOCIO
------------------------------------------------------------------- V Sobre todo interesan para sistemas / operación
- Warn      Fallos que no provocan la interrupción del sistema / servicio o pérdida de datos
- Error     Errores del sistema

Los desarrolladores escriben entradas en el registro de todos esos tipos. Esos niveles son INCREMENTALES.
Y al ejecutar la app marcamos el nivel MINIMO DE LOG que quiero persistir. Si pido INFO, Se persisten INFO, WARN y ERROR, pero no DEBUG ni TRACE.

# Formato:

 2026-10-11 07:57:40.105 INFO  [http-nio-8081-exec-2] [dd.service=shop-api dd.env=course dd.version=1.0.0 dd.trace_id=6acb4173...c691 dd.span_id=5740378446534783345] c.course.shop.service.OrderService - Order created id=10 customer=ivan ... status=PAID
 ----------------------- ----  ---------------------  --------------------------------------------------- ---------------------------------------------------------- -----------------------------------  --------------------------------------------------
    FECHA / HORA         NIVEL   HILO                       UST de datadog                                   Si está disponible, la traza asociada                      En qué componente del código        Esto es el mensaje del log
                                                                                                                (donde se ejecutó esa operación)                        se genero esta entrada de log

Este formato debe ser parte de los requisitos del sistema.
Por eso siempre hablamos de requisitos funcionales y requisitos no funcionales.

En el formato hay 2 partes a definir:
- 2026-10-11 07:57:40.105 INFO  [http-nio-8081-exec-2] [dd.service=shop-api dd.env=course dd.version=1.0.0 dd.trace_id=6acb4173...c691 dd.span_id=5740378446534783345] c.course.shop.service.OrderService MENSAJE_PARTICULAR_QUE_REGISTRO
    Es la estructura GENERAL de cualquier ENTRADA del log
- Order created id=10 customer=ivan producto=camisa cantidad=1 talla=L
    Es la estructura ESPECIFICA de un tipo de entrada concreto
- Payment failed id=10 customer=ivan channel=credit_card amount=100
    Es la estructura ESPECIFICA de un tipo de entrada concreto

La estructura general DEBERIAMOS UNIFICARLA A NIVEL ORGANIZACION. Esto me ahorra mucho trabajo en el datadog.
La parte específica puede variar según el tipo de entrada. 

Es necesario definir un CATALOGO de los tipos de operaciones que deben registrarse en el log. Y los metadatos (TAGS algunos de ellos) que deben acompañar a cada entrada.
ESTO ES PARTE DE LOS REQUISITOS NO FUNCIONALES DEL SISTEMA. ESTO SE HACE POR APLICACION!

Si eso no se ha hecho, NO PODREMOS VER NADA EN DATADOG luego!

Ese catálogo puede ser simplemente un fichero con una tabla!

| Tipo de operación | Metadatos requeridos                    | Descripción                                                            |
|-------------------|-----------------------------------------|------------------------------------------------------------------------|
| order.placed      | id, customer, producto, cantidad, talla | Se registra cuando se crea un nuevo pedido en el sistema.              |
| order.failed      | id, customer, channel, amount           | Se registra cuando un pago falla en el sistema.                        |

## Logs estandar

Las herramienats que usamos externas ya generan sus logs:
- JAVA
- SPRING
- TOMCAT
- POSTGRES
- KUBERNETES

Ahí tengo poco que configurar.


---

# TRAZAS

En general las trazas es algo que interesa mucho a desarrollo... algo menos a negocio (pero también).
Básicamente nos permiten hacer seguimiento de una operación realizada por un usuario (o un sistema) a lo largo de su ciclo de vida.
Ejemplos:
- Un usuario hacer un pedido en la tienda, desde la selección de productos hasta el pago y la confirmación del pedido.
- Una ETL es ejecutada a las 3am

Lo que permiten es juntar/agrupar todas las acciones relacionadas con una misma operación, facilitando el seguimiento, la depuración y el entendimiento de cómo se realiza dicha operación.

Una traza se origina al iniciar la operación. 
- En el caso de una webapp, se genera en el frontal, cuando el usuario inicia una acción que da lugar a una operación, como hacer clic en un botón para realizar un pedido.
- En el caso de una ETL, se genera al inicio del proceso, cuando se ejecuta la tarea programada.

La traza lleva un identificador. Ese identificador acompaña a todas las acciones y eventos relacionados con la misma operación, permitiendo correlacionarlos y seguir el flujo completo de la operación.

> Ejemplo:
- El usuario aprieta en el botón de formalizar pedido en mi página de comercio electrónico.
  En el frontal se hace una petición al backend... en una determinada ruta http:
    POST /api/pedidos HTTP/1.1

        BODY
        {
            "customer": "67890",
            "producto": "abcde",
            "cantidad": 2,
            "talla": "M"
        }

    En ese momento, en el frontal, se genera un trace_id (algo como: 6acb417c69128bvh).
    Es un identificador de la operación DE ALTO NIVEL que hace nuestro usuario.

    Esa petición llega al servidor... y en servidor se hacen muchas tareas:
    1. Validar los datos recibidos en la petición.  SPAN("validar_datos_peticion", SPAN_ID= 32984987278943, TRACE_ID = 6acb417c69128bvh)
       - Que el cliente existe y está registrado    SPAN("validar_datos_peticion", SPAN_ID= 32984987278943, TRACE_ID = 6acb417c69128bvh)
       - Que le producto es un producto que realmente vendemos   SPAN("validar_datos_peticion", SPAN_ID= 32984987278943, TRACE_ID = 6acb417c69128bvh)
       - Que tenemos suficiente stock del producto    SPAN("validar_datos_peticion", SPAN_ID= 32984987278943, TRACE_ID = 6acb417c69128bvh)
       - ...
    2. Crear el pedido en la base de datos. <- Esto podría ser otro SPAN
    3. Tratar de hacer el cobro en automático con los datos preregistrados del usuario.
    4. Se deja un evento en KAFKA indicando que se ha creado un nuevo pedido, para:
       1.  que se vaya preparando el envío.
       2.  revisar si hace falta ampliar el stock del producto.
       3.  mandar un email al usuario confirmando la creación del pedido.
    5. contestar al frontend con el resultado de la operación.

    Todas esas operaciones van a recibir ese identificador de la traza (y por ende, el las lineas que escriba el usuario se guardará ese trace id en los logs)

    Además, una traza se divide en bloques llamados "spans", que representan unidades de trabajo dentro de la operación. Cada span tiene su propio identificador y puede tener un span padre, permitiendo reconstruir la jerarquía y el flujo de la operación completa.

    Por defecto, (en el caso de JAVA y de muchas otars herramientas), hay spans que se crean en automático:
    - Cuando voy a hacer una query a la base de datos, se crea automáticamente un span que representa esa operación.
    - Cuando voy a meter algo a kafka, se crea automáticamente un span que representa esa operación.

    Pero esto no pasa para todas las operaciones. Algunas operaciones requieren que creemos spans manualmente para poder rastrearlas correctamente. En general todo lo que sea lógica de negocio va a requerir spans manuales.

    Una gracia adicional de las trazas y los spans no es solo que AGRUPEN actividades relacionadas a una operación concreta, sino que MIDEN EN AUTOMATICO el tiempo que tarda cada operación en completarse, permitiendo identificar cuellos de botella y optimizar el rendimiento del sistema.

    OPERACION COMPRA DE UN PRODUCTO (TRAZA)                                                 2000 milisegundos
    1. Validar los datos recibidos en la petición. (SPAN)                                    200 milisegundos
       - Que el cliente existe y está registrado
       - Que le producto es un producto que realmente vendemos6acb417c69128bvh
       - Que tenemos suficiente stock del producto
       - ...
    2. Crear el pedido en la base de datos.   (SPAN)                                         500 milisegundos
    3. Tratar de hacer el cobro en automático con los datos preregistrados del usuario. 
    4. Se deja un evento en KAFKA indicando que se ha creado un nuevo pedido, para:(SPAN)    800 milisegundos
       1.  que se vaya preparando el envío.
       2.  revisar si hace falta ampliar el stock del producto.
       3.  mandar un email al usuario confirmando la creación del pedido.
    5. contestar al frontend con el resultado de la operación. (SPAN)                        

    Además, esto se integra con la monitorización a los usuarios (RUM).
    Una actividad que se ha grabado en RUM permite rastrear la interacción del usuario con la aplicación, y al estar vinculada a una traza, puede guardar el trace id asociado a esa interacción.

    Puede ser que haya tiempos que quiera controlar yo desde el punto de vista de negocio... y esos los controlo con METRICAS (METRICS).
    Desde el punto de vista de negocio me interesa saber cuánto tiempor ha tardado un usuario desde que entro en hacer el pedido hasta que este se completa.

# EVENTOS


Acción que ocurre en el sistema, asociada a un INSTANTE DE TIEMPO.
Planteándolo así, una venta es un evento en el sistema.
Un error es un evento en el sistema.

Y es cierto... Conceptualmente, lo que registramos en los logs son EVENTOS del sistema asociados a un instante de tiempo.

Pero en datadog le damos otro significado. 
Son cosas trascendetales que queremos que se identifiquen claramente en los gráficos / dashboards que generamos.

Una gráfica de ventas a lo largo del tiempo... por categorias.
Qué gráfico?
- histrógrama agrupado o apilado. Normalmente apilado.

Las ventas son eventos (conceptualmente)... pero las representamos en el propio gáfico (el área de las barras del histograma es proporcional al número de eventos/ventas que se han realizado en ese intervalo de tiempo).

Pero desde el punto de vista de datadog hay otra cosa a la que se llama eventos.
Los eventos salen representados como lineas verticales, que marcan un antes y un despues de un momento del tiempo.

    Ventas
     ^
     |                          EVENTO
     |                            |
     |                            |
     |    oo   xxx   ooooooooooooo|oooo   
     |   ooooxxxxxxxxxxxxxxxoooooo|oooooooooxxxxxxxxxxxxxxxooooooooooooooooooxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     |  xxxxxxxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  
     +----------------------------|---------------------------------------------------------------------------------> Tiempo
        enero   febrero   marzo   abril   mayo   junio   julio   agosto   septiembre   octubre   noviembre   diciembre

Qué se os ocurre que puedo marcar como evento en datadog?
- Campaña publicitaria
- Oferta especial
- Lanzamiento de un producto
- Despliegue de una nueva versión de mi app
- Cambio de un proveedor (cloud)


> Ejemplo sobre despliegue de nueva versión:

Si lo hago con logs...
Puede haber un log que escriba ARRANCANDO EL SISTEMA en versión 1.2.3
Y otro que diga:               ARRANCANDO EL SISTEMA en versión 1.2.4

El problema es que:
Esas lineas se ejecutarían en cada una de las inatancias de mi aplicación (contenedor)
De una app no se ejecuta una única instancia... se ejecutan muchas en paralelo (es es lo que me poermite asumir picos de carga)... además nos ofrecen HA(Alta Disponibilidad)
    DIA 1 puedo poner instancia 1 y otra 2
    Pero si el DIA 17 muere instancia 1, tengo que abrir Instancia 3 para reemplazarla.. 
    Y esa al arrancar, generaría: ARRANCANDO EL SISTEMA en versión 1.2.4

ESTO LO TRATARIAMOS COMO UN EVENTO!

Datadog tiene su API para dar de alta eventos.
Se lo puedo pedir al agente:
```bash
datadog-agent event post "Despliegue de nueva versión" \
  --text "ARRANCANDO EL SISTEMA en versión 1.2.4" \
  --tags "app:mi_app,env:produccion,version:1.2.4"
```

O desde código JAVA por ejemplo.

```java
    public void event(String title, String text) {
        client.recordEvent(com.timgroup.statsd.Event.builder()
                .withTitle(title).withText(text)
                .withAlertType(com.timgroup.statsd.Event.AlertType.INFO).build());
    }
```

Habrá eventos más de sistema, como por ejemplo:
- Despliegue de la nueva versión de la aplicación
- Rollback a una versión anterior del sistema
- Cambio en una configuración en caliente

Pero luego tenemos eventos más relacionados con el negocio, como por ejemplo:
- Campaña publicitaria
- Oferta especial
- Lanzamiento de un producto

Esto hay que definirlo.
Lo normal es que en compañía haya una POLITICA GLOBAL donde se definan eventos de sistema.

En cada proyecto/producto, tendré que definir si quiero registrar eventos de negocio adicionales a los de sistema.

# Sobre despliegues!

En la parte de APM (solo en la parte de APM), en las gráficas que se me generan por defecto, la primera vez que se registra algo con tag "version" con un valor diferente al que se hubiera usado anteriormente , datadog muestra una línea vertical indicando el cambio de versión.

Con independencia de que esto me lo regalen, es recomendable seguir registrando eventos de despliegue de manera explícita para tener un historial completo y poder correlacionar con otros eventos del sistema y del negocio.

# Nota 2

Tenemos la parte de CI/CD... eso tampoco me genera en automático eventos de despliegue.
Lo normal es que yo, dentro de un pipeline de CI/CD genere esos eventos.
Eso lo haría en el github actions.

El datadog lo que hará es recopilar la información de los despliegues que se generen desde el pipeline de CI/CD y mostrarlos en su interfaz, permitiendo correlacionarlos con otros eventos y métricas.

---

# Sobre DEVOPS y CI/CD

Ayer dije que la clave de DEVOPS es AUTOMATIZAR.

## Automatizar?

Es crear una MAQUINA ( o cambiar el comportamiento de una mediante programas) que hagan lo que antes hacia un humano con sus manos.

Puedo automatizar el lavado de la ropa (LAVADORA), incluso , puedo cambiar su comportamiento con PROGRAMAS DE LAVADO (el de ropa fría, prendas delicadas).
En nuestro mundo, la máquina ya la tenamos creada (COMPUTADORA). En nuestro mundo AUTOMATIZAR = PROGRAMAR!

HAsta ahora , en un proyecto teníamos un programa (mi sistema), Ahora tengo 2:
- Mi programa/sistema
- El programa que despliega mi programa/sistema. (Un pipeline de despliegue continuo)
  -  Estos programas son más simples: SCRIPTS!
- El programa que prueba mi programa/sistema para generar un informe de pruebas en tiempo real (Un pipeline de Integración Continua)

Esos programas , como programas que son tendrán:
- Su propio control de versiones
- Su propio historial de cambios
- Su propios despliegues
- Sus ejecuciones, independientes de las de mi sistema principal.

Y querré monitorizarlos y observarlos... eso es lo que me da la parte de CI/CD de datadog.
Una cosa es ejecuatarlos (eso es lo que hacemos en GITHUB ACTIONS)
Otra cosa es monitorizarlos y observar su comportamiento, eso es lo que nos proporciona la integración de CI/CD en datadog.

Y en cuanto uso estos programas , si tengo DATADOG, quiero monitorizarlos y observar su comportamiento en tiempo real.

De hecho, esos pipelines se organizan en STAGES (bloques de trabajo)... En automático, en datadog, cada ejecución de uno de esos programas genera UNA TRAZA!
Y cada STAGE es un SPAN dentro de la TRAZA correspondiente a la ejecución del pipeline. Se hace solo.
Es el mismo tratamiento que con lo otro.

Esto me da muchas cosas:
- Ver en tiempo real el comportamiento de mis pipeline y su evolución en el tiempo.
- Deployments / DORA. Métricas DORA (qué tal trabaja un equipo): 
  - Lead Time for Changes (Tiempo desde que se hace un cambio hasta que se despliega)
  - Deployment Frequency (Frecuencia de despliegues)
  - Change Failure Rate (Tasa de fallos en los cambios)
  - Mean Time to Recovery (Tiempo medio de recuperación)
- Seguridad: Detección de vulnerabilidades conocidas.
  Un sistema lleva muchos subcomopoentes. Y se van publicando problemas de seguridad en ellos. 
    AQUI HAY CIERTO SOLAPAMIENTO CON GITHUB ACTIONS / SONARQUBE
- Tests Optimization (Optimización de pruebas):
  - Pruebas que fallan random
  - Pruebas que tardan demasiado tiempo en ejecutarse

De alguna forma esto completa la foto... pero es independiente al sistema principal.
---



# Datos que nos interesan como Product Owners?

SLI     Actual      |
SLO     Objetivo    |
SLA     Acuerdo

Número de ventas
Ventas por producto
Tiempo de checkout

## Cómo definimos el SLO?
Nos acabamos de dar cuenta que no hay una definición ÚNICA de lo que debería ser el SLO...
En realidad el SLI...
Porque el SLO = SLI + Objetivo

- Cuantas peticiones se lanzan al backend que el backend es capaz de procesar / cuantas no se procesaron bien
- Tiempo de respuesta del backend. Los que tardan menos 1000ms vs los que tardan más de 1000ms
- Ventas por producto vs Accesos al producto = TASA DE CONVERSION


---

# Ingeniería de plataformas?

Es el siguiente paso a DEVOPS.
Lo de la automatización está guay... solo tiene una cosita? 
- QUE CUESTA UNA PASTA A CORTO PLAZO DE NARICES
- HAY QUE MANTENERLA EN EL TIEMPO
- Si cada proycto va a su bola -> MUY BAJA ESTANDARIZACION:
 - Hago lo mismo una y otra vez... con los mismos errores, una y otra vez! ^^^ PRESUPUESTO
 - Solo las personas del proycto conocen los detalles y las decisiones tomadas. Que no se vaya nadie ni venga nadie nuevo..

Ingeniería de plataformas: Plantear de forma centralizada todo lo que hace falta para comenzar un proyecto nuevo.. y entregarlo al equipo de desarrollo/negocio

Hay un concepto que usamos mucho que es el de IdP : Plataforma Interna para Desarrolladores.
Cuando un equipo comienza un nuevo proyecto, en lugar de tener que reinventar la rueda y configurar todo desde cero, puede apoyarse en la IdP para obtener todas las herramientas, configuraciones y buenas prácticas necesarias de manera centralizada.
- El IDP le genera:
  - Repos de git
  - Plantillas de la app
  - Pipelines de CI/CD
  - Monitorización
  - SLOs (Service Level Objectives)
