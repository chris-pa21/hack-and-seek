package Diplo10.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Controllers return DTOs, but a Player that is serialized by accident must
 * still not carry its password hash or VPN data.
 */
class PlayerJsonTest {

    @Test
    @DisplayName("a serialized player contains no password hash or VPN data")
    void serializedPlayerCarriesNoSecrets() throws Exception {
        Player alice = new Player("alice", "HASH_alice");
        alice.setVpnIp("10.8.19.2");
        alice.setVpnPrivateKey("PRIVATE_KEY_alice");
        alice.setVpnPublicKey("PUBLIC_KEY_alice");

        String json = new ObjectMapper().writeValueAsString(alice);

        assertThat(json)
                .contains("\"player\":\"alice\"")
                .doesNotContain("HASH_alice", "PRIVATE_KEY_alice", "PUBLIC_KEY_alice",
                        "10.8.19.2", "password", "vpn");
    }
}
