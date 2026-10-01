from flask import Flask, request, jsonify
import hmac
import ipaddress
import os
import secrets
import subprocess
import sys

app = Flask(__name__)

# Shared secret with the backend, the only legitimate caller. Challenge
# containers can reach this port over the game networks, so every request
# must prove it comes from the backend.
TOKEN = os.environ.get("VPN_MANAGER_TOKEN", "")

def is_placeholder(value):
    """True for a value still in env-example form, such as "[TOKEN]"."""
    value = value.strip()
    return value.startswith("[") and value.endswith("]")

# an unreplaced placeholder is public knowledge, so treat it as no token at all
if is_placeholder(TOKEN):
    TOKEN = ""

# Players get addresses from the WireGuard subnet; the server itself holds the
# first one. Game networks are /24s that NetworkService allocates in 10.200.x.
VPN_SUBNET = ipaddress.IPv4Network(os.environ.get("INTERNAL_SUBNET", "10.8.19.0/24"))
VPN_SERVER_IP = VPN_SUBNET.network_address + 1
GAME_RANGE = ipaddress.IPv4Network("10.200.0.0/16")

# New on every start of the manager, so on every start of the WireGuard
# container. The backend compares it to notice that peers and firewall rules
# were wiped and must be restored.
INSTANCE_ID = secrets.token_hex(8)

def run_cmd(args, quiet=False):
    """Runs a command safely using a list of arguments. quiet=True does not log a
    failure, for commands where failing is an expected answer, like iptables -C."""
    try:
        result = subprocess.run(args, capture_output=True, text=True)
        if result.returncode != 0:
            if not quiet:
                print(f"Command Failed: {' '.join(args)}\nError: {result.stderr}", file=sys.stderr)
            return False
        return True
    except Exception as e:
        print(f"Exception while running command: {e}", file=sys.stderr)
        return False

def ensure_rule(chain, rule):
    """Appends an iptables rule unless it is already there, so repeated calls don't
    pile up duplicates. True if the rule is in place afterwards."""
    if run_cmd(["iptables", "-C", chain] + rule, quiet=True):
        return True
    return run_cmd(["iptables", "-A", chain] + rule)

def init_firewall():
    """Sets up the 'Default Deny' state for the VPN traffic."""
    print("Initializing: Setting up isolated forward policy...")
    run_cmd(["iptables", "-F", "FORWARD"])
    run_cmd(["iptables", "-P", "FORWARD", "DROP"])
    run_cmd(["iptables", "-A", "FORWARD", "-m", "conntrack", "--ctstate", "ESTABLISHED,RELATED", "-j", "ACCEPT"])
    # Masquerade VPN traffic going into Docker networks so containers can route replies back
    ensure_rule("POSTROUTING", ["-t", "nat", "-s", str(VPN_SUBNET), "!", "-o", "wg0", "-j", "MASQUERADE"])
    # VPN clients have no business with this API: drop it on the tunnel interface
    ensure_rule("INPUT", ["-i", "wg0", "-p", "tcp", "--dport", "9999", "-j", "DROP"])

def parse_player_ip(value):
    """A single player address inside the VPN subnet, or None. Ranges like 0.0.0.0/0 are rejected."""
    if not isinstance(value, str):
        return None
    try:
        ip = ipaddress.IPv4Address(value)
    except ValueError:
        return None
    if ip not in VPN_SUBNET or ip in (VPN_SUBNET.network_address, VPN_SUBNET.broadcast_address, VPN_SERVER_IP):
        return None
    return ip

def parse_game_subnet(value):
    """A /24 game network inside 10.200.0.0/16, or None."""
    if not isinstance(value, str):
        return None
    try:
        subnet = ipaddress.IPv4Network(value)
    except ValueError:
        return None
    if subnet.prefixlen != 24 or not subnet.subnet_of(GAME_RANGE):
        return None
    return subnet

@app.before_request
def require_token():
    if request.path == "/health":
        return None
    supplied = request.headers.get("X-Manager-Token", "")
    # an unset TOKEN never matches, so a misconfigured manager fails closed
    if not TOKEN or not hmac.compare_digest(supplied.encode(), TOKEN.encode()):
        return jsonify({"error": "unauthorized"}), 401
    return None

def player_rule_args():
    """Validated (player_ip, subnet) strings from the request body, or None."""
    data = request.get_json(silent=True) or {}
    player_ip = parse_player_ip(data.get('playerIp'))
    subnet = parse_game_subnet(data.get('subnet'))
    if player_ip is None or subnet is None:
        return None
    return str(player_ip), str(subnet)

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "instance": INSTANCE_ID}), 200

@app.route('/api/game/player', methods=['POST'])
def add_player_to_game():
    args = player_rule_args()
    if args is None:
        return jsonify({"error": "Invalid playerIp or subnet"}), 400
    player_ip, subnet = args

    # Allow specific player to reach game subnet and vice-versa. Idempotent: the
    # backend re-sends every grant after a restart, and a duplicate rule would
    # survive the single -D of a later revoke.
    success = True
    success &= ensure_rule("FORWARD", ["-s", player_ip, "-d", subnet, "-j", "ACCEPT"])
    success &= ensure_rule("FORWARD", ["-s", subnet, "-d", player_ip, "-j", "ACCEPT"])

    if success:
        return jsonify({"status": "active", "playerIp": player_ip, "subnet": subnet}), 200
    return jsonify({"error": "Failed to apply player rules"}), 500

@app.route('/api/game/player', methods=['DELETE'])
def remove_player_from_game():
    args = player_rule_args()
    if args is None:
        return jsonify({"error": "Invalid playerIp or subnet"}), 400
    player_ip, subnet = args

    run_cmd(["iptables", "-D", "FORWARD", "-s", player_ip, "-d", subnet, "-j", "ACCEPT"])
    run_cmd(["iptables", "-D", "FORWARD", "-s", subnet, "-d", player_ip, "-j", "ACCEPT"])

    return jsonify({"status": "deleted"}), 200

if __name__ == '__main__':
    if not TOKEN:
        sys.exit("VPN_MANAGER_TOKEN is not set or still the env-example placeholder"
                 " - refusing to start an unauthenticated firewall API")
    init_firewall()
    app.run(host='0.0.0.0', port=9999)
