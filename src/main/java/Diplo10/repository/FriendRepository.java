package Diplo10.repository;

import Diplo10.entity.Friends;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import static org.aspectj.asm.IProgramElement.Kind.PROJECT;

@Repository
public interface FriendRepository extends JpaRepository<Friends, String> {
    Friends findByUsername(String player);
}
