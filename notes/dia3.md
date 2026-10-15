# Día 3

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
