
# Aplicación de Chat con Persistencia en PostgreSQL

Una aplicación de chat estilo **WhatsApp**, desarrollada en **Java**, con comunicación **TCP/UDP** y persistencia de datos en una **base de datos PostgreSQL local**.

## Características

* Registro y autenticación de usuarios
* Mensajería privada
* Chats grupales
* Notas de voz (mensajes de audio)
* Llamadas de voz en tiempo real (UDP)
* **Persistencia en PostgreSQL** para:

  * Usuarios
  * Mensajes (privados y grupales)
  * Grupos y membresías
  * Historial de llamadas

---

## Configuración de la Base de Datos

### Requisitos previos

* **PostgreSQL** versión 12 o superior
* **Java 17** o superior
* **Gradle** instalado

### ⚙️ Configuración básica

Esta aplicación utiliza una **base de datos local** por defecto.
Es decir, PostgreSQL debe estar instalado y ejecutándose en el mismo computador donde corre el servidor Java.

1. **Crear la base de datos:**

   ```bash
   createdb chatdb
   ```

2. **Configurar la conexión (opcional):**

   Por defecto, la aplicación se conecta usando los siguientes valores:

   * Host: `localhost`
   * Puerto: `5432`
   * Base de datos: `chatdb`
   * Usuario: `postgres`
   * Contraseña: `postgres`

   Si deseas usar otras credenciales, puedes definirlas mediante variables de entorno:

   ```bash
   export DATABASE_URL="postgres://usuario:contraseña@localhost:5432/chatdb"
   # O bien:
   export DB_USER="tu_usuario"
   export DB_PASSWORD="tu_contraseña"
   ```

   La aplicación creará automáticamente las tablas necesarias en el primer inicio.

---

##  Conexión desde otros equipos

Para conectar clientes que se encuentren en **otros computadores**, es necesario que:

1. Todos los equipos estén **en la misma red local (LAN o WiFi)**.
2. Se conozca la **dirección IP del servidor**, es decir, el computador donde corre el servidor Java y la base de datos PostgreSQL.
3. En el código o configuración del cliente se reemplace `localhost` por la IP del servidor.
   Por ejemplo:

   ```java
   private static final String SERVER_HOST = "192.168.1.10"; // IP del servidor
   private static final int SERVER_PORT = 5000;
   ```
4. PostgreSQL debe permitir conexiones remotas (configurando `postgresql.conf` y `pg_hba.conf` si es necesario).

---

##  Esquema de la Base de Datos

La aplicación crea automáticamente las siguientes tablas:

* **users** → Cuentas de usuario con estado en línea
* **groups** → Grupos de chat
* **group_members** → Relaciones de membresía (muchos a muchos)
* **messages** → Mensajes privados y grupales
* **calls** → Historial de llamadas con duración y estado

---

##  Ejecución del Proyecto

### 1. Compilar el proyecto

```bash
./gradlew build
```

### 2. Ejecutar el servidor principal

```bash
./gradlew runServer
```

### 3. Ejecutar el servidor de voz (para llamadas)

```bash
./gradlew runVoiceServer
```


### 4. Ejecutar un cliente

```bash
./gradlew runClient
```

---

## 💡 Uso

1. Inicia primero el **servidor principal y el de voz**
2. Luego ejecuta uno o varios **clientes** (pueden estar en otros equipos de la red local)
3. Regístrate o inicia sesión con un nombre de usuario
4. Utiliza el menú para:

   * Enviar mensajes privados
   * Crear y unirte a grupos
   * Enviar mensajes grupales
   * Realizar llamadas de voz
   * Enviar notas de voz
   * Consultar historial de mensajes (guardado en PostgreSQL)

---

##  Persistencia de Datos

Toda la información se guarda de forma permanente en la base de datos PostgreSQL local:

* **Usuarios** → se conservan entre sesiones
* **Mensajes** → almacenados y recuperables
* **Grupos y membresías** → se mantienen activos
* **Llamadas** → registradas con duración y estado

---

## Arquitectura Técnica

* **TCP** → Para mensajes, comandos y señalización
* **UDP** → Para audio en tiempo real durante llamadas
* **PostgreSQL** → Almacenamiento persistente
* **HikariCP** → Pool de conexiones eficiente a la base de datos
