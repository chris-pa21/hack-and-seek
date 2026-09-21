package Diplo10.service.vpn;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(VpnManagerClient.class)
public class VpnManagerClientTest {

    private static final String BASE_URL = "http://wireguard-server:9999";

    @Autowired
    private VpnManagerClient vpnManagerClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    public void setupVpnBridgePostsTheVpnSubnet() {
        this.server.expect(requestTo(BASE_URL + "/api/vpn/bridge"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"vpnSubnet\":\"10.8.19.0/24\"}"))
                .andRespond(withSuccess());

        vpnManagerClient.setupVpnBridge("10.8.19.0/24");

        this.server.verify();
    }

    @Test
    public void addPlayerToGamePostsPlayerIpAndSubnet() {
        this.server.expect(requestTo(BASE_URL + "/api/game/player"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"playerIp\":\"10.8.19.5\",\"subnet\":\"172.20.0.0/16\"}"))
                .andRespond(withSuccess());

        vpnManagerClient.addPlayerToGame("10.8.19.5", "172.20.0.0/16");

        this.server.verify();
    }

    @Test
    public void removePlayerFromGameDeletesPlayerIpAndSubnet() {
        this.server.expect(requestTo(BASE_URL + "/api/game/player"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().json("{\"playerIp\":\"10.8.19.5\",\"subnet\":\"172.20.0.0/16\"}"))
                .andRespond(withSuccess());

        vpnManagerClient.removePlayerFromGame("10.8.19.5", "172.20.0.0/16");

        this.server.verify();
    }
}
