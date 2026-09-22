package Diplo10.controller;

import Diplo10.entity.Player;
import Diplo10.service.PlayerService;
import Diplo10.service.vpn.VpnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for the /api/user authorization fix: key material must
 * never be reachable through a caller-supplied username.
 */
@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PlayerService playerService;
    @MockitoBean private VpnService vpnService;
    @MockitoBean private UserDetailsService userDetailsService;

    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = withVpn(new Player("alice", "secret"), "10.8.19.2", "ALICE_PRIVATE_KEY");
        alice.setPoints(120);

        bob = withVpn(new Player("bob", "secret"), "10.8.19.3", "BOB_PRIVATE_KEY");
        bob.setPoints(300);
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("the public profile never exposes another player's key material")
    void publicProfileHasNoKeyMaterial() throws Exception {
        when(playerService.findByPlayer("bob")).thenReturn(bob);

        mockMvc.perform(get("/api/user").param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.points").value(300))
                .andExpect(jsonPath("$.vpnPrivateKey").doesNotExist())
                .andExpect(jsonPath("$.vpnPublicKey").doesNotExist())
                .andExpect(jsonPath("$.vpnIp").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("BOB_PRIVATE_KEY"))));
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("/me answers for the session user and ignores any username parameter")
    void meIgnoresTheUsernameParameter() throws Exception {
        when(playerService.findByPlayer("alice")).thenReturn(alice);

        mockMvc.perform(get("/api/user/me").param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.vpnIp").value("10.8.19.2"))
                .andExpect(jsonPath("$.vpnPrivateKey").doesNotExist());

        // bob was never looked up, so his data could not have been returned
        verify(playerService, never()).findByPlayer("bob");
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("the VPN config is built for the session user and offered as a download")
    void vpnConfigIsBuiltForTheSessionUser() throws Exception {
        when(playerService.findByPlayer("alice")).thenReturn(alice);
        when(vpnService.generateConfig(alice)).thenReturn("[Interface]\nPrivateKey = ALICE_PRIVATE_KEY\n");

        mockMvc.perform(get("/api/user/me/vpn-config").param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"alice-wg.conf\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ALICE_PRIVATE_KEY")));

        verify(vpnService).generateConfig(alice);
        verify(vpnService, never()).generateConfig(bob);
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("the config is withheld until the player's keys exist")
    void vpnConfigUnavailableWithoutKeys() throws Exception {
        Player keyless = new Player("alice", "secret");
        when(playerService.findByPlayer("alice")).thenReturn(keyless);

        mockMvc.perform(get("/api/user/me/vpn-config"))
                .andExpect(status().isServiceUnavailable());

        verify(vpnService, never()).generateConfig(any());
    }

    private static Player withVpn(Player player, String ip, String privateKey) {
        player.setVpnIp(ip);
        player.setVpnPrivateKey(privateKey);
        player.setVpnPublicKey(privateKey + "_PUB");
        return player;
    }
}
