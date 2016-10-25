
# Trabajo Práctico Especial 2016/2

## Abstract

   Este documento describe el Trabajo Especial de la materia Protocolos
   de comunicación para la cursada del segundo cuatrimestre de 2016.

## Requirements Language

   The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this
   document are to be interpreted as described in RFC 2119 [RFC2119].

## Tabla de Contenidos

   1.  Requerimientos Funcionales

   1.1.  Objetivo 

   **1.2.  Funcionamiento**

   1.3.  Concurrencia

   1.4.  Fallos

   **1.5.  Registros de acceso**

   1.6.  Métricas

   1.7.  Multiplexador de Cuentas

   **1.8.  Silenciar usuarios**

   **1.9.  Transformaciones de mensajes requeridos**

   **1.10. Stream Initiation File Transfer**

   1.11. Configuración  

   1.12. Monitoreo Remoto

   2.  Requerimientos No Funcionales

   2.1.  Performance

   2.2.  Lenguaje

   2.3.  Librerías Externas

## 1.  Requerimientos Funcionales

### 1.1.  Objetivo

   El objetivo del trabajo es que los alumnos implementen un servidor
   proxy para el Extensible Messaging and Presence Protocol XMPP (XMPP)
   [RFC6120] que pueda ser utilizado por clientes XMPP existentes.  El
   proxy proveerá al usuario algunos servicios extras que el servidor de
   origen XMPP no provee (como ser la manipulación del contenido del
   mensaje de chat).

   Otros RFCs PUEDEN aplicar para la realización de este trabajo (Como
   ser [RFC6121]).  Lo mismo aplica para otras extensiones de XMPP [1].

   Los alumnos DEBEN demostrar habilidad para la programación de
   aplicaciones cliente/servidor con sockets, la comprensión de
   estándares de la industria, y que son capaces de diseñar protocolos.

   Se RECOMIENDA que los alumnos instalen su propios servidores de XMPP
   de forma local ya que la mayoría de los servidores XMPP requiere de
   mecanismos de encriptación que están por fuera del objetivo del
   trabajo práctico.

### 1.2.  Funcionamiento

   El usuario DEBERÁ configurar su cliente de chat para que se utilice
   el proxy.  No será necesario soportar STARTTLS, TLS, SSL.

### 1.3.  Concurrencia

   El servidor proxy DEBE soportar múltiples clientes de forma
   concurrente y simultánea.  Se DEBE tener en cuenta en la
   implementación aquellos factores que afecten la performance.

### 1.4.  Fallos

   El servidor proxy DEBE reportar los fallos a los User-Agents usando
   toda la potencia del protocolo XMPP.

### 1.5.  Registros de acceso

   El servidor proxy DEBE dejar registros de los accesos en la consola
   y/o en un archivo que permitan entender que requests están pasando
   por el proxy y su resultado.

   Consejo: es conveniente observar como registran los accesos los
   servidores XMPP tradicionales.  Imprimir todos los bytes que pasa por
   el proxy no es algo que se desea.

### 1.6.  Métricas

   El sistema DEBE implementar mecanismos que recolecten métricas para
   entender el funcionamiento del sistema

   o  cantidad de accesos

   o  bytes transferidos

   o  cualquier otra métrica que considera oportuno para el
      entendimiento del funcionamiento dinámico del sistema

   Las estadísticas PUEDEN ser volátiles (si se reinicia el servidor las
   estadísticas pueden perderse).

### 1.7.  Multiplexador de Cuentas

   Se DEBE implementar mecanismos que permitan configurar el sistema
   para que un JID sea mapeado a un servidor origen diferente de
   configurado por defecto.  Por ejemplo el administrador PODRÁ
   configurar como servidor origen por defecto a xmpp.example.org
   mientras que destinar el usuario "jperez" al servidor
   "xmpp.backoffice.example.org".

