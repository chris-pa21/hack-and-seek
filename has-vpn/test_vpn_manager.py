"""Tests for the VPN manager's firewall API.

Run with: pytest has-vpn

run_cmd is replaced by a recorder, so no iptables command is executed.
"""
import importlib.util
import os
import pathlib
import subprocess
import sys

import pytest

TOKEN = "test-token"


def load_manager(monkeypatch, token):
    monkeypatch.setenv("VPN_MANAGER_TOKEN", token)
    monkeypatch.setenv("INTERNAL_SUBNET", "10.8.19.0/24")
    # the file name has a dash, so it cannot be imported by name
    path = pathlib.Path(__file__).with_name("vpn-manager.py")
    spec = importlib.util.spec_from_file_location("vpn_manager", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def manager(monkeypatch):
    module = load_manager(monkeypatch, TOKEN)

    module.commands = []

    def record(args):
        module.commands.append(args)
        return True

    monkeypatch.setattr(module, "run_cmd", record)
    return module


@pytest.fixture
def client(manager):
    return manager.app.test_client()


def post_player(client, body, token=TOKEN):
    headers = {"X-Manager-Token": token} if token is not None else {}
    return client.post("/api/game/player", json=body, headers=headers)


VALID = {"playerIp": "10.8.19.5", "subnet": "10.200.7.0/24"}


# ── authentication ──────────────────────────────────────────────────────

def test_request_without_token_is_rejected(client, manager):
    response = post_player(client, VALID, token=None)

    assert response.status_code == 401
    assert manager.commands == []


def test_request_with_wrong_token_is_rejected(client, manager):
    response = post_player(client, VALID, token="guessed")

    assert response.status_code == 401
    assert manager.commands == []


def test_unset_token_rejects_everything(client, manager, monkeypatch):
    # a manager started without a token must not fall back to "no auth"
    monkeypatch.setattr(manager, "TOKEN", "")

    response = post_player(client, VALID, token="")

    assert response.status_code == 401
    assert manager.commands == []


@pytest.mark.parametrize("placeholder", ["[TOKEN]", " [TOKEN] ", "[changeme]"])
def test_env_example_placeholder_counts_as_no_token(monkeypatch, placeholder):
    # "[TOKEN]" is readable by anyone in the repository's env-example
    module = load_manager(monkeypatch, placeholder)
    module.commands = []
    monkeypatch.setattr(module, "run_cmd", lambda args: module.commands.append(args) or True)

    response = post_player(module.app.test_client(), VALID, token=placeholder)

    assert module.TOKEN == ""
    assert response.status_code == 401
    assert module.commands == []


@pytest.mark.parametrize("token", ["", "[TOKEN]"])
def test_manager_refuses_to_start_without_a_real_token(token):
    # exits before init_firewall, so no iptables command is attempted
    path = pathlib.Path(__file__).with_name("vpn-manager.py")
    env = {**os.environ, "VPN_MANAGER_TOKEN": token}

    result = subprocess.run([sys.executable, str(path)], env=env,
                            capture_output=True, text=True, timeout=10)

    assert result.returncode != 0
    assert "VPN_MANAGER_TOKEN" in result.stderr


def test_health_needs_no_token(client):
    assert client.get("/health").status_code == 200


# ── granting and revoking access ────────────────────────────────────────

def test_valid_request_opens_both_directions(client, manager):
    response = post_player(client, VALID)

    assert response.status_code == 200
    assert manager.commands == [
        ["iptables", "-A", "FORWARD", "-s", "10.8.19.5", "-d", "10.200.7.0/24", "-j", "ACCEPT"],
        ["iptables", "-A", "FORWARD", "-s", "10.200.7.0/24", "-d", "10.8.19.5", "-j", "ACCEPT"],
    ]


def test_delete_removes_both_directions(client, manager):
    response = client.delete("/api/game/player", json=VALID, headers={"X-Manager-Token": TOKEN})

    assert response.status_code == 200
    assert manager.commands == [
        ["iptables", "-D", "FORWARD", "-s", "10.8.19.5", "-d", "10.200.7.0/24", "-j", "ACCEPT"],
        ["iptables", "-D", "FORWARD", "-s", "10.200.7.0/24", "-d", "10.8.19.5", "-j", "ACCEPT"],
    ]


# ── validation ──────────────────────────────────────────────────────────

@pytest.mark.parametrize("player_ip", [
    "0.0.0.0/0",       # everyone
    "10.8.19.0/24",    # the whole VPN
    "10.8.19.0",       # network address
    "10.8.19.1",       # the WireGuard server itself
    "10.8.19.255",     # broadcast
    "10.200.7.3",      # a challenge container, not a player
    "192.168.1.10",
    "not-an-ip",
    "",
    None,
    167973637,         # an int is a valid IPv4Address() argument, but not a player IP
])
def test_invalid_player_ip_is_rejected(client, manager, player_ip):
    response = post_player(client, {"playerIp": player_ip, "subnet": "10.200.7.0/24"})

    assert response.status_code == 400
    assert manager.commands == []


@pytest.mark.parametrize("subnet", [
    "10.200.0.0/16",   # every game at once
    "0.0.0.0/0",
    "10.8.19.0/24",    # the VPN itself
    "172.20.0.0/24",   # a Docker network outside the game range
    "10.200.7.0/25",
    "10.200.7.5/24",   # host bits set
    "not-a-subnet",
    None,
])
def test_invalid_subnet_is_rejected(client, manager, subnet):
    response = post_player(client, {"playerIp": "10.8.19.5", "subnet": subnet})

    assert response.status_code == 400
    assert manager.commands == []


def test_missing_body_is_rejected(client, manager):
    response = client.post("/api/game/player", data="nonsense",
                           headers={"X-Manager-Token": TOKEN, "Content-Type": "application/json"})

    assert response.status_code == 400
    assert manager.commands == []


# ── removed routes ──────────────────────────────────────────────────────

@pytest.mark.parametrize("method, path", [
    ("POST", "/api/game"),
    ("DELETE", "/api/game"),
    ("GET", "/api/game"),
    ("POST", "/api/vpn/bridge"),
])
def test_unused_routes_are_gone(client, method, path):
    response = client.open(path, method=method, json={"subnet": "10.200.7.0/24"},
                           headers={"X-Manager-Token": TOKEN})

    assert response.status_code in (404, 405)


# ── firewall setup ──────────────────────────────────────────────────────

def test_firewall_blocks_the_api_on_the_tunnel(manager, monkeypatch):
    # -C (check) fails for every rule, so each one gets appended
    monkeypatch.setattr(manager, "run_cmd", lambda args: manager.commands.append(args) or args[1] != "-C")

    manager.init_firewall()

    assert ["iptables", "-A", "INPUT", "-i", "wg0", "-p", "tcp", "--dport", "9999", "-j", "DROP"] \
        in manager.commands


def test_firewall_rules_are_not_duplicated_on_restart(manager):
    # the recorder answers True to -C: every rule already exists
    manager.init_firewall()

    appended_input = [c for c in manager.commands if c[:3] == ["iptables", "-A", "INPUT"]]
    assert appended_input == []
