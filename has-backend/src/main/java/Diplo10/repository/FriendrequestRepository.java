package Diplo10.repository;

import Diplo10.entity.Friendrequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendrequestRepository extends JpaRepository<Friendrequest, Long> {
    List<Friendrequest> findByRequester(String requester);

    List<Friendrequest> findByReceiver(String receiver);

    Optional<Friendrequest> findById(Long id);

    boolean existsByRequesterAndReceiver(String requester, String receiver);

    Optional<Friendrequest> getFriendrequestByRequesterAndReceiver(String requester, String receiver);
}
