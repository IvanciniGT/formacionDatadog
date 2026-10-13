# Datadog

Es una herramienta de monitorización y observabilidad.

Se ofrece mediante un modelo SaaS (Software as a Service).
El sistema central de datadog no corre en nuestra infraestructura. Opera sobre una infra que provee Datadog.
Lo que si, es que tenemos que montar nosotros AGENTES en nuestra infraestructura para recopilar métricas, logs y trazas.
En cada "servidor, máquina" debemos instalar un agente de Datadog que se encargue de recopilar y enviar las métricas, logs y trazas al sistema central de Datadog.

Lo que busca datadog es una plataforma integrada tanto para operaciones como para desarrollo, permitiendo tener visibilidad completa del estado y rendimiento de los sistemas.

Aunque es una herramienta, dentro engloba muchos productos y funcionalidades que cubren diferentes aspectos de la monitorización y observabilida:
- Infraestructura
- Logs
- APM, 
- BBDD

NOTA: Cada uno de ellos se paga por separado.
NOTA2: En datadog se paga por muchos conceptos... los productos es uno de ellos... pero también se paga por:
    - Gbs de datos procesados o almacenados
    - Número de hosts/instancias monitorizadas
    - Número de contenedores
    - Número de agentes desplegados
Esto tiene una consecuencia... HAY QUE MIRAR MUY BIEN qué llevar a datadog y que no!
Como me despiste, la factura puede ser muy alta.

Además lleva integración con centenares de servicios y plataformas, lo que permite centralizar la monitorización y observabilidad de todo el ecosistema tecnológico de una organización.
En automático sabe leer logs de:
- Kubernetes
- Kafka
- MySQL
- PostgreSQL

Y además, trae cuadros de mando (dashboards) preconfigurados para visualizar métricas, logs y trazas de manera centralizada.

Por esto se paga. El tener monitorización out-of-the-box y dashboards preconfigurados ahorra mucho tiempo y esfuerzo en comparación con montar todo desde cero.

## Monitorización vs observabilidad

Monitorización: Conocimiento a priori. Decido de antenamo qué puede ir mal... qué me interesa medir/alertar.
    Vamos midiendo cosas que consideramos importantes.
    Registramos esas medias.
    Configuramos alertas basadas en esas métricas (trabajar con umbrales)
        RAM > 80%
        Peticiones HTTP (status code !=2XX y 3XX) % > 5%

    Me ayuda a identificar cosas que SE QUE PUEDEN PASAR!
    RESPONDE A: Está ROTO?
    
Observabilidad: Conocimiento a posteriori. 
Capacidad de entender qué está pasando/qué ha pasado dentro de un sistema a partir de los datos que genera (datos que emite)

    Me ayuda a identificar cosas que NO SABÍA QUE PODÍAN PASAR.
    RESPONDE A: Por qué se rompió?

En general decimos que Monitorización es un subconjunto de la Observabilidad.

## Hay varios conceptos que necesitamos aclarar.

- Métrica

    Es un dato numético (casi siempre cuantificable) asociado a una fecha y a unas etiquetas
    Las etiquetas (que es un concepto-tag- que usaremos hasta la saciedad en Datadog) permiten clasificar y filtrar las métricas de manera flexible.

    Ejemplos:
        - Uso de memoria RAM (etiqueta: instancia, host, contenedor, app)
        - Número de usuario. (endpoint/app/backend, role)
        - Status codes de un endpoint/servicio (origen -endpoint/app/backend, traza)
   
    Para qué me sirven?
        - Saber cómo se está comportando el sistema y saber cómo se ha comportado en el pasado -> Evolución
        - Identitificar problemas, anomalías y potenciales puntos de mejora

- Log

    Es un dato de texto (no estructurado), asociado a una fecha y a unas etiquetas
    Nos ayuda a entender QUÉ ESTA HACIENDO CONCRETAMENTE UN SISTEMA (TAREAS)
    Y además, en qué orden ocurren.

    Aquí ya sale algo interesante:
        He identificado una tarea que se está ejecutando en el sistema. Eje: Se hace una query a una BBDD... o... se manda un mensaje a una cola (topic) de Kafka.
        Qué me puede interesar saber adicionalmente?
        - Origen/destino <- Datos cualitativos (etiquetas, asociadas a la entrada del log)
        - Tiempo que tarda aquello en procesarse/ejecutarse <- Datos cuantitativos (métricas asociadas al log? En general no.)
        - Cómo asociamos una métrica a una entrada en un log?

