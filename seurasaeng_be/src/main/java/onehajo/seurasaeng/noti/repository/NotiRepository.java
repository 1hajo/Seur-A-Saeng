package onehajo.seurasaeng.noti.repository;

import onehajo.seurasaeng.entity.Noti;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotiRepository extends JpaRepository<Noti, Long> {
}
