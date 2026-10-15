# Día 3

# EJEMPLO DE DASHBOARD: "Ivan - Tienda (referencia)"

https://app.datadoghq.eu/dashboard/s9r-yur-pc5/ivan---tienda-referencia

UN dashboard con DOS PESTAÑAS (una por perfil); en cada pestaña, GRUPOS que contestan UNA PREGUNTA.
Variable de plantilla:  $env  (por defecto: course)  -> el mismo dashboard vale para prod, pre, dev...

Regla de diseño:  PREGUNTA  ->  TIPO DE DATO  ->  WIDGET  ->  CONSULTA

Las consultas están COMPLETAS, tal y como están guardadas. En la interfaz se construyen con el FORMULARIO
(métrica, from, sum by, función as_count...). Para verlo: lápiz de editar el widget -> sale el formulario relleno.
Las fórmulas (a / b * 100) son lo único que se escribe a mano.

---

## PESTAÑA 1: NEGOCIO   (para: dirección comercial)

### ¿Cuánto vendemos?

    ¿Cuántos pedidos pagados llevamos hoy?
        Widget:   Query Value (número)   —   título: "Pedidos pagados (hoy)"
                  q1 =  sum:shop.orders.created{$env,status:paid}.as_count()
                  rango propio:  último día

    ¿Qué % de compras sale bien?
        Widget:   Query Value (número)   —   título: "% de compras con éxito"
                  a =  sum:shop.orders.created{$env,status:paid}.as_count()
                  b =  sum:shop.orders.created{$env}.as_count()
                  fórmula:  a / b * 100
                  semáforo:  verde si >= 65 · amarillo si >= 50 · rojo si < 50
                  unidad:  %

    ¿Cuánto importe suman los pedidos?
        Widget:   Query Value (número)   —   título: "Importe total pedidos (¡incluye NO pagados!)"
                  q1 =  sum:shop.orders.value{$env}
                  unidad:  €
                  OJO: la métrica shop.orders.value NO lleva status: suma también los pedidos
                  no pagados. Es un dato que HAY QUE PEDIR bien al equipo.

    ¿Vendemos más o menos que ayer?
        Widget:   Timeseries   —   título: "Pedidos pagados: hoy (barras) vs. ayer a la misma hora (línea)"
                  sum:shop.orders.created{$env,status:paid}.as_count()   [bars]
                  timeshift(sum:shop.orders.created{$env,status:paid}.as_count(), -86400)   [line]

    ¿Cuántos pedidos van bien y cuántos no?
        Widget:   Timeseries   —   título: "Pedidos por estado (área apilada)"
                  sum:shop.orders.created{$env} by {status}.as_count()   [area]
                  rayas de eventos:  sources:kubernetes kube_namespace:datadog-curso

### ¿Qué se vende?

    ¿Qué categoría vende más?
        Widget:   Pie / Sunburst (tarta)   —   título: "Pedidos pagados por categoría (tarta)"
                  q1 =  sum:shop.orders.created{$env,status:paid} by {category}.as_count()

    ¿Qué categoría trae más importe?
        Widget:   Treemap   —   título: "Importe por categoría (treemap)"
                  q1 =  sum:shop.orders.value{$env} by {category}

    Todo por categoría, en una tabla
        Widget:   Table (tabla)   —   título: "Resumen por categoría (tabla)"
                  pag =  sum:shop.orders.created{$env,status:paid} by {category}.as_count()
                  fal =  sum:shop.orders.created{$env,status:payment_failed} by {category}.as_count()
                  imp =  avg:shop.orders.value{$env} by {category}
                  fórmula:  pag   (columna "Pagados")
                  fórmula:  fal   (columna "Pago fallido")
                  fórmula:  fal / (pag + fal) * 100   (columna "% fallo")
                  fórmula:  imp   (columna "Ticket medio (€)")

    ¿De cuánto es un pedido normal?
        Widget:   Query Value (número)   —   título: "Ticket medio"
                  q1 =  avg:shop.orders.value{$env}
                  unidad:  €

    ¿Y el pedido más grande?
        Widget:   Query Value (número)   —   título: "Ticket máximo"
                  q1 =  max:shop.orders.value{$env}
                  unidad:  €
                  (El p50 / p95 del ticket necesitan ACTIVAR PERCENTILES en la métrica:
                   se pagan y solo se calculan DESDE ESE MOMENTO.)

    ¿Cuánto gasta un cliente "típico"? ¿Y los que más?
        Widget:   Query Value (número) x 2   —   títulos: "Ticket mediano (p50)" y "Ticket p95"
                  q1 =  p50:shop.orders.value{$env}
                  q1 =  p95:shop.orders.value{$env}
                  unidad:  €

    ¿Cómo se reparten los importes de los pedidos?
        Widget:   Distribution (histograma)   —   título: "Distribución del importe de los pedidos"
                  histogram:shop.orders.value{$env}
                  + nota amarilla con flecha: "Cómo leerla"
                  En la tienda: 10 productos de precio fijo y 1 unidad por pedido -> 10 BARRAS
                  (19,90 · 29 · 34,50 · 49,90 · 59 · 79 · 99 · 129 · 249 · 499 €).
                  El ticket MEDIO (~124 €) NO COINCIDE CON NINGÚN PEDIDO REAL: la media no representa a nadie.
                  La MEDIANA (p50) es lo que gasta la mitad de los clientes.

### ¿Cumplimos la promesa? ¿Funciona desde fuera?

    ¿Cumplimos la promesa? ¿Cuánto margen queda?
        Widget:   SLO   —   título: "SLO: peticiones procesadas correctamente"
                  SLO: "Ivan - Tienda - Peticiones Procesadas Correctamente" (7 días)

    ¿Se puede comprar ahora desde fuera?
        Widget:   Monitor Summary   —   título: "Robots (tests sintéticos)"
                  búsqueda:  type:synthetics

### ¿Quién nos visita y desde dónde? (RUM, 7 días)

    ¿Desde qué países nos visitan?
        Widget:   Geomap (mapa)   —   título: "Sesiones por país (mapa)"
                  q1 (RUM, recuento):  @type:session   agrupado por @geo.country_iso_code
                  rango propio:  última semana

    ¿Cuántos clientes reales?
        Widget:   Query Value (número)   —   título: "Sesiones de clientes reales"
                  q1 (RUM, recuento):  @type:session @session.type:user
                  rango propio:  última semana

    ¿Cuántas visitas son del robot?
        Widget:   Query Value (número)   —   título: "Sesiones del robot sintético"
                  q1 (RUM, recuento):  @type:session @session.type:synthetics
                  rango propio:  última semana

    ¿Personas o robots, a lo largo del tiempo?
        Widget:   Timeseries   —   título: "Sesiones por tipo (personas vs. robot)"
                  q1 (RUM, recuento):  @type:session   agrupado por @session.type
                  rango propio:  última semana

---