- Traza

Me ayuda a entender el camino que sigue una determinada actividad (petición de un usuario, job que se ejecuta en segundo plano) dentro del sistema.
      - ETLs (pyspark)
      - Petición de un listado en una app que hace un usuario

    Ejemplo:
        - Un usuario accede a una app de vehículos de segunda mano.
          Hace una búsqueda:
            Actividades a nivel del backend de la app de vehículos de segunda mano.
                - Recibes la petición y validas los datos
                - Realizas la consulta a la base de datos: QUERY CONCRETA QUE SE EJECUTÓ
                - Procesas los resultados
                - Devuelves la respuesta al usuario
                Todo eso lo verías en logs. 
            Métricas que te puedan interesar:
                - Tiempo total que tarda en procesarse el trabajo (la búsqueda)
                - Tiempo que tarda cada una de las actividades individuales (validación, consulta a la base de datos, procesamiento de resultados)
        - Una ETL (programa que exporta datos de un sistema a otro) que se ejecuta el día 12-Ocubre a als 03:13 am...
          Actividades a nivel del backend del sistema:
              - Inicia la ejecución del job
              - Extrae los datos del sistema origen
              - Transforma los datos según las reglas de negocio
              - Carga los datos en el sistema destino
              Todo eso lo verías en logs.
          Métricas que te puedan interesar:
              - Tiempo total que tarda en ejecutarse la ETL
              - Tiempo que tarda cada una de las actividades individuales (extracción, transformación, carga)
              - Cantidad de registros procesados en cada etapa (extracción, transformación, carga)
              - Cantidad de datos para los que ha fallado alguna etapa (extracción, transformación, carga)

- Evento: algo que ocurre en un momento dado del tiempo:
  - Reinicio de un sistema
  - Despliegue de una nueva versión del sistema
  - Error crítico en la aplicación

Para ese seguimiento, necesito IDENTIFICAR una actividad (trabajo) de manera única.
Esos identificadores de las actividades se llaman TRACE ID o SPAN ID, dependiendo del nivel de granularidad que queramos seguir.
Y los solemos asociar a los log o las métricas vía etiquetas.

Al igual que los anteriores, los eventos van asociados a una fecha y a unas etiquetas que permiten clasificarlos y filtrarlos.

- Experiencia de usuario (en el contexto de la observabilidad)
  Qué le ocurre al usuario (qué ve,  que nota) en el uso de la app.
  A nivel interno, el sistema tuvo un error al leer la BBDD (Base de Datos)
  Qué percibe el usuario a consecuencia de ese error (por ejemplo, un mensaje de error en la interfaz). Y qué comportamiento tiene el usuario como resultado (por ejemplo, abandona la página, intenta nuevamente, contacta soporte).

Muchas herramientas me ayudan a medir cosas que ocurren en mi backend... en mis servidores.
Medir lo que le ocurre a un usuario es más complejo. Y registrar lo que hace el usaurio es más complejo.

> Ejemplo: Webapp hoy en día

        Frontal                                 Backend
    -----------------------------------     -------------------------------------------------------------------
        Teléfono usuario                        Mis servicios HTTP   ----> BBDD
           Navegador                                                 ----> Kafka
               Parte de la app que es
               la que genera el HTML

                                            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                                                Esta parte corre en mi infraestructura: Mis servidores
                                                De los cuales tengo mucho control. 
                                                Puedo ir generando logs, recopilando métricas.
    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        Esta parte no corre en nuestra 
        infra.. es más compleja de 
        registrar.




datadog.ivanosuna.com



¿Datos CUALITATIVOS?
¿Qué usamos para los datos cualitativos? ETIQUETAS
En nuestro caso, la mayor parte de ellos son NOMINALES!

---

# Dashboards

