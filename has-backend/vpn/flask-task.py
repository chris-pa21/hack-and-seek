from flask import Flask, request, jsonify
import subprocess

app = Flask(__name__)

def run_cmd(command):
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"Command Failed: {command}\nError: {result.stderr}")
            return False
        return True
    except Exception as e:
        print(f"Exception: {e}")
        return False

def init_firewall():
    """Sets up the 'Default Deny' state for the VPN traffic."""
    print("Initializing: Setting up isolated forward policy...")
    
    # 1. Flush only the FORWARD chain (where VPN routing happens)
    run_cmd("iptables -F FORWARD")
    
    # 2. Set default policy to DROP. 
    # This kills all cross-subnet talking immediately.
    run_cmd("iptables -P FORWARD DROP")
    
    # 3. Allow established connections (crucial for stability)
    run_cmd("iptables -A FORWARD -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT")

@app.route('/api/game', methods=['POST'])
def add_game_network():
    data = request.get_json()
    if not data or 'subnet' not in data:
        return jsonify({"error": "Payload must include 'subnet' (e.g. 10.8.19.0/24)"}), 400

    name = data.get('name', 'unnamed-game')
    subnet = data.get('subnet')

    # THE MAGIC RULE:
    # Allow traffic ONLY if both Source and Destination are within the same subnet.
    # This prevents Subnet A from ever reaching Subnet B.
    cmd = f"iptables -A FORWARD -s {subnet} -d {subnet} -j ACCEPT"
    
    if run_cmd(cmd):
        return jsonify({
            "status": "active",
            "game": name,
            "allowed_range": subnet,
            "isolation": "Strict (Intra-subnet only)"
        }), 200
    
    return jsonify({"status": "error", "message": "Failed to apply networking rule"}), 500

if __name__ == '__main__':
    init_firewall()
    # Listen on all interfaces so you can hit it from outside the container
    app.run(host='0.0.0.0', port=9999)
