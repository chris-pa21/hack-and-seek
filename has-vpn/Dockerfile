FROM lscr.io/linuxserver/wireguard:latest

# 1. Install Python and Flask
# --break-system-packages is required in newer Alpine versions for global pip installs
RUN apk add --no-cache python3 py3-pip && \
    pip install --no-cache-dir flask --break-system-packages

# 2. Setup the application directory
WORKDIR /app
COPY vpn-manager.py .

# 3. Create a service for s6-overlay to manage the Flask app
# This ensures it starts automatically and restarts if it crashes
RUN mkdir -p /etc/services.d/vpn-manager && \
    echo "#!/usr/bin/with-contenv sh" > /etc/services.d/vpn-manager/run && \
    echo "exec python3 /app/vpn-manager.py" >> /etc/services.d/vpn-manager/run && \
    chmod +x /etc/services.d/vpn-manager/run

# WireGuard UDP and Flask API TCP
