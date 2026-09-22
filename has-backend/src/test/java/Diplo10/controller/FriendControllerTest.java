package Diplo10.controller;

import Diplo10.entity.Friendrequest;
import Diplo10.entity.Player;
import Diplo10.service.FriendService;
import Diplo10.service.FriendrequestService;
import Diplo10.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every friend action is performed as the session user. Each request below
 * also names someone else ("mallory"), the way an attacker would try to act in
 * another player's name, and the tests check that name is ignored.
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
    @DisplayName("a friend request is sent in the session user's name")
    void requestIsSentAsTheSessionUser() throws Exception {
        Friendrequest saved = new Friendrequest(1L, "alice", "bob");
        when(friendrequestService.sendRequest("alice", "bob")).thenReturn(Optional.of(saved));

        mockMvc.perform(post("/api/friend").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requester\":\"mallory\",\"receiver\":\"bob\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUEST_SENT"));

        verify(friendrequestService).sendRequest("alice", "bob");
        verify(friendrequestService, never()).sendRequest(eq("mallory"), any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a request can only be accepted by the session user")
    void requestIsAcceptedAsTheSessionUser() throws Exception {
        mockMvc.perform(post("/api/friend/acceptRequest").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requester\":\"bob\",\"accepter\":\"mallory\"}"))
                .andExpect(status().isOk());

        verify(friendService).acceptFriendRequest("bob", "alice");
        verify(friendService, never()).acceptFriendRequest(any(), eq("mallory"));
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a request can only be refused by the session user")
    void requestIsRefusedAsTheSessionUser() throws Exception {
        mockMvc.perform(post("/api/friend/refuseRequest").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requester\":\"bob\",\"refuser\":\"mallory\"}"))
                .andExpect(status().isOk());

        verify(friendService).refuseFriendRequest("bob", "alice");
        verify(friendService, never()).refuseFriendRequest(any(), eq("mallory"));
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("a friendship is deleted from the session user's side")
    void friendIsDeletedAsTheSessionUser() throws Exception {
        mockMvc.perform(delete("/api/friend").with(csrf())
                        .param("username", "mallory")
                        .param("friend", "bob"))
                .andExpect(status().isOk());

        verify(friendService).deleteFriend("alice", "bob");
        verify(friendService, never()).deleteFriend(eq("mallory"), any());
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("only the session user's incoming requests are listed")
    void onlyOwnRequestsAreListed() throws Exception {
        when(friendrequestService.getAllFriendRequests("alice"))
                .thenReturn(List.of(new Friendrequest(1L, "bob", "alice")));
        Player bob = new Player("bob", "secret");
        bob.setPoints(300);
        when(playerService.findByPlayer("bob")).thenReturn(bob);

        mockMvc.perform(get("/api/friend/requests").param("username", "mallory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].points").value(300));

        verify(friendrequestService, never()).getAllFriendRequests("mallory");
    }

    @Test
    @WithMockUser(username = "alice")
    @DisplayName("only the session user's friends are listed, by username and points only")
    void onlyOwnFriendsAreListedWithoutSecrets() throws Exception {
        Player bob = new Player("bob", "HASH_bob");
        bob.setPoints(300);
        bob.setVpnPrivateKey("PRIVATE_KEY_bob");
        when(friendService.getFriends("alice")).thenReturn(Set.of(bob));

        mockMvc.perform(get("/api/friend/friends").param("username", "mallory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].points").value(300))
                .andExpect(content().string(not(containsString("HASH_"))))
                .andExpect(content().string(not(containsString("PRIVATE_KEY_"))));

        verify(friendService, never()).getFriends("mallory");
    }
}
