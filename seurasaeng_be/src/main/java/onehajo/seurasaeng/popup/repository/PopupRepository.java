package onehajo.seurasaeng.popup.repository;

import onehajo.seurasaeng.entity.Popup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PopupRepository extends JpaRepository<Popup, Long> {
    Optional<Popup> findFirstByOrderByIdAsc();
}
