
package org.xbib.elasticsearch.websocket.helper;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.xbib.elasticsearch.action.cluster.admin.websocket.WebsocketInfoAction;
import org.xbib.elasticsearch.action.cluster.admin.websocket.WebsocketInfoRequest;
import org.xbib.elasticsearch.action.cluster.admin.websocket.WebsocketInfoResponse;

import java.net.URI;
import java.util.Map;

import static org.elasticsearch.common.collect.Maps.newHashMap;
import static org.elasticsearch.common.settings.ImmutableSettings.Builder.EMPTY_SETTINGS;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public abstract class AbstractNodeTestHelper {

    private final static ESLogger logger = ESLoggerFactory.getLogger("test");

    protected final String CLUSTER = "test-cluster-" + NetworkUtils.getLocalAddress().getHostName();

    protected final String INDEX = "test-" + NetworkUtils.getLocalAddress().getHostName().toLowerCase();

    protected Settings defaultSettings = ImmutableSettings
            .settingsBuilder()
            .put("cluster.name", CLUSTER)
            .put("threadpool.bulk.queue_size", 200)
            .build();

    private Map<String, Node> nodes = newHashMap();

    private Map<String, Client> clients = newHashMap();

    private Map<String, InetSocketTransportAddress> addresses = newHashMap();

    protected URI getAddressOfNode(String n) {
        InetSocketTransportAddress address = addresses.get(n);
        return URI.create("ws://" + address.address().getHostName() + ":" + address.address().getPort() + "/websocket");
    }

    @Before
    public void setUp() throws Exception {
        startNode("1");
        WebsocketInfoRequest request = new WebsocketInfoRequest("1");
        WebsocketInfoResponse response = client("1").admin().cluster().execute(WebsocketInfoAction.INSTANCE, request).actionGet();
        InetSocketTransportAddress address = response.getAt(0).getAddress();
        addresses.put("1", address);

        logger.info("websocket address = {}", address);

        logger.info("creating index {}", INDEX);
        client("1").admin().indices().create(new CreateIndexRequest(INDEX)).actionGet();
        logger.info("index {} created", INDEX);
    }

    @After
    public void deleteIndices() {
        logger.info("deleting index {}", INDEX);
        try {
            client("1").admin().indices().delete(new DeleteIndexRequest().indices(INDEX)).actionGet();
        } catch (IndexMissingException e) {
            // ignore
        }
        logger.info("index {} deleted", INDEX);
        closeAllNodes();
    }

    public Node startNode(String id) {
        return buildNode(id).start();
    }

    public Node buildNode(String id) {
        return buildNode(id, EMPTY_SETTINGS);
    }

    public Node buildNode(String id, Settings settings) {
        String settingsSource = getClass().getName().replace('.', '/') + ".yml";
        Settings finalSettings = settingsBuilder()
                .loadFromClasspath(settingsSource)
                .put(defaultSettings)
                .put(settings)
                .put("name", id)
                .put("gateway.type", "none")
                .put("cluster.routing.schedule", "50ms")
                .build();
        Node node = nodeBuilder().settings(finalSettings).build();
        Client client = node.client();
        nodes.put(id, node);
        clients.put(id, client);
        return node;
    }

    public Client client(String id) {
        return clients.get(id);
    }

    public void closeAllNodes() {
        for (Client client : clients.values()) {
            client.close();
        }
        clients.clear();
        for (Node node : nodes.values()) {
            if (node != null) {
                node.close();
            }
        }
        nodes.clear();
    }

}
