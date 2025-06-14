package onehajo.seurasaeng.newnoti.repository;

import onehajo.seurasaeng.entity.Newnoti;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewnotiRepository extends JpaRepository<Newnoti, Long> {
    Optional<Newnoti> findFirstByOrderByIdAsc();
}
