# RabbitMQ Cluster con Docker Compose y Spring Boot

Proyecto práctico para el despliegue, federación y monitoreo de un clúster de **RabbitMQ** (2 nodos) mediante **Docker Compose**, integrado con una aplicación **Spring Boot 3.2+** que implementa administración programática de recursos (`RabbitAdmin`) y control dinámico del ciclo de vida de los consumidores (`RabbitListenerEndpointRegistry`).

---

## 🏛️ Arquitectura del Sistema

```
                 +-----------------------------------------+
                 |       Spring Boot 3 (Port 8080)         |
                 |  - RabbitAdmin                          |
                 |  - RabbitListenerEndpointRegistry       |
                 +-----------------------------------------+
                                      |
                           AMQP (Port 5672)
                                      v
+-----------------------------------+   Erlang (25672)    +-----------------------------------+
|      rabbitmq-1 (Master)          |<------------------->|       rabbitmq-2 (Peer)           |
|-----------------------------------|   Erlang Cookie     |-----------------------------------|
| AMQP: 5672 | Management: 15672    |  "secret_cookie..." | AMQP: 5673 | Management: 15673    |
| Volume: rabbitmq_data_1           |                     | Volume: rabbitmq_data_2           |
| Config: /etc/rabbitmq/rabbitmq.conf|                     | Config: /etc/rabbitmq/rabbitmq.conf|
+-----------------------------------+                     +-----------------------------------+
```

### Características Clave
* **Alta Disponibilidad:** Dos nodos RabbitMQ corriendo sobre una red tipo bridge compartiendo metadatos Mnesia.
* **Seguridad y Clustering:** Autenticación inter-nodo mediante `RABBITMQ_ERLANG_COOKIE` compartida.
* **Gestión Dinámica:** Creación, purga y eliminación de colas y exchanges en caliente sin reiniciar servicios.
* **Control de Consumo:** Capacidad de pausar (`stop`) y reanudar (`start`) listeners AMQP mediante llamadas REST.

---

## 📁 Estructura del Repositorio

```text
rabbitmq-avanzado/
├── docker-compose.yml                      # Orquestación de los contenedores RabbitMQ
├── rabbitmq.conf                           # Configuración de peer discovery del clúster
├── pom.xml                                 # Dependencias Maven (Web, AMQP, Lombok)
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           ├── RabbitMQAdvancedApplication.java     # Entry point Spring Boot
        │           ├── config/
        │           │   ├── RabbitAdminConfig.java           # Configuración de RabbitAdmin y RabbitTemplate
        │           │   └── RabbitListenerConfig.java        # Fábricas de contenedores con concurrencia
        │           ├── controller/
        │           │   ├── ListenerController.java          # REST: Pausa y reanudación de listeners
        │           │   └── RabbitMQController.java          # REST: Gestión de colas, exchanges y bindings
        │           ├── listener/
        │           │   └── OrderListener.java               # Consumidor de prueba ("orders.queue")
        │           └── service/
        │               ├── ListenerManagementService.java   # Lógica de ciclo de vida de listeners
        │               └── RabbitResourceManager.java       # Operaciones de infraestructura con RabbitAdmin
        └── resources/
            └── application.properties              # Parámetros de conexión y logging
```

---

## 📋 Requisitos Previos

* **Docker Engine** (v20.10+) y **Docker Compose** (v2.0+)
* **Java Development Kit (JDK)** 17 o superior
* **Apache Maven** 3.8+ (o Maven Wrapper)
* Herramienta para peticiones HTTP (`curl`, Postman, Thunder Client)

---

## 🚀 Puesta en Marcha

### 1. Iniciar la Infraestructura RabbitMQ

Sitúate en la raíz del repositorio y levanta los contenedores en segundo plano:

```bash
docker-compose up -d
```

Verifica que ambos nodos estén en estado `Up`:

```bash
docker-compose ps
```

### 2. Validar la Cookie de Erlang

Ambos nodos deben compartir el mismo secreto para poder sincronizarse:

```bash
docker exec rabbitmq-1 rabbitmqctl eval 'erlang:get_cookie().'
docker exec rabbitmq-2 rabbitmqctl eval 'erlang:get_cookie().'
```
*Salida esperada en ambos:* `'secret_cookie_value'`

### 3. Verificar el Estado del Clúster

```bash
docker exec rabbitmq-1 rabbitmqctl cluster_status
```

Ambos nodos deben figurar bajo `Disk Nodes` y `Running Nodes`:
```text
Disk Nodes
  rabbit@rabbitmq-1
  rabbit@rabbitmq-2

Running Nodes
  rabbit@rabbitmq-1
  rabbit@rabbitmq-2
```

