package Diplo10.service.vpn;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(VpnManagerClient.class)
public class VpnManagerClientTest {

    @Autowired
    private VpnManagerClient vpnManagerClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    public void testAddSubnet() {
        this.server.expect(requestTo("/api/game"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().json("{\"subnet\":\"10.8.19.0/24\"}"))
                .andRespond(withSuccess());

        vpnManagerClient.addSubnet("10.8.19.0/24");
        
        this.server.verify();
    }

    @Test
    public void testDeleteSubnet() {
        this.server.expect(requestTo("/api/game"))
                .andExpect(method(org.springframework.http.HttpMethod.DELETE))
                .andExpect(content().json("{\"subnet\":\"10.8.19.0/24\"}"))
                .andRespond(withSuccess());

        vpnManagerClient.deleteSubnet("10.8.19.0/24");

        this.server.verify();
    }
}
