package Diplo10.repository;

import Diplo10.entity.Friendrequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendrequestRepository extends JpaRepository <Friendrequest, Integer>{
    List<Friendrequest> findByRequesterPlayer (String username);
    List<Friendrequest> findByRecieverPlayer (String username);

    Friendrequest findById (int id);
}