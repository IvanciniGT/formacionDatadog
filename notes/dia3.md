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
