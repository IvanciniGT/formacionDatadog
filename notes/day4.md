# Sobre notebooks

Nos permiten ir generando un documento en el que mezclamos:
- Visualizaciones / Widgets (como los que metemos en los dashboards... misma mecánica)
- Textos en Markdown

Cada vez que entramos en un notebook se reejecutan todos las visualizaciones... dicho de otra forma, se recalculan los datos y se actualizan los widgets correspondientes.

Arriba tengo la ventana de tiempo... solo hay un problema: DATADOG NO GUARDA DATOS INFINITOS.

Si quiero usar los notebooks para un post-mortem o un análisis histórico, debo tener en cuenta que los datos antiguos pueden no estar disponibles debido a la retención limitada de Datadog.

La única opción que hay aquí es desvincular los widgets del rango de tiempo global del notebook, y marcar en cada widget el timeframe específico que quiero analizar. Cuando hago eso, el widget se queda con una copia de los datos correspondientes a ese periodo, permitiéndome revisar información histórica aunque el rango de tiempo global del notebook cambie.
PERO MUCHO CUIDADO CON ESTO... como se me ocurra la idea de tocar la ventana de tiempo especifica más adelante y la cambie... ya pierdo los datos.
Al tiempo lo que hace datadog es guardarte una imagen del widget.

Usos:
- Documentar procesos, formas de trabajo
- Analisis en tiempo real compartido
  Hay un incidente/evento (operaciones-caída de servicio-, negocio - campaña publicitaria-, otros - cualquier otro tipo de evento-) y queremos hacer un seguimiento en tiempo real de lo que está pasando... además colaborativo... Podemos tener a varias personas mirando / editando ese documento/notebook simultáneamente.
- Análisis histórico de datos/postmortem... evento que ha ocurrido y quiero dejarlo guardadito: aunque Datadog no guarda datos infinitos, podemos capturar snapshots de widgets para revisar información pasada sin depender del rango de tiempo global del notebook.

---

# Downtimes

Ventanas de indisponibilidad planificadas:
- Despliegue
- Mantenimiento programado
- Actualización de infraestructura
- Backup frio

Las ventanas de indisponibilidad lo que hacen es silenciar MONITORES (que dejen de mandar notificaciones) durante el periodo especificado. Esto es útil para evitar alertas innecesarias mientras se realizan tareas planificadas que podrían generar falsos positivos en los monitores.

SOLO SIRVEN PARA ESO!

Esto no tiene nada que ver con los SLOs, ni con los bugdets (budgets).

---

Datadog quiere convertirse en la plataforma centralizada de TODO EL MUNDO!
Y no para de crecer/ampliar funcionalidad en horzontal.

Ellos hacen eso... otra cosa es que quiera yo entrar en su juego o no.
Hay mínimos. Quiero decir, hay muchas funcionalidades muy integradas.. y es la gracia de la plataforma.
Si no voy a usar todas esas integraciones, mejor me voy a otras herramientas. No porque datadog sea malo... sino porque es caro.

El problema es que viene un poco ariba! Y a veces crece (opinión subjetiva) demasiado... y se mete en charcos.


---

# Digital experience

Dentro de este paratado enconyramos muchas funcionalidades... muchas interesantes... otras... que cada uno decida.

- RUM (Real User Monitoring) = GUAY!
- Session replay = GUAY! 

## Monitorización sintética

La monitorización sintética permite definir programas (robots) que actúan en frontal o en backend, simulando operaciones realizadas por usuarios/sistemas reales. Lo que definimos es un script de tareas que se van realizando.
A su vez, pòdemos convertir esas tareas en TEST (pruebas) autoamtizadas.
Si el script consigue ejecutarse sin problemas, la tarea/prueba se da buena. De lo contrario, se considera fallida.

De alguna forma, esta funcionalidad choca/solapa con las herramienast de autoamtización de pruebas.

REPLAY + ACCIONES AUTOAMTIZADAS en un NAVEGADOR = CYPRESS, SELENIUM
PRUEBAS A ENDPOINT DE UN BACKEND = POSTMAN, SOAPUI, READYAPI, KARATE (pruebas funcionales como de rendimiento)

Lo quepasa es que esas pruebas se suelen hacer en 2 momentos:
- Durante el desarrollo: para detectar defectos lo antes posible.
  - Pruebas unitarias y de integración
- Antes del despliegue: para asegurar que la aplicación funciona correctamente en un entorno similar al de producción.
  - Pruebas end-2end y de aceptación

Cuando se hace un despliegue, solemos hacer otro tipo de prueba SmokeTest.
Lo que se prueba es el despliegue no el sistema... el sistema ya está probado.
- Para estas pruebas tradicionalmente también se han usado esas herramientas (Appium, Selenium, karate)... lo que pasa es que es matar moscas a cañonazos.

Además otra cosa... luego tenemos MONITORIZACION.
Cómo se motoriza un sistema? Este mundo ha evolucionado mucho.. en herramientas y en conceptos.
Antiguamente, lo que se hacía por parte de "OPERACIONES" era mirar:
- Si un puerto estaba abierto
- Si una llamada a un endpoint de salud del sistema daba ok:  https://misystema/health
- Si el uso de recursos del sistema estaba dentro de los límites aceptables (CPU, memoria, disco, etc.)
- Si los logs del sistema no mostraban errores críticos

Pero con el tiempo hemos visto que no es suficiente.
Puede ser que de repente una parte del sistema si funciones y otra parte no.
Puede ser que el software funcione, pero tenga un problema de datos.
Puede ser que desde el punto de vista informático todo esté ok, pero no desde el punto de vista de negocio!
    > El sistema funciona, pero no hay stock y no vendemos!

Y eso hay que hacerlo de continuo.

- Aquí es donde se posiciona inicialmente DATADOG con los test sintéticos =
  PRUEBAS DE HUMO EXTENDIDAS, junto a pruebas continuas de monitorización del sistema y del negocio.

Continuous Integration (CI)  ->  (Durante el desarrollo)
- Tener CONTINUAMENTE la úiltima versión del sistema en el entorno de INTEGRACION, sometida a pruebas AUTOMATIZADAS, para produucir un INFORME DE PRUEBAS EN TIEMPO REAL.

Ahora lo que intentan es:
> YA que has definido muchas pruebas para ejecutar mientras el sistema está en producción, por qué no usarlas para desarrollo también (sobre todo para la parte de pruebas END2END-SISTEMA + aceptación)

PRUEBAS DE SISTEMA : Buscan garantizar que el sistema funciona y se comporta como debe.
Quién dice cómo debe comportarse el sistema? REQUISITOS 
Quién define los requisitos? EL CLIENTE O EL PRODUCT OWNER

Las pruebas de aceptación NO MIRAN SI EL SISTEMA FUNCIONA. El sistema YA SE QUE FUNCIONA CUANDO HAGO ESTAS PRUEBAS.
Miran si el producto es ADECUADO PARA EL CLIENTE.

Sale este concepto de Continuous TESTING: la idea es ejecutar pruebas de manera continua a lo largo de todo el ciclo de vida del desarrollo y de la operación del sistema. Y Que las pruebas de sistema sean las mismas.
Y ya que las estás haciendo en datadog (las de monitorización...) pues usa esas también para desarrollo.