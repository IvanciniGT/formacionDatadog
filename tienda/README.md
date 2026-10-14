# La tienda del curso: de dónde sale cada dato

Esta carpeta es una copia de la aplicación de demo que corre en https://datadog.ivanosuna.com,
para ver **qué hay que poner, y dónde, para que cada dato llegue a Datadog**.
No hace falta saber programar: basta con saber en qué fichero mirar.

```
tienda/
  app/                 shop-api: el backend en Java (Spring Boot)
    Dockerfile         cómo se construye la imagen (y dónde se mete el agente de Datadog)
    pom.xml            las librerías que usa
    src/main/java/...  el código
    src/main/resources/
      application.yaml     configuración de la app (base de datos)
      logback-spring.xml   el formato de los logs
  web/                 shop-web: la página que ve el usuario
    index.html         la página, con el SDK de RUM
    entrypoint.sh      mete las credenciales de RUM en la página al arrancar
  nginx/nginx.conf     sirve la página y reenvía /api a shop-api
  postgres/init.sql    tablas, datos iniciales y el usuario de Datadog en la base de datos
  k8s/app.yaml         CÓMO SE DESPLIEGA la tienda en Kubernetes (con sus variables)
  k8s/values.yaml      CÓMO SE CONFIGURA el agente de Datadog en el clúster
  generate-load.sh     el "robot" que simula usuarios
```

---

## 1. Las tres etiquetas que lo enlazan todo: env, service, version

Se ponen **en el despliegue**, no en el código. En `k8s/app.yaml`, en el Deployment de `shop-api`:

```yaml
      labels:
        tags.datadoghq.com/env: course          # <- env
        tags.datadoghq.com/service: shop-api    # <- service
        tags.datadoghq.com/version: "1.0.5"     # <- version: la cambia el pipeline
...
          env:
            - name: DD_SERVICE
              valueFrom: { fieldRef: { fieldPath: "metadata.labels['tags.datadoghq.com/service']" } }
            - name: DD_ENV
              valueFrom: { fieldRef: { fieldPath: "metadata.labels['tags.datadoghq.com/env']" } }
            - name: DD_VERSION
              valueFrom: { fieldRef: { fieldPath: "metadata.labels['tags.datadoghq.com/version']" } }
```

- Las **labels** del pod son el **único sitio** donde se escriben los valores. El agente de Datadog
  las lee para etiquetar las métricas del contenedor y sus logs.
- Las **variables** `DD_*` no repiten los valores: los **leen de las labels**. Las usa el tracer dentro
  de la app para etiquetar las trazas y los logs.
- En la **página web** van en el código JavaScript (ver punto 5).

**Un solo sitio, y que lo actualice el pipeline.** Cada dato duplicado es una ocasión de que dos
fuentes no coincidan, y en observabilidad eso falla en silencio: si la versión está escrita en tres
sitios y alguien solo cambia uno, Datadog dice una cosa y el clúster otra.

---

## 2. Trazas: el agente de Java (sin tocar código)

En `app/Dockerfile`:

```dockerfile
ADD https://dtdg.co/latest-java-tracer /app/dd-java-agent.jar
...
ENTRYPOINT ["java", "-javaagent:/app/dd-java-agent.jar", "-jar", "/app/shop-api.jar"]
```

**Esa línea `-javaagent` es todo el APM.** Al arrancar, ese jar se engancha a Java y, sin tocar el
código, registra cada petición HTTP, cada consulta a la base de datos, los errores, y la memoria y el
recolector de basura de la JVM.

Sus opciones van como variables en `k8s/app.yaml`:

```yaml
            - name: DD_AGENT_HOST                    # dónde está el agente de Datadog: en el propio nodo
              valueFrom: { fieldRef: { fieldPath: status.hostIP } }
            - name: DD_LOGS_INJECTION                # mete el trace id en cada línea de log
              value: "true"
            - name: DD_TRACE_SAMPLE_RATE             # guardar el 100 % de las trazas (en una demo)
              value: "1"
            - name: DD_PROFILING_ENABLED             # qué línea de código gasta CPU/memoria
              value: "true"
            - name: DD_DBM_PROPAGATION_MODE          # pone el trace id como comentario en el SQL
              value: full
            - name: DD_TRACE_OTEL_ENABLED            # acepta la API estándar de OpenTelemetry (punto 4)
              value: "true"
```

