package Diplo10.service.vpn;

import Diplo10.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class VpnConfigService {
    private Path config;
    private List<String> iptables;
    private List<String> peers;

    @Autowired
    private PlayerRepository repository;

    /*public VpnConfigService(Path config) {
        config = Path.of("wg0.conf");
    }

    public parse() {

    }

    public compile() {

    }*/
}