Aquí echaremos mano de ESTADÍSTICA.
La estadística clasifica los datos en 2 tipos:
    - Cualitativos: Datos que describen cualidades o características (ej. etiquetas)
      - Nominal: Categorías sin un orden específico (ej. etiquetas de host, rol de usuario)
      - Ordinal: Categorías con un orden específico (ej. niveles de severidad de logs) EN GENERAL TRABAJREMOS POCO CON DATOS ORDINALES EN ESTE MUNDO
    - Cuantitativos: Datos que se pueden medir numéricamente (ej. métricas)


Nosotros, vamos a ir recopilando datos cualitativos y cuantitativos para poder analizarlos y visualizarlos en dashboards.

Tengo un dato cualitativo: Status Code de las peticiones que se van haciendo a un determinado endpoint de un servicio.

¿Qué puedo hacer desde el punto de vista de la estadística con datos de tipo cualitativo? CLASIFICAR -> TABLA DE FRECUENCIAS!

¿Para qué sirve la estadística? RESUMIR LOS DATOS PARA ENTENDERLOS MEJOR.
20000 nóminas 

TENER LOS DATOS != ENTENDER LOS DATOS

Para entender los datos, necesito RESUMIR LOS DATOS.La estadística nos da formas de resumir los datos cualitativos y cuantitativos para poder entenderlos mejor.

La forma más básica es creando una tabla de frecuencias... y para crear esa tabla, lo que necesito es CLASIFICAR los datos:

TENGO PETICIONES.. y cada petición tiene su STATUS CODE.

Petición 1:  STATUS CODE: 200
Petición 2:  STATUS CODE: 404
Petición 3:  STATUS CODE: 500

TABLA DE FRECUENCIAS:
    STATUS CODE | FRECUENCIA
    200         | 10009
    404         | 110
    500         | 11

Esa frecuencia que acabo de calcular, es un DATO CUANTITATIVO.
Cuá es su unidad de medida? "peticiones"

    Tengo 10009 peticiones con status code 200
    Tengo 110 peticiones con status code 404
    Tengo 11 peticiones con status code 500

Eso, a su vez es un dato cuantitativo. -> METRICA
Cuántas peticiones dan cada status code a lo largo del tiempo o en un intervalo de tiempo.

---

# ETL

Procesos de (E)xtracción, (T)ransformación y (L)carga de datos. (L-> Load)

Básicamente, sacar datos de un sistema, transformarlos para llevarlos a otro.
Por ejemplo , lo hacemos mucho en el mundo BI (Business Intelligence).

BBDD -> Datawarehouse <- Paneles(Dashboards) en una herramienta tipo PowerBI

Las BBDD están en entornos de producción.. y los datos, en las BBDD se guardan de forma que se prioriza la eficiencia de las operaciones de lectura y escritura atómicas para el funcionamiento del sistema, no necesariamente para el análisis posterior.

Por contra, en un Datawarehouse los datos se organizan y optimizan para el análisis y la generación de informes, priorizando la eficiencia en consultas complejas y agregaciones sobre grandes volúmenes de datos.

BBDD -> DataLake -> Datawarehouse <- Paneles(Dashboards) en una herramienta tipo PowerBI

Un datalake que hoy en día los usamos mucho son almacenes con información en crudo sin procesar.


BBDD (que contiene la información con la que actualmente trabajamos). Pero datos de 5 años atrás no los quiero aquí.
Haría que la BBDD fuera muy lenta.
Los saco a un Datalake, con la misma estructura que están en origen.
Porteriormente, cuando he decidido hacer algo concreto con esos datos, los prepararo para esa actividad concreta (los cocino) y los dejo en un Datawarehouse.

Para mover datos de un entorno a otro, usamos procesos de tipo ETL. Hay variantes:
- TEL
- TETL

Estos son procesos en su mayoría batch (aunque pueden ser en streaming), que corren a ciertas horas cada día (noche), moviendo y transformando los datos de un entorno a otro según las necesidades del análisis y la generación de informes.

Ejemplo:
- Proceso de alimentación del datawarehouse del departamento de ventas, desde la BBDD de Ventas de coches de segunda mano.
  Y se ejecuta cada noche a las 3am
  Quiero, me puede interesar, realizar un seguimiento de ese trabajo:
   - Qué tareas atómicas va realizando (LOGS)
   - Qué tiempos tarda, o cuántos errrores se producen (MÉTRICAS)

