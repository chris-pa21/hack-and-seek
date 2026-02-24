# 1. Block everything by default (run once at startup)
iptables -P FORWARD DROP

# 2. Allow a specific "Game Subnet" to talk to itself
# Replace 10.0.1.0/24 with your dynamic subnet
iptables -A FORWARD -s 10.0.1.0/24 -d 10.0.1.0/24 -j ACCEPT

# 3. Ensure they can't reach other subnets (implicitly handled by DROP)
# but you might want to allow them to reach the internet/gateway
iptables -A FORWARD -s 10.0.1.0/24 -o eth0 -j ACCEPT