> **Nota:** Si `rabbitmq-2` no se unió automáticamente en el primer arranque, consulta la sección de [Solución de Problemas](#-solución-de-problemas).

---

## ☕ Ejecución de la Aplicación Spring Boot

Compila y arranca la aplicación:

```bash
mvn clean install
mvn spring-boot:run
```

La aplicación quedará disponible en `http://localhost:8080`.

---

## 🌐 Consolas Web de Administración

Puedes inspeccionar el clúster desde el navegador (Credenciales: `admin` / `admin123`):

| Nodo | URL Management UI | Puerto AMQP |
| :--- | :--- | :--- |
| **rabbitmq-1** | [http://localhost:15672](http://localhost:15672) | `5672` |
| **rabbitmq-2** | [http://localhost:15673](http://localhost:15673) | `5673` |

---

## 📡 Referencia de Endpoints REST

### 1. Gestión de Recursos RabbitMQ (`/api/rabbitmq`)

| Método | Endpoint | Parámetros | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/rabbitmq/queues` | `queueName` (query) | Crea una cola durable |
| `POST` | `/api/rabbitmq/exchanges` | `exchangeName` (query) | Crea un Direct Exchange |
| `POST` | `/api/rabbitmq/bindings` | `queueName`, `exchangeName`, `routingKey` | Asocia una cola a un exchange |
| `GET` | `/api/rabbitmq/queues/{queueName}` | Path param | Obtiene conteo de mensajes y consumidores |
| `POST` | `/api/rabbitmq/queues/{queueName}/purge` | Path param | Elimina los mensajes pendientes de la cola |
| `DELETE`| `/api/rabbitmq/queues/{queueName}` | Path param | Elimina la cola indicada |

### 2. Control Dinámico de Listeners (`/api/listeners`)

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/listeners` | Lista todos los IDs de listeners activos |
| `POST` | `/api/listeners/{listenerId}/pause` | Pausa el consumo (`container.stop()`) |
| `POST` | `/api/listeners/{listenerId}/resume` | Reanuda el consumo (`container.start()`) |

---

## 🧪 Pruebas Paso a Paso (cURL)

Abre otra terminal y ejecuta el siguiente flujo de prueba:

```bash
# 1. (OPCIONAL) Solo si no se ha creado aún la cola o si quieres crear una nueva
# Crear la cola que escucha el listener (orders.queue)
curl -i -X POST "http://localhost:8080/api/rabbitmq/queues?queueName=orders.queue"

# 2. Consultar listeners registrados (retorna ["order-listener"])
curl -i http://localhost:8080/api/listeners

# 3. Pausar temporalmente el listener de órdenes
curl -i -X POST "http://localhost:8080/api/listeners/order-listener/pause"

# 4. Reanudar el listener
curl -i -X POST "http://localhost:8080/api/listeners/order-listener/resume"

# 5. Consultar métricas de la cola
curl -i "http://localhost:8080/api/rabbitmq/queues/orders.queue"
```

## 🧪 Pruebas de Flujo de Mensajería y Control Dinámico de Listeners

En esta sección se evalúa el comportamiento del sistema ante dos escenarios habituales:
1. **Consumo en tiempo real** de mensajes individuales.
2. **Buffer/Acumulación de mensajes en ráfaga (*burst*)** al suspender dinámicamente el consumidor, seguido del procesamiento concurrente al reanudarlo.

---

### Escenario 1: Envío y Consumo Inmediato de Mensajes Individuales

Envía un mensaje individual para verificar que el listener activo lo procesa en tiempo real:

```bash
curl -i -X POST "http://localhost:8080/api/rabbitmq/orders?orderId=ORD-1001"
```

* **Respuesta HTTP:** `200 OK` con el mensaje:
  ```text
  Orden enviada a la cola: ORD-1001
  ```
* **Comportamiento en Logs (Spring Boot):**
  El contenedor toma la orden de inmediato y simula el trabajo durante 2 segundos (`Thread.sleep(2000)`):
  ```text
  [pool-X-thread-Y] INFO  c.e.listener.OrderListener - Procesando orden: ORD-1001
  [pool-X-thread-Y] INFO  c.e.listener.OrderListener - Orden procesada: ORD-1001
  ```

---

### Escenario 2: Acumulación en Ráfaga (*Burst*) con Listener Pausado

Este flujo demuestra la capacidad de RabbitMQ para actuar como un búfer de alta disponibilidad y resiliencia: la aplicación puede suspender temporalmente el procesamiento de tareas (por ejemplo, para mantenimiento de dependencias downstream) sin perder mensajes, procesándolos de manera concurrente al reactivarse.

#### Paso 1: Pausar el Listener dinámicamente
Detén el consumo de mensajes de la cola `orders.queue`:

```bash
curl -i -X POST "http://localhost:8080/api/listeners/order-listener/pause"
```
* **Respuesta:** `Listener pausado: order-listener`
* **Log en Spring Boot:** `Listener pausado (detenido): order-listener`

#### Paso 2: Enviar ráfaga masiva de mensajes
Con el consumidor detenido, despacha una ráfaga de 12 órdenes ficticias:

```bash
curl -i -X POST "http://localhost:8080/api/rabbitmq/orders/burst?count=12&prefix=ORD-BURST"
```
* **Respuesta:**
  ```text
  Se generaron y enviaron 12 órdenes con prefijo 'ORD-BURST'
  ```

#### Paso 3: Validar el encolamiento y acumulación en el Broker
Verifica el estado de la cola consultando la API:

```bash
curl -s "http://localhost:8080/api/rabbitmq/queues/orders.queue"
```
* **Respuesta JSON esperada:**
  ```json
  {
    "name": "orders.queue",
    "messageCount": 12,
    "consumerCount": 0
  }
  ```
* **En RabbitMQ Management (`http://localhost:15672`):**
  En la pestaña **Queues**, la cola `orders.queue` mostrará `12 Ready Messages` y `0 Consumers`. Los mensajes permanecen retenidos y persistidos de forma segura en el clúster.

#### Paso 4: Reanudar el Listener y observar el procesamiento concurrente
Reactiva el consumidor:

```bash
curl -i -X POST "http://localhost:8080/api/listeners/order-listener/resume"
```
* **Respuesta:** `Listener reanudado: order-listener`
* **Comportamiento en Logs (Spring Boot):**
  Gracias a la concurrencia configurada (`setConcurrentConsumers(3)`), 3 hilos tomarán lotes de mensajes simultáneamente, liquidando las 12 órdenes en aproximadamente 8 segundos (en vez de los 24 segundos que tomaría en modo secuencial):
  ```text
  [orderListenerFactory-1] INFO  c.e.listener.OrderListener - Procesando orden: ORD-BURST-001-a1b2
  [orderListenerFactory-2] INFO  c.e.listener.OrderListener - Procesando orden: ORD-BURST-002-c3d4
  [orderListenerFactory-3] INFO  c.e.listener.OrderListener - Procesando orden: ORD-BURST-003-e5f6
  ...
  [orderListenerFactory-1] INFO  c.e.listener.OrderListener - Orden procesada: ORD-BURST-001-a1b2
  [orderListenerFactory-2] INFO  c.e.listener.OrderListener - Orden procesada: ORD-BURST-002-c3d4
  [orderListenerFactory-3] INFO  c.e.listener.OrderListener - Orden procesada: ORD-BURST-003-e5f6
  ```

#### Paso 5: Confirmar cola procesada
Una vez completado el procesamiento, valida que la cola haya retornado a cero mensajes pendientes:

```bash
curl -s "http://localhost:8080/api/rabbitmq/queues/orders.queue"
```
```json
{
  "name": "orders.queue",
  "messageCount": 0,
  "consumerCount": 3
}
```

---

## 🔍 Monitoreo Desatendido

Para monitorear el tráfico y eventos del clúster en tiempo real mientras realizas pruebas de carga o consumo:

```bash
# Terminal 1 - Logs en vivo Nodo 1:
docker-compose logs -f rabbitmq-1

# Terminal 2 - Logs en vivo Nodo 2:
docker-compose logs -f rabbitmq-2
```

---

## 🛠️ Solución de Problemas

### 1. El clúster no se federó (`rabbitmq-2` aparece aislado)
Si al ejecutar `cluster_status` solo figura `rabbit@rabbitmq-1`, fuerza la unión en caliente con estos comandos:

```bash
docker exec rabbitmq-2 rabbitmqctl stop_app
docker exec rabbitmq-2 rabbitmqctl reset
docker exec rabbitmq-2 rabbitmqctl join_cluster rabbit@rabbitmq-1
docker exec rabbitmq-2 rabbitmqctl start_app
```

### 2. Reinicio completo desde cero
Si necesitas purgar todos los datos anteriores y reiniciar el clúster con volúmenes limpios:

```bash
docker-compose down -v
docker-compose up -d
```

### 3. Error `The import org.springframework.amqp.core.ConnectionFactory cannot be resolved`
En Spring AMQP, `ConnectionFactory` debe importarse desde:
```java
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
```