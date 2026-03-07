package Diplo10.config;

import Diplo10.service.vpn.VpnManagerClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VpnConfig {

    private final VpnManagerClient vpnManagerClient;
    private final String vpnSubnet;

    public VpnConfig(VpnManagerClient vpnManagerClient, 
                     @Value("${vpn.subnet:10.8.19.0/24}") String vpnSubnet) {
        this.vpnManagerClient = vpnManagerClient;
        this.vpnSubnet = vpnSubnet;
    }

    @PostConstruct
    public void init() {
        System.out.println("Configuring VPN Bridge for subnet: " + vpnSubnet);
        try {
            vpnManagerClient.setupVpnBridge(vpnSubnet);
        } catch (Exception e) {
            System.err.println("Failed to setup VPN bridge: " + e.getMessage());
        }
    }
}
