FROM lscr.io/linuxserver/wireguard:latest

# 1. Install Python and Flask
# --break-system-packages is required in newer Alpine versions for global pip installs
RUN apk add --no-cache python3 py3-pip && \
    pip install --no-cache-dir flask --break-system-packages

# 2. Setup the application directory
WORKDIR /app
COPY vpn_manager.py .

# 3. Create a service for s6-overlay to manage the Flask app
# This ensures it starts automatically and restarts if it crashes
RUN mkdir -p /etc/services.d/vpn-manager && \
    echo "#!/usr/bin/with-contenv sh" > /etc/services.d/vpn-manager/run && \
    echo "exec python3 /app/vpn_manager.py" >> /etc/services.d/vpn-manager/run && \
    chmod +x /etc/services.d/vpn-manager/run

# The WireGuard server normally uses 51820/udp
# Our Flask API will use 9999/tcp
EXPOSE 9999/tcp