## PESTAÑA 2: GUARDIA   (para: quien está de guardia)

### ¿Está roto? ¿Va lento?

    ¿Qué % de peticiones falla?
        Widget:   Query Value (número)   —   título: "% de errores (shop-api)"
                  a =  sum:trace.servlet.request.errors{$env,service:shop-api}.as_count()
                  b =  sum:trace.servlet.request.hits{$env,service:shop-api}.as_count()
                  fórmula:  a / b * 100
                  semáforo:  verde si < 2 · amarillo si < 5 · rojo si >= 5
                  unidad:  %

    ¿Cuánto tarda comprar?
        Widget:   Query Value (número)   —   título: "p95 de la compra"
                  q1 =  p95:trace.servlet.request{$env,service:shop-api,resource_name:post_/api/orders}
                  semáforo:  verde si < 1 · rojo si >= 1
                  unidad:  s

    ¿Hay alertas saltando?
        Widget:   Monitor Summary   —   título: "Alertas de shop-api"
                  búsqueda:  service:shop-api

    Todo el servicio de un vistazo
        Widget:   Service Summary (APM)   —   título: "Resumen del servicio shop-api (APM)"
                  servicio shop-api, operación servlet.request, entorno course
                  peticiones, errores, latencia, desglose, distribución y lista de endpoints: TODO EN UNO

### ¿Dónde está el problema?

    ¿Qué endpoint falla o tarda más?
        Widget:   Table (tabla)   —   título: "Endpoints (tabla)"
                  h =  sum:trace.servlet.request.hits{service:shop-api,env:course} by {resource_name}.as_count()
                  e =  sum:trace.servlet.request.errors{service:shop-api,env:course} by {resource_name}.as_count()
                  p =  p95:trace.servlet.request{service:shop-api,env:course} by {resource_name}
                  fórmula:  h   (columna "Peticiones")
                  fórmula:  e   (columna "Errores")
                  fórmula:  e / h * 100   (columna "% error")
                  fórmula:  p   (columna "p95 (s)")

    ¿Cuáles son muy usados Y lentos?
        Widget:   Scatter Plot (dispersión)   —   título: "Endpoints: tráfico (x) vs. lentitud p95 (y)"
                  eje x:  sum:trace.servlet.request.hits{service:shop-api,env:course} by {resource_name}.as_count()
                  eje y:  p95:trace.servlet.request{service:shop-api,env:course} by {resource_name}
                  -> arriba a la derecha: los que más duelen (muy usados y lentos)

    ¿Cómo se reparten los tiempos de compra?
        Widget:   Distribution (histograma)   —   título: "Distribución del tiempo de compra (POST /api/orders)"
                  histogram:trace.servlet.request{service:shop-api,env:course,resource_name:post_/api/orders}

    ¿Qué endpoint da errores y cuándo?
        Widget:   Timeseries   —   título: "Errores por endpoint (barras)"
                  sum:trace.servlet.request.errors{service:shop-api,env:course} by {resource_name}.as_count()   [bars]
                  rayas de eventos:  sources:kubernetes kube_namespace:datadog-curso

    ¿Es culpa del proveedor de pagos?
        Widget:   Timeseries   —   título: "Proveedor de pagos: duración media por resultado (ms)"
                  avg:shop.payment.duration.avg{env:course} by {status}   [line]
                  marcador:  y = 500  (500 ms)

### Mapa del sistema

    ¿Quién llama a quién?
        Widget:   Service Map   —   título: "Mapa de servicios alrededor de shop-api"
                  servicio shop-api, filtro env:course

### ¿Por qué?

    ¿Qué dicen los logs de error?
        Widget:   Log Stream   —   título: "Errores en los logs de shop-api (en directo)"
                  búsqueda:  service:shop-api status:error

    ¿Qué ha cambiado en Kubernetes?
        Widget:   Event Timeline   —   título: "Eventos de Kubernetes del namespace"
                  búsqueda:  sources:kubernetes kube_namespace:datadog-curso

### ¿Tenemos recursos?

    ¿Cuánta CPU usan frente a su límite?
        Widget:   Timeseries   —   título: "CPU de los pods de shop-api (núcleos) vs. límite"
                  sum:kubernetes.cpu.usage.total{kube_namespace:datadog-curso,kube_deployment:shop-api} by {pod_name} / 1000000000   [line]
                  sum:kubernetes.cpu.limits{kube_namespace:datadog-curso,kube_deployment:shop-api} by {pod_name}   [line]

    ¿Cuánta memoria usan frente a su límite?
        Widget:   Timeseries   —   título: "Memoria de los pods de shop-api vs. límite"
                  sum:kubernetes.memory.usage{kube_namespace:datadog-curso,kube_deployment:shop-api} by {pod_name}   [area]
                  sum:kubernetes.memory.limits{kube_namespace:datadog-curso,kube_deployment:shop-api} by {pod_name}   [line]

    ¿Qué pods del clúster consumen más?
        Widget:   Heatmap (mapa de calor)   —   título: "Mapa de calor: CPU de todos los pods del clúster"
                  avg:kubernetes.cpu.usage.total{kube_cluster_name:ivanosuna-cluster} by {pod_name}

    ¿Qué nodo está más cargado?
        Widget:   Host Map (hexágonos)   —   título: "Nodos del clúster por CPU (hexágonos)"
                  color:  avg:system.cpu.user{kube_cluster_name:ivanosuna-cluster} by {host}

---

## Los widgets usados (y para qué tipo de dato)

    Note                 texto: para quién es, qué contesta, quién lo mantiene
    Group                agrupa widgets bajo una pregunta (plegable)
    Query Value          UN número (+ semáforo con formato condicional)
    Timeseries           evolución en el tiempo: líneas, barras, área (apilada)
                         + overlay de EVENTOS (rayas) + MARCADORES (umbrales)
    Sunburst             tarta: reparto de una variable CUALITATIVA (tabla de frecuencias)
    Treemap              reparto de una CUANTITATIVA por categorías (área proporcional)
    Query Table          tabla: varias medidas por cada valor de una etiqueta
    Distribution         histograma de una CUANTITATIVA (con percentiles)
    Scatter Plot         CUANTITATIVA x CUANTITATIVA (cada punto = un grupo)
    Heatmap              distribución a lo largo del TIEMPO (muchas series a la vez)
    Geomap               valor por PAÍS
    Host Map             hexágonos: una medida por máquina
    SLO                  estado de un SLO y su presupuesto de error
    Monitor Summary      lista y recuento de monitores por estado
    Service Summary      el resumen de APM de un servicio
    Service Map          mapa de dependencias entre servicios
    Log Stream           lista de logs en directo
    Event Timeline       eventos en el tiempo

---

## Notificaciones por email: monitores y alertas de SLO

### Cómo avisa Datadog