---

# SOBRE LOGs

Una cosa es una entrada en unn fichero de log... y otra cosa a lo que aquí nos estamos refefiriendo como LOG.

Por ejemplo, un log típico de servidor web apache (access log) podría verse así:

```
127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326
```

Ahí lo que tengo no es un log... es una linea de un archivo de log que contiene información sobre una única petición al servidor web.
- Fecha y hora de la petición:           10/Oct/2000:13:55:36 -0700
- Ruta:                                  /apache_pb.gif
- Método HTTP:                           GET
- Código de estado de la respuesta:      200
- Tamaño de la respuesta:                2326

Eso es un log? Ni siquiera sería una entrada de log.
Lo que tenemos es una forma de recopilar METRICAS!

    Métricas:     Es un dato numético (casi siempre cuantificable) asociado a una fecha y a unas etiquetas
    - Tamaño de la respuesta: 2326 bytes!
        Fecha: 10/Oct/2000:13:55:36 -0700
        Tags:
            - Ruta: /apache_pb.gif
            - Método HTTP: GET
            - Código de estado de la respuesta: 200

Muchísimas métricas se recopilan sin logs de por medio (la gran mayoría)
Eso de CPU, RAM, DISCO, RED...nada de esto viene de logs... Hay programas (EXPORTADORES DE METRICAS) que se encargan de recopilar estas métricas directamente del sistema.

Un log, según lo entendemos en la clasificación que hice arriba sería algo más del tipo:

[10/Oct/2000:13:55:36 -0700] INFO App started successfully
[10/Oct/2000:13:55:37 -0700] ERROR Failed to connect to database
[10/Oct/2000:13:55:38 -0700] DEBUG Query executed successfully: SELECT * FROM users

Aqui el log es:
    Query executed successfully: SELECT * FROM users
    Fecha: 10/Oct/2000:13:55:38 -0700
    tag: 
        - Nivel: DEBUG
        - TraceId: 1234567890abcdef


Para procesar estos archivos de log, solemos usar expresiones regulares, herramientas de parsing de logs o sistemas de gestión de logs como ELK (Elasticsearch, Logstash, Kibana) o Fluentd.

---

# Nuestro entorno para el curso.

Tenemos un cluster de kubernetes. Ese cluster tiene 5 nodos y en cada uno de los nodos hay montado un agente de Datadog.

Antiguamente desplegábamos software (una BBDD, una aplicación, un kafka) directamente en los servidores físicos o máquinas virtuales.
Hoy en día hay otra forma (tecnología) para desplegar apps en entornos de producción: Contenedorización.
En este mundo de la contenedorización el concepto básico es el concepto de CONTENEDOR.
Un contenedor es un entorno AISLADO Dentro de una máquina que ejecute LINUX, donde poner procsos a funcionar (BBDD, APPs, Kafkas...)
Son una alternativa al trabajo con máquinas virtuales.

Aunque un contenedor es conceptualmente muy diferente a una máquina virtual, nos ofrece funcionalidades/capacidades similares en la práctica: GEnerar entornos aislados y controlados donde ejecutar apps.

Pero a diferencia de las máquinas virtuales, el mundo de los contenedores esta FUERTEMENTE ESTANDARIZADO.
Y gracias a estar estandarizado, LA OPERACION de sistemas instalados vía contenedores se puede AUTOMATIZAR.

Kubernetes es una herramienta que AUTOMATIZA la creación y operación de entornos de producción basados en contenedores.

Hoy en día, los entornos de producción de las empresas están migrando a KUBERNETES.
KUBERNETES REEMPLAZA A PERSONAS (los antiguos sysadmins).
En kubernetes se trabaja con un concepto llamado POD. Un POD es un conjunto de contenedores que escalan juntos y se depliegan en el mismo host.
Namespace... en kubernetes es un entorno aislado dende controlo lo relativo a un sistema/app. Es útil para separar recursos y gestionar permisos dentro de un mismo cluster.

> Qué hace el agente de datadog en cada nodo?

