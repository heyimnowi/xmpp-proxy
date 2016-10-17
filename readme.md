El servidor que maneja el chat va a estar aca:

ec2-54-69-136-236.us-west-2.compute.amazonaws.com

El mismo corre este programa que es una implementacion del protocolo XMPP:

prosody (mas info goolgear)

Vamos a estar usando los siguientes clientes de XMPP para realizar pruebas con nuestro proxy

Adium si tenes OSx
Pidgin si tenes ubuntu

--------

admins = { "admin@nowixmppserver" }
authentication = "internal_plain"
allow_registration = true;
modules_enabled = {
  "roster"; -- Allow users to have a roster. Recommended ;)
  "register"; -- Allow users to register on this server using a client and change passwords
  "admin_adhoc"; -- Allows administration via an XMPP client that supports ad-hoc commands
  "saslauth"; -- Authentication for clients and servers. Recommended if you want to log in.
  --"admin_telnet"; -- Opens telnet console interface on localhost port 5582
  -- "watchregistrations"; -- Alert admins of registrations
}
log = {
  debug = "/var/log/prosody/prosody.log"; -- Change 'info' to 'debug' for verbose logging
  error = "/var/log/prosody/prosody.err";
  -- "*syslog"; -- Uncomment this for logging to syslog
  -- "*console"; -- Log to the console, useful for debugging with daemonize=false
}
daemonize=false
VirtualHost "nowixmppserver"



-----------

usando Adium:

La configuracion va asi:

Jabber ID:

un_nombre@nowixmppserver

Password:
gilada

Connect Server:
ec2-54-69-136-236.us-west-2.compute.amazonaws.com

resource:
nowixmppserver

Register Account:
Pones nowixmppserver 5222