---

## 3. Métricas de negocio: hay que pedirlas en el código

Las métricas de CPU, memoria, peticiones y errores salen solas (del agente del nodo y del agente
de Java). Pero **Datadog no sabe qué es "un pedido pagado"**. Eso lo decide un desarrollador:

**`app/src/main/java/com/course/shop/service/Metrics.java`**: una clase que envía métricas al
agente de Datadog (por UDP, puerto 8125, protocolo DogStatsD). Define cinco:

```java
client.incrementCounter("orders.created", "category:" + category, "status:" + status);  // COUNT
client.distribution("orders.value", euros, "category:" + category);                     // DISTRIBUTION
client.histogram("payment.duration", millis, "status:" + ...);                           // HISTOGRAM
client.gauge("stock.units", stock, "product:" + productName);                            // GAUGE
client.recordEvent(...)                                                                   // EVENTO
```

(Con el prefijo `shop.`: en Datadog se llaman `shop.orders.created`, etc.)

**Y alguien tiene que llamarlas en el sitio donde pasa el hecho de negocio.** En
`service/OrderService.java`, al terminar de crear un pedido:

```java
metrics.stockLevel(product.getName(), product.getStock());                    // stock que queda
metrics.orderCreated(product.getCategory(), order.getStatus());               // un pedido más, con su estado
metrics.orderValue(order.getTotal().doubleValue(), product.getCategory());    // su importe
```

Fijaos en **las etiquetas**: `category` y `status`, con pocos valores. Nunca el id del cliente ni del
pedido (cardinalidad = factura).

---

## 4. Trazas de negocio: lo que el agente automático no ve

El agente de Java ve "una llamada HTTP" o "una consulta SQL", pero no sabe que un trozo de código es
"cobrar". Dos añadidos pequeños:

**Un paso propio en la traza** (`service/PaymentGateway.java`):

```java
@Trace(operationName = "payment.charge", resourceName = "PaymentGateway.charge")
public String charge(String customer, double amount) { ... }
```

Esa anotación hace que "cobrar" aparezca como su propio span en el flame graph.

**Etiquetas de negocio en la traza** (`service/OrderService.java`):

```java
Span span = Span.current();
span.setAttribute("order.category", product.getCategory());
span.setAttribute("order.quantity", quantity);
span.setAttribute("usr.name", customer);
```

Con esto se pueden buscar trazas por categoría o por cliente. (Aquí sí vale el cliente: las trazas no
se cobran por cardinalidad como las métricas.)

---

## 5. La experiencia del usuario: el SDK de RUM en la página

En `web/index.html`:

```html
<script src="https://www.datadoghq-browser-agent.com/eu1/v6/datadog-rum.js"></script>
<script>
  window.DD_RUM.init({
    applicationId: ...,  clientToken: ...,       // identifican la app RUM (las pone entrypoint.sh)
    site: 'datadoghq.eu',
    service: 'shop-web', env: 'course', version: '1.0.0',   // las 3 etiquetas, también aquí
    sessionSampleRate: 100,                       // grabar el 100 % de las sesiones
    sessionReplaySampleRate: 100,                 // y su "vídeo"
    trackUserInteractions: true,                  // los clics
    defaultPrivacyLevel: 'mask-user-input',       // lo que se escribe, enmascarado
    allowedTracingUrls: [ ... mismo dominio ... ] // CREA EL TRACE ID y lo manda al backend
  });
</script>
```

Y en los botones, nombres legibles para los clics: `btn.dataset.ddActionName = "buy " + nombre`.
Y quién es el usuario: `DD_RUM.setUser({ id: name, name })`.

Este script lo ejecuta **el navegador del usuario** y manda los datos directamente a Datadog, sin
pasar por nuestros servidores.

