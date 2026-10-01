package Diplo10.service.vpn;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(VpnManagerClient.class)
public class VpnManagerClientTest {

    private static final String BASE_URL = "http://wireguard-server:9999";
    // from src/test/resources/application.properties
    private static final String TOKEN = "test-token";

    @Autowired
    private VpnManagerClient vpnManagerClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    public void addPlayerToGamePostsPlayerIpAndSubnetWithTheToken() {
        this.server.expect(requestTo(BASE_URL + "/api/game/player"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(VpnManagerClient.TOKEN_HEADER, TOKEN))
                .andExpect(content().json("{\"playerIp\":\"10.8.19.5\",\"subnet\":\"10.200.7.0/24\"}"))
                .andRespond(withSuccess());

        vpnManagerClient.addPlayerToGame("10.8.19.5", "10.200.7.0/24");

        this.server.verify();
    }

    @Test
    public void removePlayerFromGameDeletesPlayerIpAndSubnetWithTheToken() {
        this.server.expect(requestTo(BASE_URL + "/api/game/player"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(VpnManagerClient.TOKEN_HEADER, TOKEN))
                .andExpect(content().json("{\"playerIp\":\"10.8.19.5\",\"subnet\":\"10.200.7.0/24\"}"))
                .andRespond(withSuccess());

        vpnManagerClient.removePlayerFromGame("10.8.19.5", "10.200.7.0/24");

        this.server.verify();
    }

    @Test
    public void instanceIdIsReadFromHealth() {
        this.server.expect(requestTo(BASE_URL + "/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"ok\",\"instance\":\"3f9a1c0be7d24a65\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(vpnManagerClient.instanceId()).isEqualTo("3f9a1c0be7d24a65");

        this.server.verify();
    }

    @Test
    public void refusesToStartWithoutAToken() {
        assertThatThrownBy(() -> new VpnManagerClient(RestClient.builder(), BASE_URL, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VPN_MANAGER_TOKEN");
    }

    @Test
    public void refusesToStartWithTheEnvExamplePlaceholder() {
        assertThatThrownBy(() -> new VpnManagerClient(RestClient.builder(), BASE_URL, "[TOKEN]"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }
}