- Lee los logs de los programas (contenedores) 
- Lee las méticas correspondientes asociadas a esa máquina (host, contenedores)
- Metricas custom
- En paralelo comprueba si tiene conectividad con el servidor de Datadog....

Tenemos montada una app:
- Frontal JS, que se sirve desde un nginx         shop-web              https://datadog.ivanosuna.com
- Backend JAVA.                                   shop-api              https://datadog.ivanosuna.com/api
- Postgres                                        shop-db



    USUARIO - Navegador                                   BACKEND (Cluster de kubernetes)
        https://datadog.ivanosuna.com       ----------->  Nginx que entrega html + js          API JAVA                                     POSTGRES
                                            <-----------       SDK de RUM                          TRACER
        Monta la pantalla y ejecuta los JS
        Como parte de esos JS se incluye
        el SDK de RUM

        Cuando el usuario interactúa,
        se genera un TRACE_ID

        Eso se guarda en los datos de 
        la cabecera HTTP de la petición     -------------------------------------------------> Toma el traceId y crea un SPAN_ID
                                                                                                SPAN_ID: es un identificador único para un segmento específico de la traza
                                                                                                Es lo que identifica al trabajo que se hace en Backend JAVA

                                                                                                Como parte de las tareas 
                                                                                                propias de la app
                                                                                                                        --------------->    Ejecuta su consulta a BBDD
                                                                                                                                            Recibe el TRACE_ID
                                                                                                                                            (en este caso, como comentario en el SQL)
                    |                                           |                                   |                                           |
                    |                                      logs / métricas                      logs/metricas                               logs/metricas
           Habla directamente con los                           |                                   |                                           |
            servidores de datadog                               v                                   v                                           v
                    |                                                       AGENTE DEL NODO / MAQUINA DONDE ESTAN ESOS PROGRAMAS
            Principalmente para el RUM                                                              |
                    |                                                                   Trabajos que puede hacer:
                    |                                                                       Filtrado de datos
                    |                                                                               |
                    v                                                                               v
                    €€€€€                                                                          €€€€
            -------------------------------------------------------------------------------------------------------------------------------------------------
                                    Servidores de datadog
                                    INGESTA. Aquí es donde se aplican TRANSFORMACIONES A LOS DATOS!
                                             (Hay una opción que permite hacer esa transformación en cliente: Observability pipeline, pero se paga adicionalmente)

                                             LOGS: Extracción de datos (etiquetas-facets, measures)
                                             SE EXTRAE el TRACE_ID y el SPAN_ID
                                             GUARDAN METRICAS, TRAZAS

                                    - UI: Acceso a los datos transformados y almacenados (dashboards, notebooks)
                                    - Notificaciones (alertas, integraciones con otros sistemas)

> Quíen decide qué logs y métricas se toman?

    El problema suele ser que NADIE! Y se recopila lo que sea! Las cosas por defecto y lo que desarrollo haya estimado.
    POR QUE ES UN PROBLEMA? Porque la infromación que puedo sacar luego va condicionada por las métricas/logs que he recopilado.
    Si nadie ha puesto cabeza en ello... luego saldrá lo que salga! Que normalmente NO SERVIRÁ A NADIE!

    Cómo debería ser? MUCHA GENTE TOMA ESTA DECISION. Y se toma en base a la información que quiero obtener luego: LAS PREGUNTAS A LAS QUE QUIERO RESPONDER.
    Y CADA PERFIL TIENE SUS PREGUNTAS!

        Desarrollo:
         - Dónde se están produciendo errores?
         - Si hay errores regresivos
         - Rendimiento, cuanto tarda cada operación allí dentro: QUERY, PROCESAR LOS DATOS DE UNA QUERY, CONECTAR CON UN MICROSERVICIO?
         - Uso de un endpoint (quién lo usa)

        Operaciones:
        - CPU, RAM de cada contenedor/pod (BBDD, Tomcat, nginx)... Namespace
        - Salud actual, reinicios, errores, disponibilidad

        Control de Releases/Despliegues:
        - Cada cuanto despliega
        - Si fallan los pipelines de integración/entrega continua/despliegue continuo y por qué
        - Incidencias relacionadas
        - Tiempo para restablecer el servicio en caso de un despliegue fallido
        - Cuánto tardan los despliegues

         Tech lead:
         - Cuánto tiempo desde un commit a producción
         - Calidad del código y cobertura de pruebas antes de llegar a producción (pipelines de CI)
       
         Product Owner?
         - Quién nos consume
         - Usuarios
         - Cómo se usa el producto!
         - Rendimiento
         - Número de errores en operaciones de negocio.
         - Negocio:
           - Cuántas ventas
           - Ticket medio
         - Tiempo que pasa desde que hago una solicitud de funcionalidad (REQUISITO) hasta que se implementa en producción:
           - Definir un identificador de REQUEST
           - Que los desarrolladores en el commit correspondiente lo incluyan.

