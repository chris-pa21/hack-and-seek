package Diplo10.service.vpn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class VpnManagerClient {

    /** Header the VPN manager checks on every request (see has-vpn/vpn-manager.py). */
    static final String TOKEN_HEADER = "X-Manager-Token";

    private final RestClient restClient;

    /**
     * @param token shared secret with the VPN manager, from VPN_MANAGER_TOKEN.
     *              There is no default: without it the manager rejects every
     *              call, so the backend refuses to start instead. The same
     *              goes for the unreplaced env-example placeholder "[TOKEN]",
     *              which anyone can read in the repository.
     */
    public VpnManagerClient(RestClient.Builder restClientBuilder,
                            @Value("${vpn.manager.url:http://wireguard-server:9999}") String baseUrl,
                            @Value("${vpn.manager.token}") String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("VPN_MANAGER_TOKEN is not set");
        }
        String trimmed = token.strip();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            throw new IllegalStateException(
                    "VPN_MANAGER_TOKEN is still the env-example placeholder " + trimmed);
        }
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(TOKEN_HEADER, token)
                .build();
    }

    public void addPlayerToGame(String playerIp, String subnet) {
        restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/game/player").build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("playerIp", playerIp, "subnet", subnet))
                .retrieve()
                .toBodilessEntity();
    }

    public void removePlayerFromGame(String playerIp, String subnet) {
        restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder.path("/api/game/player").build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("playerIp", playerIp, "subnet", subnet))
                .retrieve()
                .toBodilessEntity();
    }
}
