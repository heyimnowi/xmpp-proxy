## Installation and execution

Clone repository or download

Execute:

mvn install

mvn exec:java   (-Dexec.args="-v" for verbose)

## AWS Server
ec2-54-69-136-236.us-west-2.compute.amazonaws.com

## XMPP Server
prosody

## XMPP Client
Adium

## Prosody AWS Config

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

## Adium config

- Jabber ID: username@nowixmppserver
- Connect Server: ec2-54-69-136-236.us-west-2.compute.amazonaws.com
- resource: nowixmppserver
- Register Account: nowixmppserver 5222
