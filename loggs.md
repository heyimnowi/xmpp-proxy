## Logg




### Connections

#### INFO

**Proxy Started**

      YY-MM-DD HH:MM:SS INFO Proxy started

**XMPP Client connected**

      YY-MM-DD HH:MM:SS INFO Client user@server have connected

**XMPP Client disconnected**

      YY-MM-DD HH:MM:SS INFO Client user@server have disconnected



#### DEBUG

**New XMPP Client connection to XMPP Proxy**


      YY-MM-DD HH:MM:SS DEBUG Accepted new client connection from XXX.XXX.XXX.XXX:PORT

**New socket between XMPP Client and XMPP Proxy**


      YY-MM-DD HH:MM:SS DEBUG opened new socket from XXX.XXX.XXX.XXX:PORT_CLIENT to XXX.XXX.XXX.XXX:PORT_PROXY

**New socket between XMPP Proxy to XMPP server**


      YY-MM-DD HH:MM:SS DEBUG opened new socket from XXX.XXX.XXX.XXX:PORT_PROXY to XXX.XXX.XXX.XXX:PORT_SERVER

**XMPP Client and XMPP Proxy socket closed**


      YY-MM-DD HH:MM:SS DEBUG Client[XXX.XXX.XXX.XXX:PORT_CLIENT] to XMPP Proxy[XXX.XXX.XXX.XXX:PORT_SERVER] socket closed

**XMPP Proxy and XMPP Server socket closed**


      YY-MM-DD HH:MM:SS DEBUG XMPP Proxy[XXX.XXX.XXX.XXX:PORT_PROXY] to XMPP Server[XXX.XXX.XXX.XXX:PORT_SERVER] socket closed



#### WARN

**Connection closed**


      YY-MM-DD HH:MM:SS WARN Connection closed by client



#### ERROR

**Error closing XMPP Client - XMPP Proxy socket or XMPP Proxy - XMPP Server**

      YY-MM-DD HH:MM:SS ERROR Error closing server and client channels




### Messages

#### INFO

**Filtered messages**

      YY-MM-DD HH:MM:SS INFO Message from user@server filtered
