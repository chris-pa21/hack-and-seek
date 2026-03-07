from flask import Flask, request, jsonify
import subprocess
import ipaddress
import sys

app = Flask(__name__)

def run_cmd(args):
    """Runs a command safely using a list of arguments."""
    try:
        result = subprocess.run(args, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"Command Failed: {' '.join(args)}\nError: {result.stderr}", file=sys.stderr)
            return False
        return True
    except Exception as e:
        print(f"Exception while running command: {e}", file=sys.stderr)
        return False

def init_firewall():
    """Sets up the 'Default Deny' state for the VPN traffic."""
    print("Initializing: Setting up isolated forward policy...")
    run_cmd(["iptables", "-F", "FORWARD"])
    run_cmd(["iptables", "-P", "FORWARD", "DROP"])
    run_cmd(["iptables", "-A", "FORWARD", "-m", "conntrack", "--ctstate", "ESTABLISHED,RELATED", "-j", "ACCEPT"])

def validate_subnet(subnet):
    """Validates that the input is a valid CIDR subnet."""
    try:
        ipaddress.IPv4Network(subnet)
        return True
    except ValueError:
        return False

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"}), 200

@app.route('/api/game', methods=['POST'])
def add_game_network():
    data = request.get_json()
    subnet = data.get('subnet')
    if not subnet or not validate_subnet(subnet):
        return jsonify({"error": "Invalid or missing subnet"}), 400
    if run_cmd(["iptables", "-A", "FORWARD", "-s", subnet, "-d", subnet, "-j", "ACCEPT"]):
        return jsonify({"status": "active", "subnet": subnet}), 200
    
    return jsonify({"error": "Failed to apply firewall rule"}), 500

@app.route('/api/game', methods=['DELETE'])
def delete_game_network():
    data = request.get_json()
    subnet = data.get('subnet')
    if not subnet or not validate_subnet(subnet):
        return jsonify({"error": "Invalid or missing subnet"}), 400
    if run_cmd(["iptables", "-D", "FORWARD", "-s", subnet, "-d", subnet, "-j", "ACCEPT"]):
        return jsonify({
            "status": "deleted", 
            "message": f"Isolation rule for {subnet} removed."
        }), 200
    
    return jsonify({"error": "Rule not found or could not be deleted"}), 404

@app.route('/api/game', methods=['GET'])
def get_game_networks():
    """Returns a list of current FORWARD rules to verify what is active."""
    try:
        result = subprocess.run(["iptables", "-L", "FORWARD", "-n"], capture_output=True, text=True)
        return jsonify({"rules": result.stdout.splitlines()}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/vpn/bridge', methods=['POST'])
def setup_vpn_bridge():
    data = request.get_json()
    vpn_subnet = data.get('vpnSubnet')
    if not vpn_subnet or not validate_subnet(vpn_subnet):
        return jsonify({"error": "Invalid or missing vpnSubnet"}), 400
    
    # Allow traffic from VPN to anyone and anyone back to VPN
    success = True
    success &= run_cmd(["iptables", "-A", "FORWARD", "-s", vpn_subnet, "-j", "ACCEPT"])
    success &= run_cmd(["iptables", "-A", "FORWARD", "-d", vpn_subnet, "-j", "ACCEPT"])
    
    if success:
        return jsonify({"status": "active", "vpnSubnet": vpn_subnet}), 200
    
    return jsonify({"error": "Failed to apply bridge rules"}), 500

if __name__ == '__main__':
    init_firewall()
    # Running on 9999 as per your previous setup
    app.run(host='0.0.0.0', port=9999)
