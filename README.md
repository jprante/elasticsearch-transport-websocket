# Elasticsearch WebSocket transport plugin

This is an implementation of WebSockets for Elasticsearch.

WebSockets are implemented as an [Elasticsearch transport plugin](http://www.elasticsearch.org/guide/reference/modules/plugins.html) 
using the latest implementation of WebSockets in the [Netty project](http://netty.io).

The [WebSocket protocol specification](http://tools.ietf.org/html/rfc6455) defines an API that 
enables web clients to use the WebSockets protocol for two-way communication with a remote host. 
It  defines a full-duplex communication channel that operates through a single socket over the Web. 
WebSockets provide an enormous reduction in unnecessary network traffic and latency compared to the 
unscalable polling and long-polling solutions that were used to simulate a full-duplex connection by 
maintaining two connections. WebSocket-based applications place less burden on servers, allowing 
existing machines to support more concurrent connections.

Elasticsearch offers a HTTP REST API for nearly all the features available, so using it via `curl` 
or via script languages is comparable to a HTTP client connecting to a HTTP server. Some limitations 
apply when using the REST API and WebSockets come to the rescue.

Motivations for implementing an Elasticsearch WebSocket transport layer are

- to supersede the HTTP request/response model by a full-duplex communication channel

- to implement scalable and responsive real-time apps, like distributed publish/subscribe services 

- to attach thousands of clients to an Elasticsearch node without service degradation

- to allow sequences of bulk index and delete operations on a single connection

- to implement new types of streaming applications like subscribing to change streams from ElasticSearch indexes


## Versions

| Elasticsearch version    | Plugin      | Release date |
| ------------------------ | ----------- | -------------|
| 1.2.1                    | 1.2.1.0     | Jun  8, 2014 |

## Installation

```
./bin/plugin -install websocket -url http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-websocket/1.2.1.0/elasticsearch-websocket-1.2.1.0.zip
```

Do not forget to restart the node after installing.

## Checksum

| File                                          | SHA1                                     |
| --------------------------------------------- | -----------------------------------------|
| elasticsearch-websocket-1.2.1.0.zip           | e31d348968a99ba89ddc2c4647207329427be862 |

## Project docs

The Maven project site is available at [Github](http://jprante.github.io/elasticsearch-websocket)

## Issues

All feedback is welcome! If you find issues, please post them at [Github](https://github.com/jprante/elasticsearch-websocket/issues)

Overview
--------

![Modules](https://github.com/jprante/elasticsearch-transport-websocket/blob/master/src/site/resources/transport-modules.png?raw=true)

The transport plugin uses Netty WebSockets for server and clients. WebSocket clients can connect to an 
Elasticsearch Node with the transport plugin installed. Between nodes, WebSockets connections may establish 
when needed for forwarding messages. The more nodes are installed with websocket transport, 
the more clients can get connected.

## WebSocket Module

![Websocket](https://github.com/jprante/elasticsearch-transport-websocket/blob/master/src/site/resources/elasticsearch-websocket.png?raw=true)

The WebSocket module includes a module that allows to expose the Elasticsearch API over HTTP. It is superseding the 
standard HTTP module on port 9200-9299.

The http mechanism is completely asynchronous in nature, meaning that there is no blocking thread waiting for a 
response. The benefit of using asynchronous communication for HTTP is solving the 
[C10k problem](http://en.wikipedia.org/wiki/C10k_problem).

When possible, consider using [HTTP keep alive](http://en.wikipedia.org/wiki/Keepalive#HTTP_Keepalive)  
when connecting for better performance and try to get your favorite client not to do 
[HTTP chunking](http://en.wikipedia.org/wiki/Chunked_transfer_encoding).  

| Setting                          | Description                                                                            |
| -------------------------------- | -------------------------------------------------------------------------------------- |
| **websocket.port**               | A bind port range. Defaults to **9400-9499**.                                          |
| **websocket.max_content_length** | The max content of an HTTP request. Defaults to **100mb**                              |
| **websocket.compression**        | Support for compression when possible (with Accept-Encoding). Defaults to **false**.   |
|  **websocket.compression_level** | Defines the compression level to use. Defaults to **6**.                               |

Node level network settings allows to set common settings that will be shared among all network based modules (unless explicitly overridden in each module).


The **network.bind_host** setting allows to control the host different network components will bind on. By default, the bind host will be **anyLocalAddress** (typically **0.0.0.0** or **::0**).


The **network.publish_host** setting allows to control the host the node will publish itself within the cluster so other nodes will be able to connect to it. Of course, this can't be the **anyLocalAddress**, and by default, it will be the first non loopback address (if possible), or the local address.


The **network.host** setting is a simple setting to automatically set both **network.bind_host** and **network.publish_host** to the same host value.


Both settings allows to be configured with either explicit host address or host name. The settings also accept logical setting values explained in the following table:


| Logical Host Setting Value       | Description                                                                                |
| -------------------------------- | ------------------------------------------------------------------------------------------ |
| **_local_**                      | Will be resolved to the local ip address.                                                  |   
| **_non_loopback_**               | The first non loopback address.                                                            |   
| **_non_loopback:ipv4_**          | The first non loopback IPv4 address.                                                       |   
| **_non_loopback:ipv6_**          | The first non loopback IPv6 address.                                                       |   
| **_[networkInterface]_**         | Resolves to the ip address of the provided network interface. For example **_en0_**.       |   
| **_[networkInterface]:ipv4_**    | Resolves to the ipv4 address of the provided network interface. For example **_en0:ipv4_**.|   
| **_[networkInterface]:ipv6_**    | Resolves to the ipv6 address of the provided network interface. For example **_en0:ipv6_**.|   


## TCP Settings

| Setting                              | Description                                                                                      |
| ------------------------------------ | ------------------------------------------------------------------------------------------------ |
| **network.tcp.no_delay**             | Enable or disable tcp no delay setting. Defaults to **true**.                                    |       
| **network.tcp.keep_alive**           | Enable or disable tcp keep alive. By default not explicitly set.                                 | 
| **network.tcp.reuse_address**        | Should an address be reused or not. Defaults to **true** on none windows machines.               | 
| **network.tcp.send_buffer_size**     | The size of the tcp send buffer size (in size setting format). By default not explicitly set.    | 
| **network.tcp.receive_buffer_size**  | The size of the tcp receive buffer size (in size setting format). By default not explicitly set. | 

## Disable WebSocket

The websocket module can be completely disabled and not started by setting **websocket.enabled** to **false**.

# License

Elasticsearch Websocket Transport Plugin

Copyright (C) 2012 Jörg Prante

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations