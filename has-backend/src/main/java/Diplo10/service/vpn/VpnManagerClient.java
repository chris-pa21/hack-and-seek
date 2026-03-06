package Diplo10.service.vpn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class VpnManagerClient {

    private final RestClient restClient;

    public VpnManagerClient(@Value("${vpn.manager.url:http://wireguard-server:9999}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void addSubnet(String subnet) {
        restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/game").build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("subnet", subnet))
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteSubnet(String subnet) {
        restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder.path("/api/game").build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("subnet", subnet))
                .retrieve()
                .toBodilessEntity();
    }
}