Datadog NO avisa porque sí. Avisa un MONITOR:
    una consulta que Datadog evalúa cada minuto
    + unos umbrales (aviso / alerta)
    + un MENSAJE
    + a QUIÉN se lo manda

El "a quién" va DENTRO DEL MENSAJE, con una arroba:

    @ivan.osuna@kiwitec.es          -> un email (cualquier dirección)
    @slack-canal-alertas             -> un canal de Slack       (si está la integración)
    @teams-canal                     -> Microsoft Teams         (si está la integración)
    @pagerduty-servicio              -> PagerDuty (guardias)
    @servicenow-instancia-incident   -> una incidencia en ServiceNow
    @webhook-nombre                  -> cualquier sistema con un webhook

El email NO necesita integración: viene de serie. El resto se configura en Integrations.

### Un SLO NO manda correos. Se le cuelga una ALERTA DE SLO

    SLO           = la promesa ("el 95 % de las peticiones van bien en 7 días")
    ALERTA DE SLO = un monitor que vigila esa promesa. Dos tipos:

    PRESUPUESTO CONSUMIDO (error budget)      "ya hemos gastado más del 75 % del margen de fallos"
                                              -> sencillo de entender para un PO
    VELOCIDAD DE CONSUMO (burn rate)          "estamos gastando el margen 10 veces más rápido de lo normal;
                                               a este ritmo se acaba en 3 días"
                                              -> avisa ANTES; ignora un pico de 2 minutos

    Se crean desde la página del SLO: botón "New Alert" / "Set up alerts".

### Lo que vamos a crear en clase (cuenta de Ivan)

    1. Ivan - Tienda - Presupuesto de error del SLO casi agotado      (alerta de SLO)
         sobre:   Ivan - Tienda - Peticiones Procesadas Correctamente
         aviso:   > 50 % del presupuesto consumido
         alerta:  > 75 %
         email:   @ivan.osuna@kiwitec.es

    2. Ivan - Tienda - Tasa de pagos fallidos alta                     (monitor de métrica)
         consulta:  pedidos con status:payment_failed / todos los pedidos * 100   (últimos 10 min)
         aviso:     > 20 %
         alerta:    > 25 %       (la tienda falla ~30 % a propósito: SALTA)
         email:     @ivan.osuna@kiwitec.es

