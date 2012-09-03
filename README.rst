ElasticSearch WebSocket transport plugin
========================================

This is an implementation of WebSockets for Elasticsearch.

WebSockets are implemented as an `Elasticsearch transport plugin <http://www.elasticsearch.org/guide/reference/modules/plugins.html>`_ using the latest implementation of WebSockets in the `Netty project <http://netty.io>`_.

The `WebSocket protocol specification <http://tools.ietf.org/html/rfc6455>`_ defines an API that enables web clients to use the WebSockets protocol for two-way communication with a remote host. It  defines a full-duplex communication channel that operates through a single socket over the Web. WebSockets provide an enormous reduction in unnecessary network traffic and latency compared to the unscalable polling and long-polling solutions that were used to simulate a full-duplex connection by maintaining two connections. WebSocket-based applications place less burden on servers, allowing existing machines to support more concurrent connections.

ElasticSearch offers a HTTP REST API for nearly all the features available, so using it via ``curl`` or via script languages is comparable to a HTTP client connecting to a HTTP server. Some limitations apply when using the REST API and WebSockets come to the rescue.

Motivations for implementing an ElasticSearch WebSocket transport layer are

- to supersede the HTTP request/response model by a full-duplex communication channel

- to implement scalable and responsive real-time apps, like distributed publish/subscribe services 

- to attach thousands of clients to an ElasticSearch node without service degradation

- to allow sequences of bulk index and delete operations on a single connection

- to implement new types of streaming applications like subscribing to change streams from ElasticSearch indexes

Installation
------------

The current version of the plugin is **1.0.0**

In order to install the plugin, please run

``bin/plugin -install jprante/elasticsearch-transport-websocket/1.0.0``.

In case the version number is omitted, you will have the source code installed for manual compilation.

================ ================
WebSocket Plugin ElasticSearch
================ ================
master           0.20.x -> master
1.0.0            0.20.x           
================ ================

Attention: you need at least **Java 7** to run the WebSocket transport plugin.

Documentation
-------------

`The Maven project site <http://jprante.github.com/elasticsearch-transport-websocket>`_

`The Javadoc API <http://jprante.github.com/elasticsearch-transport-websocket/apidocs/index.html>`_

Overview
--------

.. image:: https://github.com/jprante/elasticsearch-transport-websocket/blob/master/src/main/site/images/transport-modules.png?raw=true

The transport plugin uses Netty WebSockets for server and clients. WebSocket clients can connect to an Elasticsearch Node with the transport plugin installed. Between nodes, WebSockets connections may establish when needed for forwarding messages. The more nodes are installed with websocket transport, the more clients can get connected.

WebSocket Module
================

.. image:: https://github.com/jprante/elasticsearch-transport-websocket/blob/master/src/main/site/images/elasticsearch-websocket.png?raw=true

The WebSocket module includes a module that allows to expose the *elasticsearch* `API` over HTTP. It is superseding the standard HTTP module on port 9200-9299.

The http mechanism is completely asynchronous in nature, meaning that there is no blocking thread waiting for a response. The benefit of using asynchronous communication for HTTP is solving the `C10k problem <http://en.wikipedia.org/wiki/C10k_problem>`_.  

When possible, consider using `HTTP keep alive <http://en.wikipedia.org/wiki/Keepalive#HTTP_Keepalive>`_  when connecting for better performance and try to get your favorite client not to do `HTTP chunking <http://en.wikipedia.org/wiki/Chunked_transfer_encoding>`_.  

================================  ======================================================================================
 Setting                           Description                                                                          
================================  ======================================================================================
**websocket.port**                  A bind port range. Defaults to **9400-9499**.                                         
**websocket.max_content_length**    The max content of an HTTP request. Defaults to **100mb**                             
**websocket.compression**           Support for compression when possible (with Accept-Encoding). Defaults to **false**.  
**websocket.compression_level**     Defines the compression level to use. Defaults to **6**.                              
================================  ======================================================================================

Node level network settings allows to set common settings that will be shared among all network based modules (unless explicitly overridden in each module).


The **network.bind_host** setting allows to control the host different network components will bind on. By default, the bind host will be **anyLocalAddress** (typically **0.0.0.0** or **::0**).


The **network.publish_host** setting allows to control the host the node will publish itself within the cluster so other nodes will be able to connect to it. Of course, this can't be the **anyLocalAddress**, and by default, it will be the first non loopback address (if possible), or the local address.


The **network.host** setting is a simple setting to automatically set both **network.bind_host** and **network.publish_host** to the same host value.


Both settings allows to be configured with either explicit host address or host name. The settings also accept logical setting values explained in the following table:


===============================  =============================================================================================
 Logical Host Setting Value       Description                                                                                 
===============================  =============================================================================================
**_local_**                      Will be resolved to the local ip address.                                                    
**_non_loopback_**               The first non loopback address.                                                              
**_non_loopback:ipv4_**          The first non loopback IPv4 address.                                                         
**_non_loopback:ipv6_**          The first non loopback IPv6 address.                                                         
**_[networkInterface]_**         Resolves to the ip address of the provided network interface. For example **_en0_**.         
**_[networkInterface]:ipv4_**    Resolves to the ipv4 address of the provided network interface. For example **_en0:ipv4_**.  
**_[networkInterface]:ipv6_**    Resolves to the ipv6 address of the provided network interface. For example **_en0:ipv6_**.  
===============================  =============================================================================================


TCP Settings
============

=====================================  ==================================================================================================
 Setting                                Description                                                                                      
=====================================  ==================================================================================================
**network.tcp.no_delay**               Enable or disable tcp no delay setting. Defaults to **true**.                                     
**network.tcp.keep_alive**             Enable or disable tcp keep alive. By default not explicitly set.                                  
**network.tcp.reuse_address**          Should an address be reused or not. Defaults to **true** on none windows machines.                
**network.tcp.send_buffer_size**       The size of the tcp send buffer size (in size setting format). By default not explicitly set.     
**network.tcp.receive_buffer_size**    The size of the tcp receive buffer size (in size setting format). By default not explicitly set.  
=====================================  ==================================================================================================

Disable WebSocket
=================

The websocket module can be completely disabled and not started by setting **websocket.enabled** to **false**.

