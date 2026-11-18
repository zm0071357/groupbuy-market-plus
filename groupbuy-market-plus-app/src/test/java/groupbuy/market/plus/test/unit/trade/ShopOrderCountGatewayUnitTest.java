package groupbuy.market.plus.test.unit.trade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import groupbuy.market.plus.infrastructure.gateway.ShopOrderCountGateway;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.OptionalLong;

import static org.junit.Assert.*;

/** 验证商城查询契约、超时与不可信响应的处理。 */
public class ShopOrderCountGatewayUnitTest {
    private HttpServer server;
    private int status = 200;
    private String response = "{\"orderCount\":5}";
    private volatile String query;
    private volatile String authorization;
    private volatile long delayMillis;

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/orders", exchange -> {
            try {
                query = exchange.getRequestURI().getRawQuery();
                authorization = exchange.getRequestHeaders().getFirst("Authorization");
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    @Test
    public void passesShopUserAndOrderToExclude() {
        ShopOrderCountGateway gateway = gateway();
        assertEquals(OptionalLong.of(5), gateway.count("shop a", "buyer+b", "order&6"));
        assertTrue(query.contains("shopId=shop%20a"));
        assertTrue(query.contains("userId=buyer%2Bb"));
        assertTrue(query.contains("excludeOutTradeNo=order%266"));
        assertEquals("Bearer test-token", authorization);

        assertTrue(gateway.count("shop-a", "buyer", null).isPresent());
        assertFalse(query.contains("excludeOutTradeNo"));
    }

    @Test
    public void rejectsMalformedAndFailedResponses() {
        ShopOrderCountGateway gateway = gateway();
        for (String value : new String[]{"-1", "5.5", "\"5\"", "null", "9223372036854775808"}) {
            response = "{\"orderCount\":" + value + "}";
            assertTrue(gateway.count("shop", "buyer", null).isEmpty());
        }
        status = 503;
        response = "{\"orderCount\":5}";
        assertTrue(gateway.count("shop", "buyer", null).isEmpty());
        assertTrue(new ShopOrderCountGateway(new OkHttpClient(), new ObjectMapper(), "", "")
                .count("shop", "buyer", null).isEmpty());
    }

    @Test
    public void timesOutInsteadOfReturningAnAssumedCustomerRole() {
        delayMillis = 3000;
        assertTrue(gateway().count("shop", "buyer", null).isEmpty());
    }

    private ShopOrderCountGateway gateway() {
        return new ShopOrderCountGateway(new OkHttpClient(), new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/orders", "test-token");
    }
}