Quién CONFIGURA ESTAS METRICAS /LOGS EN DATADOG? El observability engineer
Eso trabajará con los datos que le lleguen de los distintos sistemas y servicios para configurar correctamente los logs y métricas en Datadog.
Quién configura los datos que manda cada sistema?
- Desarrollo
- Operaciones

NGINX:
    Logs:
    Métricas:
        - CPU               |
        - RAM               |
        - Disk I/O          | Métricas generales
        - Network I/O       |
        - BACKEND JAVA:                                                     |
          - Garbage collector                                               |   Métricas específicas de cada sistema... aunque muy generales!
          - Número de conexiones simultaneas (DATABASE POOL) a BBDD         |
          - Número de ejecutores                                            |   Datadog me lo regala de serie. Eso es bueno?
          - Número de elemento en cola, pedientes de atender                |    - Depende de para quién?
          - Tiempo en cola de una petición                                  |      El que lo configura se lo ahorra
        - Tamaño de la cola de KAFKA                                        |      El que paga la factura, si el dato realmente luego no se usa, llora!
        - Tiempo de vida de un evento en KAFKA                              |

        - Métricas CUSTOM
             ETL: Cúantos datos se han procesado por la ETL?
                  Cuántos se han descartado por problemas?  

















# DEVOPS

Una cultura, una filosofía, un movimiento en pro de la AUTOMATIZACION.
Cuando en la empresa decimos: Vamos a automatizar todo lo que podamos del ciclo de vida de un producto de sofware, entonces decimos que estamos adoptando una cultura DevOps.

En el mundo nuevo: Metodologías Ágiles, de la automatización, y de las arquitecturas (de componentes desacoplados: microservicios) de hoy en día.
La monitorización/observability se vuelve muy compleja.
Ya no es saber si algo ha fallado. Es identificar de una cadena larguísima QUE FALLO?

Y aquí es donde se hace CRITICO el manejar conceptos que antes no lo eran tanto o era mucho más simple llevar: TRAZA!

    PETICION -> MICROSERVICIOA -> 3 microservicios -> KAFKA (evento) <- LEYENDO ASINCRONAMENTE (Quizás dentro de 3 minutos) otro programa que explota

    PERO LA TRAZA DEBE SER UNICA!
     

Esto es lo que datadog ha integrado muy bien.


LOKI! OPENSEARCH (elasticsearch)! -> LOGGING

PROMETHEUS + GRAFANA -> METRICAS !

# Linux?

Linux NO ES UN SISTEMA OPERATIVO. ES UN KERNEL DE Sistema Operativo.
Un sistema operativo no es un programa que instalo en una computadora.. SON MILES DE ELLOS.
Esos programas los clasificamos en grupos:
- KERNEL
  - El conjunto de programas que:
    - Controla hardware
    - Gestiona recursos del sistema
    - Proporciona servicios básicos a las aplicaciones
    - Controlan seguridad del sistema / usuarios
- UTILIDADES DEL SISTEMA
  - Administrador de tareas
  - Explorador de archivos
- CONSOLAS DE COMANDOS (SHELLS)
  - Powershell
  - cmd.exe

De hecho linux es el kernel de SO más usado del mundo.
De hecho un solo sistema operativo convierte a linux en el más usado del mundo: ANDROID