---

## 6. Logs: lo que escribe la app, con el trace id dentro

En `app/src/main/resources/logback-spring.xml`, el formato de cada línea:

```
%d %-5level [%thread] [dd.service=%X{dd.service} dd.env=%X{dd.env} dd.version=%X{dd.version} dd.trace_id=%X{dd.trace_id} dd.span_id=%X{dd.span_id}] %logger - %msg%n
```

Los `%X{dd.trace_id}` los rellena el agente de Java (por `DD_LOGS_INJECTION=true`). Así cada línea de
log sabe a qué petición pertenece. Texto plano a propósito: Datadog tiene que **parsearlo** al
recibirlo (pipelines). Los logs de nginx, en cambio, van en JSON (`nginx/nginx.conf`), ya troceados.

¿Quién los recoge? El agente del nodo lee lo que cada contenedor escribe por pantalla. En
`k8s/app.yaml`, una anotación le dice qué tipo de log es (y así sabe parsearlo):

```yaml
      annotations:
        ad.datadoghq.com/shop-api.logs: '[{"source":"java"}]'
```

(`source: java` le dice qué tipo de log es, para que lo trocee con el pipeline de Java. El `service`
no hace falta: ya lo pone la label del pod.)

---

## 7. La base de datos: un usuario para Datadog

En `postgres/init.sql`, además de las tablas: un usuario `datadog` con permisos de solo lectura de
estadísticas, y una función para pedir planes de ejecución. Con eso, Datadog (DBM) ve qué consultas
se ejecutan, cuánto tardan y cómo las resuelve Postgres.

En `k8s/app.yaml`, en el Service de Postgres, la anotación que le dice al agente que se conecte:

```yaml
    ad.datadoghq.com/service.checks: |
      { "postgres": { "instances": [{ "host": "%%host%%", "port": 5432,
                                      "username": "datadog", "dbm": true, ... }] } }
```

---

## 8. El agente de Datadog: qué recoge y de dónde

`k8s/values.yaml` es la configuración del agente en el clúster (un agente por nodo):

```yaml
datadog:
  site: datadoghq.eu                 # a qué Datadog envía (Europa)
  clusterName: ivanosuna-cluster
  tags: [env:course, team:training]  # etiquetas que pone a TODO lo de ese nodo
  apm:        { portEnabled: true }  # recibe trazas (puerto 8126)
  dogstatsd:  { port: 8125 }         # recibe métricas custom
  logs:       { enabled: true, containerCollectAll: true }
  containerInclude: "kube_namespace:datadog-curso"    # SOLO los logs de este namespace
  processAgent:      { processCollection: true }       # procesos
  networkMonitoring: { enabled: true }                 # quién habla con quién (NPM)
  serviceMonitoring: { enabled: true }                 # latencia/errores sin tracer (USM)
```

Las claves de Datadog **no están aquí**: se meten desde un "secreto" que crea el despliegue.

---

## Resumen: cada dato, su origen

```
Dato en Datadog                       Lo produce                      Lo configura / lo pide
----------------------------------    -----------------------------   ------------------------------------------
CPU, memoria, red de nodos y pods      agente del nodo                 operaciones (values.yaml)
Peticiones, errores, latencia          agente de Java (-javaagent)     operaciones (Dockerfile + variables DD_*)
Memoria / GC de la JVM                 agente de Java                  nada: viene solo
Pedidos, importes, stock               código de la app (Metrics)      DESARROLLO, porque alguien lo PIDE (PO)
Paso "cobrar" en la traza              código (@Trace)                 desarrollo
Categoría del pedido en la traza       código (OpenTelemetry)          desarrollo
Logs con trace id                      app + logback + agente Java     desarrollo (formato) + operaciones (recogida)
Consultas SQL y sus planes             Postgres + agente (DBM)         operaciones / DBA (usuario datadog)
Sesiones, clics, errores JS, replay    navegador del usuario (RUM)     desarrollo front (index.html)
env / service / version                el despliegue                   ¡EL PIPELINE! (hoy, a mano)
```
