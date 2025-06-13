package onehajo.seurasaeng.inquiry.repository;

import onehajo.seurasaeng.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {

    Optional<Manager> findByEmail(String email);

    Optional<Manager> findByIdAndEmail(Long id, String email);

    boolean existsByEmail(String email);
}