En los servidores de las empresas solemos usar un sistema operativo llamado GNU/Linux, que combina el kernel de Linux con las utilidades y herramientas del proyecto GNU. Este Sistema oeprativo se ofrece en forma de distribucaciones: RHEL (fedora, Oracle, Rcky, Alma), Debian (Ubuntu, Mint, etc.), SuSE (openSUSE, SLES)

Windows ejecuta Linux? SI, nativamente además... WLS (Windows Subsystem for Linux)

# Windows 

Windows NO ES UN SISTEMA OPERATIVO. Windows es una familia de sistemas operativos desarrollados por Microsoft:
- Windows 10
- Windows 11
- Windows Server 2019
- Windows Server 2022

MNicrosoft ha tenido 2 kernels en su historia:
- DOS -> MSDOS, Windows 1.0 - Windows 3.x, Windows 95, Windows 98, Windows ME
- NT (New technology) -> Windows NT 3.1 - Windows NT 4.0, Windows 2000, Windows XP, Windows Vista, Windows 7, Windows 8, Windows 10, Windows 11, Windows servers (Windows Server 2003, Windows Server 2008, Windows Server 2012, Windows Server 2016, Windows Server 2019, Windows Server 2022)


---
# Herramientas alternativas:

Logging: OpenSearch, Loki, ElasticSearch
Metrics: Prometheus, Grafana, ElasticSearch, Opensearch, ISTIO
Tracing: Jaeger, Zipkin, OpenTelemetry, ISTIO
GITHUB Actions, GITLAB CI/CD, Jenkins. Ejecución de pipelines de CI/CD

En datadog pagamos por la integración de todo eso. Tengo una sola herramienta para todo! Y puedo ir pasando de una cosa a otra!


---

# Sobre Dashboards.

Como usaurios de datadog, lo que más nos va a interesar posiblemente es montarnos nuestros dashboards personalizados para visualizar métricas, logs y trazas de manera centralizada.

Y necesitaré MUCHOS DASHBOARDS de cada sistema!
Cada perfile necesita su dashboard personalizado:
- El que está de guardia
- El equipo de desarrollo
- El product owner
Importante será una buena NOMENCLATURA para los dashboards, para que sea fácil identificarlos y mantenerlos organizados.
NEcesitamos una convención en la nomenclatura:
- [SISTEMA]-[COMPONENTE] [ROLE]
   shop        api        dev
   shop        api        guard
   shop        api        po

Los dashboards en DATADOG se montan mediante WIDGETS.
De qué depende la eleción de un widget u otro?
DEPENDE DEL TIPO DE DATO... Esto ya cierra el abanico un huevo.
Hay tipos de datos para los que tengo 1 widget.
Otro puede ser que tenga 2 o 3.

Mi elección está totalmente condicionada al tipo de datos que quiero visualizar.

- UNIVARIABLE 
    - Datos cualitativos
       Tabla de frecuencias 
              - Pie
              - Barra 
        Estadístico: RECUENTO (Suma de las frecuencias: TOTAL DE DATOS QUE TENGO)

     > Ejemplo: Status code

        | Status Code | Count |
        |-------------|-------|
        | 200         | 150   |
        | 404         | 10    |
        | 500         | 5     |

        En barras:
        | Status Code | Frecuencia                           |
        |-------------|--------------------------------------|
        | 500         | XXXXX                                |
        | 404         | XXXXXXXXXX                           |
        | 200         | XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX |

    - Dato cuantitativo:
        Histograma / Distribution 
        Además de un histograma, que es una primera herramienta de resumen de datos que ofrece la estadística, qué otra cosa puedo sacar? RESUMIENDO MAS!
        INDICADORES ESTADÍSTICOS: Media, Mediana, Desviación estándar, Percentiles

     > Ejemplo: Tiempo de respuesta, Tamaño de la respuesta, Tiempo de un despliegue, Tiempo entre despliegues, cantidad de productos vendidos por ticket

        Tiempo de respuesta de un servicio:
        - Percentiles   p99 p95 p90  Tiempo máximo que espera el 90% de la gente y el 99% de la gente.
        - Media?        En general NUNCA!
          - Todo lo que sean tiempos de respuesta, la media no me interesa nada!
            La media es un indicador muy mal entendido y usado, con el que desgradiadamente estamos muy acostumbrados a trabajar, pero que en el contexto de tiempos de respuesta puede ser engañoso.
            Mediana P50 es el guay! como indicador de tendencia central... Es lo que tiene que esperar la mitad de la gente.

                Tiempo de respuesta medio: 700ms con desv. típica de 50ms