### 1.8.  Silenciar usuarios

   El sistema DEBE implementar mecanismos que permitan filtrar todos los
   mensajes entrantes y salientes a un cierto usuario en forma elegante,
   con mecanismos para indicar el error al usuario enviando el mensaje.

### 1.9.  Transformaciones de mensajes requeridos

   Se DEBE implementar las siguientes transformaciones:

   o  transformar texto del mensaje utilizando formato l33t: Tendrá que
      ser posible modificar el contenido de un mensaje (chat o mensaje)
      que realizando las siguientes sustituciones

      *  a por 4 (cuatro)

      *  e por 3 (tres)

      *  i por 1 (uno)

      *  o por 0 (cero)

      *  c por < (menor)

### 1.10.  Stream Initiation File Transfer

   Adicionalmente se implementará la extensión SI File Transfer (Stream
   Initiation File Transfer [2])

### 1.11.  Configuración

   La configuración referida a transformaciones, multiplexado, etc, DEBE
   poder ser modificada en tiempo de ejecución de forma remota.  Para
   esto deberá diseñar un protocolo el cual DEBERÁ estar debidamente
   documentado con ABNF [RFC5234]

### 1.12.  Monitoreo Remoto

   El servidor DEBE exponer un servicio (para el cual se DEBE proveer un
   protocolo) para que sea posible monitorear el funcionamiento del
   mismo.  El mismo DEBE proveer el acceso a las estadísticas
   recolectadas.


## 2.  Requerimientos No Funcionales

### 2.1.  Performance

   Se DEBE tener en cuenta todos los aspectos que hagan a la buena
   performance y escalabilidad del servidor.  Se espera que se maneje de
   forma eficiente los streams de información (ej: mensajes muy
   grandes).  El informe DEBE contener información sobre testing de
   stress.  Por ejemplo

   o  ¿cuales es la máxima cantidad de conexiones simultaneas que
      soporta?

   o  ¿como se degrada el throughput?

   La implementación del proxy XMPP DEBE hacerse usando operaciones no
   bloqueantes.

### 2.2.  Lenguaje

   El servidor DEBE implementarse con la Java Platform, Standard Edition
   8 Release.  NO SE PODRÁ utilizar para implementar la parte proxy XMPP
   librerías desarrolladas por terceros.  De ser necesario, se DEBE
   implementar un cliente XMPP propio.

### 2.3.  Librerías Externas

   NO SE PODRÁ utilizar ninguna librería externa que provea soluciones
   out-of-the-box para los problemas de exponer servicios de red (como
   Apache MINA, Netty, y otros).

   NO SE PODRÁ utilizar ninguna librería externa que implementen el
   protocolo XMPP de forma parcial o total (como ser Jabber Stream
   Objects [3], o Smack API [4]).

   Se PODRÁ utilizar codificadores y decodificadores ya existentes
   (base64, quoted-printable, ...).

   Se PODRÁ utilizar codificadores y decodificadores XML ya existentes,
   así como funciones de hash.

   Está permitido utilizar las librerías:

   o  Apache commons-lang [5]

   o  Apache commons-codec [6] para realizar codificación y
      decodificación.

   o  Junit y Mockito/JMock/EasyMock para testing

   o  Spring Framework [7] / Google Guice [8] para inversión de control.

   o  JAXB para manipular archivos xml como objetos (ej: para la
      configuración)

   o  ImageIO para manipular imagenes

   o  Log4j, LogBack, SLF4j, Apache commons-logging para realizar
      logging

   Cualquier otra librería que se quiera usar DEBE tener una licencia
   OSI approved [9] y DEBE ser aprobada por la Cátedra.  Para lograr
   esta aprobación se DEBE enviar un mail a la lista de correo [10] con
   el nombre de la librería, y el uso que se le quiere dar.  Las
   librerías aprobadas para un grupo automáticamente están aprobadas
   para todos y por lo tanto PUEDEN ser utilizadas por otros grupos
   siempre y cuando se la use con los mismos fines.
