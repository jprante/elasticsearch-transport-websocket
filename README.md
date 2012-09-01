WebSockets for ElasticSearch
============================

The [WebSocket protocol specification](http://tools.ietf.org/html/rfc6455) defines an API that enables web clients to use the WebSockets protocol for two-way communication with a remote host. It  defines a full-duplex communication channel that operates through a single socket over the Web. WebSockets provide an enormous reduction in unnecessary network traffic and latency compared to the unscalable polling and long-polling solutions that were used to simulate a full-duplex connection by maintaining two connections. WebSocket-based applications place less burden on servers, allowing existing machines to support more concurrent connections.

ElasticSearch offers a HTTP REST API for nearly all the features available, so using it via ``curl`` or via script languages is comparable to a HTTP client connecting to a HTTP server. Some limitations apply when using the REST API and WebSockets come to the rescue.

Motivations for implementing an ElasticSearch WebSocket transport layer are

- to supersede the HTTP request/response model by a full-duplex communication channel

- to implement scalable and responsive real-time apps, like distributed publish/subscribe services 

- to attach thousands of clients to an ElasticSearch node without service degradation

- to allow sequences of bulk index and delete operations on a single connection

- to implement new types of streaming applications like subscribing to change streams from ElasticSearch indexes

WebSockets are implemented as an [Elasticsearch transport plugin](http://www.elasticsearch.org/guide/reference/modules/plugins.html) using the latest implementation of WebSockets in the [Netty project](http://netty.io).


Installation
------------

The current version of the plugin is **1.0.0**

In order to install the plugin, please run

 `bin/plugin -install jprante/elasticsearch-transport-websocket/1.0.0`.

In case the version number is omitted, you will have the source code installed for manual compilation.

    ------------------------------------------
    | WebSocket Plugin    | ElasticSearch    |
    ------------------------------------------
    | master              | 0.20.x -> master |
    ------------------------------------------
    | 1.0.0               | 0.20.x           |
    ------------------------------------------

Attention: you need at least *Java 7* to run the WebSocket transport plugin.


Documentation
-------------

The Maven project site is [here](http://jprante.github.com/elasticsearch-transport-websocket)

The Javadoc API can be found [here](http://jprante.github.com/elasticsearch-transport-websocket/apidocs/index.html)

Example of startup
-----------------
	/bin/elasticsearch -f
	[2012-09-01 21:40:32,532][INFO ][node                     ] [Scarecrow II] {0.20.0.Beta1-SNAPSHOT}[6820]: initializing ...
	[2012-09-01 21:40:32,571][INFO ][plugins                  ] [Scarecrow II] loaded [transport-websocket], sites [head]
	[2012-09-01 21:40:35,169][INFO ][node                     ] [Scarecrow II] {0.20.0.Beta1-SNAPSHOT}[6820]: initialized
	[2012-09-01 21:40:35,169][INFO ][node                     ] [Scarecrow II] {0.20.0.Beta1-SNAPSHOT}[6820]: starting ...
	[2012-09-01 21:40:35,214][INFO ][websocket.http           ] [Scarecrow II] bound_address {inet[/0:0:0:0:0:0:0:0:9400]}, publish_address {inet[/192.168.1.113:9400]}
	[2012-09-01 21:40:35,271][INFO ][transport                ] [Scarecrow II] bound_address {inet[/0:0:0:0:0:0:0:0:9300]}, publish_address {inet[/192.168.1.113:9300]}
	[2012-09-01 21:40:38,311][INFO ][cluster.service          ] [Scarecrow II] new_master [Scarecrow II][h_62brvgRIyvCE5mvAPU6A][inet[/192.168.1.113:9300]], reason: zen-disco-join (elected_as_master)
	[2012-09-01 21:40:38,403][INFO ][discovery                ] [Scarecrow II] elasticsearch/h_62brvgRIyvCE5mvAPU6A
	[2012-09-01 21:40:38,419][INFO ][http                     ] [Scarecrow II] bound_address {inet[/0:0:0:0:0:0:0:0:9200]}, publish_address {inet[/192.168.1.113:9200]}
	[2012-09-01 21:40:38,419][INFO ][node                     ] [Scarecrow II] {0.20.0.Beta1-SNAPSHOT}[6820]: started
	[2012-09-01 21:40:39,369][INFO ][gateway                  ] [Scarecrow II] recovered [2] indices into cluster_state
	
Configuration
=============

TODO

Publish/Subscribe
=================

<p align="center">
  <img src="https://github.com/jprante/elasticsearch-transport-websocket/blob/master/src/main/site/images/publish-subscribe.png?raw=true" alt="Publish/Subscribe"/>
</p>

<p align="center">
  <img src="https://github.com/jprante/elasticsearch-transport-websocket/blob/master/src/main/site/images/elasticsearch-websocket.png?raw=true" alt="ElasticSearch WebSocket"/>
</p>