- BIVARIABLES
    - Cualitativa x Cualitativa
      - Código de estado, Sistema, Pais
        - Tabla de contingencia (cross-tab). En casillas se representa Frecuencia de la combinación de valores:
                                        Sistema
                STATUS CODE             ventas      backup      etl
                    200                 100          50          30
                    300                 80           40          20
                    400                 60           30          10
                    500                 50           20          5
        - Representación:
        - Barras: apiladas o agrupadas
        - Mapa de calor
        - Tree map
        - Sankey

    - Cuantitativa x Cualitativa
      > Código de estado (status http), fecha
        - Histograma apilado
          - Eje X -> Cuantitativa: FECHA
          - Eje Y -> Cualitativa: CÓDIGO DE ESTADO HTTP
    - Cuantitiva x Cuantitativa
    - Representación: 
      - Scatter plot (diagrama de dispersión)

Una variable con la que SIEMPRE TRABAJAMOS AQUI ES: Tiempo / Fecha/Hora

Fecha que tipo de dato es? Cualitativo o cuantitativo? CUANTITATIVO (claro que se puede sumar!)

Dentro de los datos cuantitativos hay 2 tipos:

- Intervalo    Cuando no hay cero absoluto          Operaciones: - (y sumas en para algunas cosas)
- Razón        Cuando hay cero absoluto             Operaciones: + - * / 

Las fechas son datos de INTERVALO. Existe un año 0 absoluto ? La fecha del bigbang... pero nosotros no medimos la fecha desde ese instante del tiempo... lo hemos traslados y eso hace que el 0 ya no sea absoluto.


Número de hijos de una familia: -5 hijos? NO.. el CERO Es ABSOLUTO. -> RAZON    
    Familia 1: 2 hijos
    Familia 2: 4 hijos
        Tiene sentido decir que la familia 1 tiene 2 hijos menos que la 2? SI (-)
        Tiene sentido decir que la familia 2 tiene el doble de hijos que la 1? SI (*)
        Tiene sentido decir que la familia 1 tiene la mitad de hijos que la 2? SI (/)
        Tiene sentido decir que entre las dos familias tiene. 6 hisjo (+)
Temperatura ºC en Una ciudad: INTERVALO!

    Santiago de compostela: 10º
    Sevilla:                30º

    Tiene sentido decir que en sevilla hace 20 grados mas que en Santiago de compostela? SI (-)
    Tiene sentido decir que etre santiago y sevilla suman 40 grados? NINGUNO
        Si bien esto carece de sentido, la operación puede hacerse y tiene esentido e otros casos:  
            La media es de 20º entre Santiago de compostela y Sevilla.

    Tiene sentido decir que en Sevilla hace el triple de temperatura que en Santiago de compostela? SI? Y si hacen -10º y 20º qe digo? que hace menos el doble?
    NO PUEDO NO TIENE SENTIDO.


Fecha:
La medimos desde el año 1-enero-0   Este es el cero relativo.
Realmente lo que medimos son días desde esa fecha... pero para no usar cosas como 1762735 días, agrupamos en años y meses.

Fecha/hora es lo mismo.

1-enero he vendido 100 cosas
2-enero he vendido 200 cosas

La variable MES... qué tipo es? CUALITATIVA NOMINAL!
La variable día... qué tipo es? CUALITATIVA NOMINAL!
La variable HORA del día: CUALITATIVA NOMINAL!

Si extraigo partes de la fecha hora, lo que quedan son variables NOMINALES, salvo 1: AÑO!

---

Un proyecto de este estilo debe partir por los cuadros de mando!
Es lo que desarrollo, pero lo primero que planteo.
Los cuadros condicionan totalmente los datos que necesito.
Y los datos que necesito condicionan el tratamiento que debo aplicar.
Y a su ver la captura/exportación en origen.