package Diplo10.controller;

import Diplo10.entity.Player;
import Diplo10.service.FriendService;
import Diplo10.service.FriendrequestService;
import Diplo10.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The friend list must expose friends as username and points only, never the
 * Player entity with its password hash and VPN keys.
 */
@WebMvcTest(FriendController.class)
class FriendControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private FriendService friendService;
    @MockitoBean private FriendrequestService friendrequestService;
    @MockitoBean private PlayerService playerService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("friends are listed by username and points only")
    void friendsCarryNoSecrets() throws Exception {
        Player bob = new Player("bob", "HASH_bob");
        bob.setPoints(300);
        bob.setVpnPrivateKey("PRIVATE_KEY_bob");
        when(friendService.getFriends("alice")).thenReturn(Set.of(bob));

        mockMvc.perform(get("/api/friend/friends").param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].points").value(300))
                .andExpect(content().string(not(containsString("HASH_"))))
                .andExpect(content().string(not(containsString("PRIVATE_KEY_"))));
    }
}