### El mensaje: lo que lee alguien que NO escribió el monitor

    {{#is_alert}}🔴 El {{value}} % de los pedidos ha fallado en el pago (umbral {{threshold}} %).{{/is_alert}}
    {{#is_warning}}🟡 Pagos fallidos al {{value}} %.{{/is_warning}}
    {{#is_recovery}}🟢 Los pagos vuelven a la normalidad ({{value}} %).{{/is_recovery}}

    **Impacto**: clientes que quieren comprar y no pueden pagar = ventas perdidas.
    **Dónde mirar**: APM -> shop-api -> POST /api/orders -> el span payment.charge.

    @ivan.osuna@kiwitec.es

    {{value}}, {{threshold}}          los valores reales en el momento del aviso
    {{#is_alert}} ... {{/is_alert}}   texto que solo sale en ALERTA (también is_warning, is_recovery, is_no_data)
    Se puede mandar a sitios distintos según la gravedad:
        {{#is_alert}}@pagerduty-guardia{{/is_alert}}   {{#is_warning}}@email-del-equipo{{/is_warning}}

Un buen mensaje contesta 3 cosas: QUÉ pasa, QUÉ IMPACTO tiene, y DÓNDE mirar primero.
Se avisa también de la RECUPERACIÓN: el que recibió la alerta quiere saber que ha terminado.

### Probar sin esperar a que pase

    En el monitor: botón "Test Notifications" -> elijo el estado (Alert, Warning, Recovery)
    -> llega el correo de prueba. Así se revisa el mensaje ANTES de que haga falta.

---

## Ejercicio: vuestro primer monitor con email

Cada uno, con su buzón del curso (https://mail.ionos.es):

    1. Monitors -> New Monitor -> Metric
    2. Consulta: a = pedidos con status:payment_failed, b = todos los pedidos, fórmula a / b * 100
       (en el editor: sum by, y as_count)
    3. Umbrales: alerta > 25, aviso > 20.  Evaluar: suma de los últimos 10 minutos.
    4. Mensaje: qué pasa, impacto, dónde mirar... y vuestro email:  @<nombre>.datadog@ivanosuna.com
    5. Nombre: <nombre> - Tienda - Tasa de pagos fallidos
    6. Guardar -> "Test Notifications" -> mirar el correo en Ionos.

    Y una alerta sobre VUESTRO SLO (después de corregirlo: buenos = hits - errors, malos = errors):
    página del SLO -> New Alert -> presupuesto consumido > 50 %.

> Preguntas:
    - ¿Esta alerta es sobre un SÍNTOMA o sobre una CAUSA?
    - ¿Despertaríais a alguien a las 3 am con esto? ¿O solo un email?
    - ¿Qué monitor habría avisado del stock agotado del martes?
         -> pedidos pagados a 0 durante 15 minutos.

---
---

# MONITORES EN DETALLE

## 1. Qué es un monitor

Un dashboard es PASIVO: alguien tiene que estar mirándolo.
Un monitor es ACTIVO: Datadog mira por mí y me AVISA.

    MONITOR = una CONSULTA
            + cada cuánto y sobre qué VENTANA de tiempo la evalúo
            + unos UMBRALES (aviso, alerta, recuperación)
            + qué hago si NO HAY DATOS
            + un MENSAJE
            + a QUIÉN se lo mando (@email, @slack-..., @servicenow-..., @webhook-...)

Datadog lo evalúa cada minuto (más o menos) y, según el resultado, el monitor tiene un ESTADO.

## 2. Estados (y cuándo avisa)

    OK          todo dentro de lo normal
    WARN        se ha cruzado el umbral de AVISO
    ALERT       se ha cruzado el umbral de ALERTA
    NO DATA     la consulta no devuelve datos (¿no hay tráfico? ¿se ha caído el que envía?)

    Avisa en los CAMBIOS de estado:   OK -> WARN -> ALERT -> OK (recuperación)
    No avisa cada minuto mientras sigue igual (salvo que configure "renotify").

    OK ──> WARN ──> ALERT
     ^                 │
     └──── RECOVERY ───┘     <- también se avisa: "ya ha pasado"

## 3. Tipos de monitor (los que importan)

    Tipo                 Qué vigila                                     Ejemplo en la tienda
    ------------------   --------------------------------------------   ------------------------------------------------
    Metric               una métrica (o una fórmula de varias)          % de pagos fallidos > 25 %
                                                                        pedidos pagados == 0 en 15 min (¡el stock!)
    APM                  latencia, errores, tráfico de un servicio      p95 de POST /api/orders > 1 s
                         o de un endpoint
    Logs                 cuántos logs cumplen una búsqueda              más de 20 "Payment rejected" en 5 min
    RUM                  lo que pasa en el navegador                    errores de JavaScript en el checkout
    Synthetics           un test robot que falla                        "no se puede comprar desde París y Dublín"
    SLO alert            el presupuesto de error de un SLO              consumido > 75 % / se gasta 10x más rápido
    Anomaly              "esto no es lo normal PARA ESTA HORA"          pedidos muy por debajo de un martes normal
    Forecast             "esto VA a pasar"                              el disco se llenará en 3 días
    Outlier              "uno se comporta distinto que los demás"       un pod de shop-api mucho más lento que el otro
    Composite            combina monitores con Y / O                    latencia alta Y hay tráfico real
    Event                cuántos eventos de un tipo                     más de 3 reinicios de pod en 10 min
    Host / Process       una máquina o un proceso dejan de reportar     el agente de un nodo no responde

## 4. La consulta y la VENTANA DE EVALUACIÓN

Un monitor de métrica dice:  "<cómo agrego> de los últimos <N minutos> de <consulta>  <comparador>  <umbral>"

    avg(last_5m):  p95:trace.servlet.request{service:shop-api}  > 1

La agregación sobre la ventana ES UNA DECISIÓN, y cambia mucho el comportamiento:

    avg  de los últimos 5 min > 1 s     "de media ha ido lento"            tolera un pico suelto
    max  de los últimos 5 min > 1 s     "en algún momento ha ido lento"    salta con cualquier pico (RUIDOSO)
    min  de los últimos 10 min > 1 s    "TODO el rato ha ido lento"        el más robusto: no salta por un pico
    sum  de los últimos 15 min > 100    para RECUENTOS: "más de 100 errores en 15 min"

    > Pregunta: ¿qué elijo para despertar a alguien a las 3 am?
      min: "lleva 10 minutos mal SIN PARAR". Si es un pico de 30 segundos, que no me despierte.

Ventana más larga = menos ruido, pero avisa más tarde. Es un equilibrio.

## 5. Umbrales: aviso, alerta y RECUPERACIÓN

    alerta       > 25 %      -> despierta / ticket
    aviso        > 20 %      -> un email, alguien lo mira cuando pueda
    recuperación < 15 %      -> solo vuelve a OK cuando baja de aquí

¿Para qué la recuperación? Para evitar el "FLAPPING" (aleteo):
    sin ella, un valor que baila entre 24,9 y 25,1 genera ALERTA, OK, ALERTA, OK... 40 correos en una hora.
    Con recuperación a 15 %: salta en 25, y no se da por arreglado hasta que baja de verdad.

## 6. MULTI-ALERTA: un monitor, muchos "grupos"

    sum:shop.orders.created{status:payment_failed}  ...                 -> UN estado para toda la tienda
    sum:shop.orders.created{status:payment_failed} by {category}  ...   -> UN estado POR CATEGORÍA

    Con "by", cada valor de la etiqueta es un grupo con su propio estado:
        category:audio        OK
        category:peripherals  ALERT   <- el mensaje dice CUÁL: {{category.name}}
        category:displays     OK

Regla: agrupar por lo que alguien puede ARREGLAR por separado (servicio, endpoint, región).
NO por host en Kubernetes: un pod que desaparece no es una incidencia.

## 7. NO DATA: ¿y si no llegan datos?

    "Notificar si no hay datos durante 10 minutos"

    ¿Silencio = está todo bien? ¿O silencio = se ha caído el que manda los datos?
    - Un servicio que SIEMPRE tiene tráfico:  sin datos = algo va muy mal  -> AVISAR
    - Un proceso nocturno (la ETL de las 3 am): sin datos durante el día = normal  -> NO avisar

    OJO con la tienda: cuando se agotó el stock, NO HABÍA pedidos "paid"...
    la serie status:paid DEJÓ DE EXISTIR. Un monitor "paid < 1" con no-data apagado... NO SALTA.
    El monitor del stock necesita:  avisar también si NO HAY DATOS.

## 8. Opciones que evitan sustos

    Require full window   no evaluar hasta tener la ventana entera de datos
                          (un pod recién arrancado no salta con su primer punto)
    Evaluation delay      esperar N segundos antes de evaluar: para datos que llegan tarde
                          (métricas de AWS/Azure llegan con 5-10 min de retraso)
    Renotify              repetir el aviso cada X min si sigue en alerta (con cuidado: es ruido)
    Priority (P1 a P5)    la gravedad. Sirve para filtrar y para enrutar (P1 -> guardia; P4 -> email)
    Tags                  team:, service:, owner: -> para encontrarlos y saber DE QUIÉN es

## 9. EL MENSAJE (lo más importante para quien lo recibe)

Quien recibe el aviso a las 3 am NO escribió el monitor, no se acuerda de él y tiene 20 segundos.
Un buen mensaje contesta:  QUÉ pasa  ·  QUÉ IMPACTO tiene  ·  DÓNDE mirar primero

    {{#is_alert}}🔴 El {{value}} % de los pedidos de los últimos 10 min ha fallado en el pago (umbral {{threshold}} %).{{/is_alert}}
    {{#is_warning}}🟡 Pagos fallidos al {{value}} %: vamos justos.{{/is_warning}}
    {{#is_recovery}}🟢 Pagos de nuevo normales ({{value}} %).{{/is_recovery}}

    **Impacto**: clientes que quieren comprar y no pueden pagar = ventas perdidas.
    **Dónde mirar**: APM -> shop-api -> POST /api/orders -> span payment.charge · logs "Payment rejected"
    **Runbook**: <enlace al notebook / wiki>

    {{#is_alert}}@ivan.osuna@kiwitec.es{{/is_alert}}

Variables:
    {{value}}                 el valor que ha hecho saltar el monitor
    {{threshold}}             el umbral
    {{<etiqueta>.name}}       en multi-alerta, el grupo: {{category.name}}, {{service.name}}
    {{last_triggered_at}}     cuándo saltó

Bloques condicionales (texto o destinatarios distintos según el estado):
    {{#is_alert}} ... {{/is_alert}}        {{#is_warning}} ... {{/is_warning}}
    {{#is_recovery}} ... {{/is_recovery}}  {{#is_no_data}} ... {{/is_no_data}}

    -> Escalado sencillo:  {{#is_warning}}@email-del-equipo{{/is_warning}}  {{#is_alert}}@pagerduty-guardia{{/is_alert}}

Destinatarios (con @):
    @persona@empresa.com             email: VIENE DE SERIE, sin integración
    @slack-canal / @teams-canal      chat (integración)
    @pagerduty-servicio / @opsgenie  guardias con escalado (integración)
    @servicenow-<instancia>-incident una incidencia (integración) -> mañana
    @webhook-<nombre>                cualquier otro sistema

## 10. Probar y silenciar

    TEST NOTIFICATIONS   botón en el monitor: elijo el estado (alerta, aviso, recuperación)
                         y llega un correo de prueba. Se revisa el mensaje ANTES de necesitarlo.

    DOWNTIMES            silenciar monitores en una ventana: mantenimiento, despliegue, la ETL nocturna.
                         Monitors -> Downtimes. Pueden ser recurrentes (todas las noches de 2:00 a 2:30).
                         El monitor sigue evaluando (queda el histórico), pero NO AVISA.

## 11. Monitores "inteligentes"

    ANOMALY    Datadog aprende lo normal POR HORA Y POR DÍA DE LA SEMANA y avisa si se sale de la banda.
               Para lo que tiene estacionalidad: las ventas de un lunes a las 10 no son las de un domingo a las 4.
               Un umbral fijo "pedidos < 10/min" saltaría todas las noches. Necesita historia (2 semanas o más).
    FORECAST   avisa ANTES: "a este ritmo, el disco se llena en 3 días".
    OUTLIER    "todos los pods tardan 100 ms y uno tarda 900": avisa del raro.
    COMPOSITE  A Y B:  "latencia alta" Y "hay tráfico real"
               -> a las 4 am, con 2 peticiones, una lenta no debería despertar a nadie.
    WATCHDOG   Datadog busca anomalías SOLO, sin configurar nada. Útil, no sustituye a los monitores.

## 12. Reducir el RUIDO (lo que de verdad importa)

Una alerta que se ignora es PEOR que no tener alerta: entrena al equipo a ignorar alertas.
El día que llega la buena, está en la misma carpeta que las 400 falsas.

    1. ALERTAR SOBRE SÍNTOMAS, NO SOBRE CAUSAS
         Síntoma: pagos fallidos, latencia del checkout, pedidos a 0     -> esto sí
         Causa:   CPU al 90 %                                           -> a un dashboard
    2. CADA ALERTA CON UNA ACCIÓN: si la respuesta es "vale, me entero"... no es una alerta.
    3. Aviso (email) para "mirarlo mañana"; alerta (guardia) para "mirarlo ya".
    4. Umbral de RECUPERACIÓN contra el aleteo.
    5. Ventanas y agregación pensadas (min de 10 min > max de 1 min).
    6. DOWNTIMES para lo previsible.
    7. ANOMALY para lo que tiene ciclos, en vez de umbrales fijos.
    8. Revisarlos cada mes: Monitors -> Manage -> ordenar por "veces que ha saltado".
       El que saltó 200 veces: se arregla, se ajusta o se borra.

## 13. Monitores de NEGOCIO (los del PO)

    Tienda sin ventas           pedidos pagados == 0 durante 15 min   (+ AVISAR SI NO HAY DATOS)
                                -> el que habría pillado el stock agotado del martes
    Pagos fallando              % de pagos fallidos > 25 % en 10 min
    Checkout lento              p95 de POST /api/orders > 1 s
    Promesa en riesgo           alerta del SLO: presupuesto consumido > 75 %
    Robot no puede comprar      test sintético de compra falla en 2 de 3 ciudades
    Usuarios frustrados         errores de JavaScript en la página de pago > N

> Para el PO: los monitores de negocio también son REQUISITOS.
  "Quiero enterarme si la tienda deja de vender" -> alguien tiene que crear ESE monitor,
  y para eso tiene que existir la métrica de pedidos pagados (¡con su status!).

---

## Lo que vamos a hacer en clase (paso a paso)

### A) Monitor de pagos fallidos (Metric)

    Monitors -> New Monitor -> Metric
    a = sum:shop.orders.created{env:course,status:payment_failed}.as_count()
    b = sum:shop.orders.created{env:course}.as_count()
    fórmula: a / b * 100
    Evaluar: suma de los últimos 10 minutos   (la tienda falla ~30 % a propósito: SALTA)
    Alerta > 25 · Aviso > 20 · Recuperación < 15
    Nombre: Ivan - Tienda - Tasa de pagos fallidos alta
    Mensaje: el del punto 9, con @ivan.osuna@kiwitec.es
    Guardar -> Test Notifications -> ver el correo

### B) Alerta sobre mi SLO (SLO alert)

    SLOs -> "Ivan - Tienda - Peticiones Procesadas Correctamente" -> New Alert
    Tipo: Error budget (presupuesto consumido)
    Aviso > 50 % · Alerta > 75 %
    Mensaje: qué significa (incumplimos la promesa), qué decisión toca (¿estabilizar o revisar la promesa?)
             + @ivan.osuna@kiwitec.es

### C) El monitor que habría pillado el stock agotado

    Metric: sum(last_15m): sum:shop.orders.created{env:course,status:paid}.as_count() < 1
    + "Notify if data is missing" 15 min   <- ¡CLAVE! Sin pedidos pagados, la serie desaparece.

---

# EJERCICIOS DE MONITORES

Todos: Monitors -> New Monitor -> <tipo>.  Nombre: <tu nombre> - Tienda - <qué vigila>
Etiquetas en todos: owner:<tu nombre>, team:training, service:shop-api   (así salen en el dashboard)
Destinatario en todos: @<tu nombre>.datadog@ivanosuna.com   (lo veis en https://mail.ionos.es)
Al terminar cada uno: botón "Test Notifications" y mirar el correo.

## Ejercicio 1. Pagos fallidos altos            [Metric con fórmula]   -> SALTA AHORA

    Consulta:   a = sum:shop.orders.created{env:course,status:payment_failed}.as_count()
                b = sum:shop.orders.created{env:course}.as_count()
                fórmula:  a / b * 100
    Evaluar:    la SUMA de los últimos 10 minutos
    Umbrales:   alerta > 25  ·  aviso > 20  ·  recuperación < 15
    Mensaje:    qué pasa ({{value}} % de pagos fallidos), impacto (ventas perdidas),
                dónde mirar (APM -> POST /api/orders -> span payment.charge)
    Esperado:   ALERTA (la tienda falla ~30 % a propósito) -> llega el email

> ¿Por qué un %, y no "más de 50 pagos fallidos"?  (con poco tráfico nunca saltaría; con mucho, siempre)

## Ejercicio 2. La tienda no vende             [Metric + SIN DATOS]   -> no salta (y es lo que queremos)

    Consulta:   sum:shop.orders.created{env:course,status:paid}.as_count()
    Evaluar:    la SUMA de los últimos 15 minutos
    Umbral:     alerta < 1
    OPCIÓN CLAVE: "Notify if data is missing for more than 15 minutes"
    Esperado:   OK

> Es el monitor que habría avisado del stock agotado del martes.
> ¿Por qué hace falta "avisar si no hay datos"? Cuando no hay pedidos pagados, la serie status:paid
  DEJA DE EXISTIR: no vale 0, no hay nada. Sin esa opción, el monitor se queda callado.

## Ejercicio 3. Promesa en riesgo               [SLO alert: presupuesto]   -> SALTA EN AVISO

    Desde: SLOs -> tu SLO (corregido: buenos = hits - errors, malos = errors) -> New Alert
    Tipo:       Error Budget  (presupuesto CONSUMIDO)
    Ventana:    7 días
    Umbrales:   alerta > 75 %  ·  aviso > 50 %
    Mensaje:    qué significa (incumplimos la promesa) y la decisión de PO
                (¿estabilizamos o revisamos la promesa?)
    Esperado:   AVISO (se lleva consumido algo más de la mitad)

## Ejercicio 4. Endpoint lento, uno por uno       [APM, MULTI-ALERTA]   -> SALTA SOLO PARA /api/slow

    Tipo:       APM -> servicio shop-api, env course, operación servlet.request
    Medida:     latencia p95, agrupada POR RECURSO (resource_name)
    Evaluar:    media de los últimos 5 minutos
    Umbral:     alerta > 1 s
    Mensaje:    usar {{resource_name.name}} para decir CUÁL:
                "El endpoint {{resource_name.name}} tarda {{value}} s (p95)"
    Esperado:   un "grupo" en ALERTA (GET /api/slow, ~2,8 s) y el resto OK

> Multi-alerta: UN monitor, un estado POR ENDPOINT. ¿Os parece útil una alerta permanente en /api/slow?
  (Es lento a propósito: o se excluye, o se le pone otro umbral. Esa decisión también es del PO.)

## Ejercicio 5. Pagos rechazados en los logs     [Logs]   -> SALTA AHORA

    Consulta:   service:shop-api "Payment rejected"
    Medida:     número de logs
    Evaluar:    los últimos 5 minutos
    Umbrales:   alerta > 20  ·  aviso > 10
    Opción:     "Include a sample of logs" -> el correo trae ejemplos de las líneas
    Esperado:   ALERTA

> Es el MISMO problema que el ejercicio 1, visto desde otra señal (logs en vez de métricas).
  ¿Cuál preferís para avisar? ¿Por qué? (pista: el log depende de que nadie cambie el texto del mensaje)

## Ejercicio 6 (extra). Clientes viendo errores  [RUM]   -> SALTA SI LO PROVOCÁIS

    Consulta:   @type:error @session.type:user
    Evaluar:    los últimos 15 minutos
    Umbral:     alerta > 3
    Provocarlo: abrid https://datadog.ivanosuna.com y pulsad 4 veces "Throw a JS error"
    Esperado:   ALERTA unos minutos después

## Ejercicio 7 (extra). Compra lenta Y hay tráfico   [Composite]

    A = un monitor "p95 de POST /api/orders > 1 s"
    B = un monitor "más de 10 peticiones a shop-api en 5 minutos"
    Composite:  A && B
> ¿Para qué? A las 4 am, con 2 peticiones, una compra lenta no debería despertar a nadie.

## Al final: los monitores en el dashboard

    - Abrid el dashboard "Ivan - Tienda (referencia)" -> pestaña Guardia -> "Alertas de shop-api":
      vuestros monitores aparecen ahí por la etiqueta service:shop-api.
    - Añadid a vuestro dashboard un widget ALERT GRAPH con vuestro monitor de pagos fallidos:
      la gráfica con la línea del umbral pintada encima.

> Preguntas de cierre:
    - ¿Cuáles de vuestros monitores son sobre SÍNTOMAS y cuáles sobre CAUSAS?
    - ¿Cuál despertaría a alguien a las 3 am? ¿Cuál solo merece un email?
    - ¿Qué monitor de negocio le pediríais a vuestro equipo para vuestro producto de verdad?

---

# DASHBOARDS, NOTEBOOKS Y SHEETS

Tres formas de trabajar con los MISMOS datos, para preguntas distintas:

    Dashboard                         Notebook                               Sheets
    -------------------------------   ------------------------------------   -------------------------------------
    PARA QUÉ: vigilar la MISMA        CONTAR UNA HISTORIA con datos:         ANALIZAR como en Excel:
    pregunta cada día                 investigación, post-mortem, informe    cruzar, calcular, pivotar
    FORMA: rejilla de widgets         documento: texto, gráfica, texto...    hoja de cálculo: filas, columnas, fórmulas
    TIEMPO: "ahora"                   un momento concreto, FIJADO            lo que consulte
    QUIÉN: quien vigila               quien investiga o explica              quien analiza (un PO, negocio)

## NOTEBOOKS

Un documento dentro de Datadog que mezcla TEXTO con GRÁFICAS VIVAS de cualquier fuente
(métricas, logs, trazas, RUM, eventos).

    - Investigar mientras pasa algo: el equipo pega gráficas, trazas y logs y escribe lo que va viendo.
      Todos en el MISMO documento (en vez de 40 capturas en un chat).
    - POST-MORTEM: qué pasó, impacto, cronología, causa y acciones.
      Se FIJA EL RANGO DE TIEMPO: dentro de 3 meses sigue enseñando ESE día (no "la última hora").
      Plantilla: New Notebook -> Templates -> Postmortem.
    - Documentación viva: runbooks con las gráficas dentro; informes semanales programados por email (PDF).
    - Cualquier gráfica de un dashboard o de un explorador -> menú -> "Export to notebook".

Ejercicio natural: el POST-MORTEM DEL STOCK AGOTADO DEL MARTES
    1. Resumen        la tienda estuvo X horas sin vender
    2. Impacto        gráfica de shop.orders.created por status -> "paid" a cero
    3. Cronología     cuándo empezó, cuándo se detectó, cuándo se arregló
                      gráfica de shop.stock.units llegando a 0 y saltando a 1.000.000
    4. Evidencias     lista de logs "Out of stock"; una traza con el 409 y SIN el span de pago
    5. Causa          nadie repone stock y no había ninguna alerta
    6. Acciones       el monitor "la tienda no vende" (con AVISAR SI NO HAY DATOS), con dueño y fecha

## SHEETS

Lo más nuevo de Datadog: HOJAS DE CÁLCULO SOBRE LOS DATOS DE DATADOG, sin exportar a Excel.

    - Se trae a una tabla el resultado de una consulta: logs, trazas, RUM, métricas...
      o una REFERENCE TABLE (una tabla propia subida a Datadog: un CSV de productos, de clientes...).
    - Encima se trabaja como en una hoja de cálculo:
        columnas calculadas con FÓRMULAS · filtros · TABLAS DINÁMICAS (pivotar) ·
        CRUZAR dos tablas por una columna (como un BUSCARV)
    - El resultado se puede llevar a un dashboard o a un notebook.

Es la herramienta más cercana a un PO: es SU lenguaje. Ejemplos con la tienda:
    - Logs de "Payment rejected" de la semana, PIVOTADOS por cliente y día:
      ¿hay clientes a los que les falla siempre?
    - Pedidos de los logs CRUZADOS con una tabla de precios / márgenes (Reference Table):
      margen perdido por pagos fallidos.
    - Sesiones RUM por país y navegador, con una columna calculada "% con error".

OJO: Sheets es reciente. En la org del curso (trial) puede salir como "preview" o no estar activado.
Buscarlo con Cmd+K -> "Sheets". Si no aparece, se cuenta como concepto.

---

# TEXTO EN LOS DASHBOARDS: NOTAS Y SIMILARES

No muestran datos... pero hacen que un dashboard SE ENTIENDA sin que nadie lo explique.

## Widgets de texto

    Note (nota)      recuadro de texto en MARKDOWN: negrita, listas, enlaces, tablas, emojis
    Free Text        un TÍTULO grande, sin recuadro (para separar zonas)
    Image            una imagen (un logo, un diagrama de la arquitectura)
    Iframe           una página web incrustada (la wiki, un runbook)
    Group            el título de un grupo también es texto: escribidlo como PREGUNTA

## Opciones de una nota

    Color de fondo        blanco, gris, azul, morado, rosa, naranja, amarillo, verde, rojo, transparente
    Tamaño de letra       pequeño ... grande
    Alineación            izquierda / centro / derecha · arriba / centro / abajo
    FLECHA (tick)         el recuadro "señala" al widget de al lado (izquierda, derecha, arriba, abajo)
                          -> como un bocadillo de cómic: "ojo con ESTE widget"
    Markdown              **negrita**, listas, [enlaces](url), tablas | a | b |, `código`

## Para qué usarlas (convención de colores)

    Gris      PORTADA: para quién es, quién lo mantiene, qué contesta            (arriba de cada pestaña)
    Amarillo  AVISO sobre un dato: "este importe incluye no pagados"            (con flecha al widget)
    Rojo      QUÉ HACER si algo está en rojo: los primeros pasos (runbook corto)
    Azul      ENLACES: al SLO, a los tests, a la página del servicio
    Morado    ENLACES para investigar más: logs, APM, eventos
    Verde     GLOSARIO: qué significa cada término (una tablita en markdown)
    Naranja   CÓMO LEER una gráfica: "la línea discontinua es el límite"         (con flecha)

## En el dashboard de ejemplo (Ivan - Tienda)

    Pestaña NEGOCIO
      - Portada (gris)                         arriba de todo
      - "Cómo leerla" (amarillo, con flecha)   junto a la distribución del importe
      - "VENTAS" (Free Text)                   título grande del primer grupo
      - "Ojo con este importe" (amarillo)      con flecha hacia el importe total
      - "Enlaces útiles" (azul)                en "¿Cumplimos la promesa?"
      - "Glosario" (verde, con tabla)          en "¿Quién nos visita?"
    Pestaña GUARDIA
      - Portada (gris)
      - "Si algo está en ROJO" (rojo)          al principio de "¿Está roto?"
      - "Para investigar más" (morado)         en "¿Por qué?"
      - "La línea discontinua es el límite"    (naranja, con flecha) en "¿Tenemos recursos?"

Regla: si para entender un dashboard hace falta que alguien te lo explique... le faltan notas.

---

# LOS MONITORES QUE YA HAY PARA LA TIENDA (y salen en el dashboard)

Los creó Ivan (por API) como REFERENCIA. No mandan correos a nadie (en el mensaje llevan @nobody).
Salen en el dashboard "Ivan - Tienda (referencia)" en dos widgets de tipo MONITOR SUMMARY:

    "Alertas de shop-api"          pestaña GUARDIA    búsqueda:  service:shop-api    -> 6 monitores
    "Robots (tests sintéticos)"    pestaña NEGOCIO    búsqueda:  type:synthetics     -> 2 monitores

Los que creéis vosotros saldrán también en "Alertas de shop-api" SI LES PONÉIS la etiqueta  service:shop-api

## Pestaña GUARDIA: "Alertas de shop-api"

### 1. Tasa de errores de la tienda
    Nombre en Datadog:  ivan - [shop] shop-api error rate
    Tipo:               Métrica, con FÓRMULA
    Qué vigila:         el % de peticiones de shop-api que acaban en error (5xx), en los últimos 10 minutos
    Estado habitual:    🟢 OK (ronda el 4 %)
    Para qué en clase:  un SÍNTOMA bien elegido; los errores como TASA, no como número

    Cómo se configura:  Monitors -> New Monitor -> Metric
      1. Consulta a:    métrica trace.servlet.request.errors · from env:course, service:shop-api · sum · Σ as_count
         Consulta b:    métrica trace.servlet.request.hits   · from env:course, service:shop-api · sum · Σ as_count
         Fórmula:       a / b * 100
      2. Evaluar:       la SUMA ("sum") de los últimos 10 minutos
      3. Umbrales:      alerta > 20 · aviso > 10 · recuperación (alert recovery) < 8
      4. Sin datos:     no avisar
    Texto equivalente:  sum(last_10m):sum:trace.servlet.request.errors{env:course,service:shop-api}.as_count()
                        / sum:trace.servlet.request.hits{env:course,service:shop-api}.as_count() * 100 > 20

### 2. Pico de pagos rechazados
    Nombre en Datadog:  ivan - [shop] payment rejections spike
    Tipo:               Logs
    Qué vigila:         cuántas líneas de log de ERROR con el texto "Payment rejected" aparecen en 5 minutos
    Estado habitual:    🔴 Alerta (el proveedor de pagos falla ~30 % a propósito)
    Para qué en clase:  el MISMO problema que los pagos fallidos, visto desde los LOGS en vez de las métricas

    Cómo se configura:  Monitors -> New Monitor -> Logs
      1. Búsqueda:      service:shop-api status:error "Payment rejected"
      2. Medida:        recuento de logs (count)
      3. Evaluar:       los últimos 5 minutos
      4. Umbrales:      alerta > 20 · aviso > 10
      5. Opcional:      "Include a sample of logs" -> el correo trae ejemplos de las líneas
    Texto equivalente:  logs("service:shop-api status:error \"Payment rejected\"").index("*").rollup("count").last("5m") > 20

### 3. Volumen de pedidos fuera de lo normal
    Nombre en Datadog:  ivan - [shop] orders volume anomaly
    Tipo:               Anomalía (Anomaly)
    Qué vigila:         que los pedidos caigan POR DEBAJO de lo que Datadog considera normal para esa hora
    Estado habitual:    🟢 OK
    Para qué en clase:  un umbral que APRENDE. No digo "menos de 10"; Datadog aprende cuánto se vende
                        normalmente a cada hora. Necesita HISTORIA (aquí solo hay unos días).

    Cómo se configura:  Monitors -> New Monitor -> Anomaly
      1. Consulta:      métrica shop.orders.created · from env:course · sum · Σ as_count
      2. Algoritmo:     agile · desviaciones: 2 · dirección: BELOW (solo me preocupa que baje)
      3. Estacionalidad: hourly (cada hora)
      4. Evaluar:       alerta si en los últimos 15 minutos el 50 % de los puntos está fuera de la banda
                        (recuperación: menos del 25 %)
      5. En el editor se ve la BANDA GRIS de "lo normal" sobre los datos: ahí se entiende enseguida.
    Texto equivalente:  avg(last_15m):anomalies(sum:shop.orders.created{env:course}.as_count(), 'agile', 2,
                        direction='below', seasonality='hourly') >= 0.5

### 4. Compra lenta, endpoint a endpoint
    Nombre en Datadog:  ivan - [shop] shop-api p95 latency high on {{resource_name.name}}
    Tipo:               Métrica, MULTI-ALERTA (un estado por endpoint)
    Qué vigila:         el p95 de la latencia de CADA endpoint de shop-api, media de 5 minutos
    Estado habitual:    🔴 Alerta, pero SOLO en /api/slow (es lento a propósito); el resto OK
    Para qué en clase:  MULTI-ALERTA: un monitor, un estado por grupo. Y la decisión: ¿excluyo /api/slow
                        o lo acepto? (esa decisión también es del PO)

    Cómo se configura:  Monitors -> New Monitor -> Metric  (o APM, que hace lo mismo con menos pasos)
      1. Consulta:      métrica trace.servlet.request · from env:course, service:shop-api
                        · agregación p95 · AGRUPADO por ("by") resource_name   <- esto lo hace multi-alerta
      2. Evaluar:       la MEDIA de los últimos 5 minutos
      3. Umbrales:      alerta > 1 · aviso > 0,7 · recuperación < 0,5   (en segundos)
      4. Opciones:      "require a full window of data" (no evaluar con datos a medias)
                        renotificar cada 30 min si sigue en alerta
      5. Mensaje:       usar {{resource_name.name}} para decir CUÁL endpoint
    Texto equivalente:  avg(last_5m):p95:trace.servlet.request{env:course,service:shop-api} by {resource_name} > 1

### 5. Latencia alta Y hay tráfico
    Nombre en Datadog:  ivan - [shop] latency high AND traffic present
    Tipo:               Compuesto (Composite)
    Qué vigila:         salta SOLO si el monitor 4 (compra lenta) Y el monitor 6 (hay tráfico) están en alerta a la vez
    Estado habitual:    ⚪ Sin datos  -> REVISARLO antes de enseñarlo
    Para qué en clase:  evitar avisos a las 4 am por una única petición lenta

    Cómo se configura:  Monitors -> New Monitor -> Composite
      1. Elegir los monitores:  a = "Compra lenta, endpoint a endpoint"   b = "Hay tráfico"
      2. Condición:     a && b
    Texto equivalente:  126530426 && 126530428    (son los ids de los dos monitores)

### 6. Hay tráfico (pieza auxiliar del compuesto)
    Nombre en Datadog:  ivan - [shop] shop-api has traffic
    Tipo:               Métrica
    Qué vigila:         que haya más de 10 peticiones a shop-api en 5 minutos
    Estado habitual:    🔴 Alerta   <- y es BUENA noticia: hay tráfico
    Para qué en clase:  como EJEMPLO DE MAL DISEÑO en un panel: "alerta" tiene que significar SIEMPRE algo malo.
                        Solo tiene sentido como pieza del compuesto; nunca debería verse en un panel ni avisar a nadie.

    Cómo se configura:  Monitors -> New Monitor -> Metric
      1. Consulta:      métrica trace.servlet.request.hits · from env:course, service:shop-api · sum · Σ as_count
      2. Evaluar:       la SUMA de los últimos 5 minutos
      3. Umbral:        alerta > 10
      4. Sin datos:     avisar si no hay datos durante 10 minutos
    Texto equivalente:  sum(last_5m):sum:trace.servlet.request.hits{env:course,service:shop-api}.as_count() > 10

## Pestaña NEGOCIO: "Robots (tests sintéticos)"

Un test sintético crea SOLO su monitor ("[Synthetics] ..."): no se configura en Monitors, sino en el propio test.

### 7. El catálogo responde
    Nombre en Datadog:  [Synthetics] ivan - shop api products
    Tipo:               Sintético de API (HTTP)
    Qué vigila:         que un robot, desde Fráncfort, Dublín y París, pida el catálogo cada 5 minutos y funcione
    Estado habitual:    🟢 OK
    Para qué en clase:  "¿funciona desde fuera?" sin esperar a que entre un cliente

    Cómo se configura:  Digital Experience -> Synthetic Monitoring -> New Test -> API Test -> HTTP
      1. Petición:      GET https://datadog.ivanosuna.com/api/products
      2. Comprobaciones (assertions):
                        código de estado = 200
                        tiempo de respuesta < 1000 ms
                        cabecera content-type contiene application/json
                        el cuerpo contiene "Keyboard"
      3. Localizaciones: Frankfurt (eu-central-1), Ireland (eu-west-1), Paris (eu-west-3)
      4. Frecuencia:    cada 5 minutos
      5. Alerta:        si falla en al menos 2 de las 3 localizaciones; 1 reintento a los 30 s
                        (una ciudad sola puede tener un mal día; dos no es casualidad)
      6. Mensaje y destinatarios: como cualquier monitor

### 8. Se puede comprar
    Nombre en Datadog:  [Synthetics] ivan - shop checkout journey
    Tipo:               Sintético de NAVEGADOR (browser)
    Qué vigila:         que un robot, con un navegador de verdad, haga el recorrido de compra cada 15 minutos
    Estado habitual:    🟢 OK
    Para qué en clase:  el recorrido COMPLETO de un cliente, probado como lo haría él

    Cómo se configura:  Digital Experience -> Synthetic Monitoring -> New Test -> Browser Test
      1. URL de inicio: https://datadog.ivanosuna.com/
      2. Pasos (se GRABAN con la extensión de Chrome "Datadog test recorder", sin código):
                        escribir "synthetic" en la caja del nombre
                        pulsar "Identify in RUM"
                        pulsar "Buy" en el primer producto
                        comprobar que el bloque "Last orders" contiene "synthetic"
      3. Localizaciones: Frankfurt, Ireland, Paris
      4. Frecuencia:    cada 15 minutos
      5. Alerta:        si falla en al menos 2 de las 3 localizaciones; 1 reintento a los 30 s
      Ojo: cada ejecución del robot es también una SESIÓN de RUM (@session.type:synthetics).

## Lo que enseñan (y lo que NO hay que copiar)

    ✔ Errores como tasa, latencia con percentiles, multi-alerta por endpoint, el mismo problema desde métricas y desde logs.
    ✔ Sintéticos: comprobar desde fuera, antes de que lo note un cliente.
    ✘ "Hay tráfico" en ROJO cuando todo va bien: confunde. Una alerta tiene que significar SIEMPRE algo malo.
    ✘ Un compuesto en "sin datos": un monitor que nadie revisa es ruido.
    ✘ Nombres en inglés y con [shop]: para un equipo de negocio, mejor  "Tienda - Pagos fallidos altos".
