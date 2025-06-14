package onehajo.seurasaeng.popup.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import onehajo.seurasaeng.entity.Noti;
import onehajo.seurasaeng.entity.Popup;
import onehajo.seurasaeng.noti.dto.NotiResDTO;
import onehajo.seurasaeng.noti.repository.NotiRepository;
import onehajo.seurasaeng.popup.repository.PopupRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class PopupService {
    private final NotiRepository notiRepository;
    private final PopupRepository popupRepository;

    public PopupService(NotiRepository notiRepository, PopupRepository popupRepository) {
        this.notiRepository = notiRepository;
        this.popupRepository = popupRepository;
    }

    @Transactional
    public void set(long id) throws Exception {
        Noti noti = notiRepository.findById(id).orElseThrow(() -> new Exception("공지가 존재하지 않습니다."));

        // 기존 popup이 있다면 삭제
        Optional<Popup> existingPopup = popupRepository.findFirstByOrderByIdAsc();
        existingPopup.ifPresent(popup -> popupRepository.deleteById(popup.getId()));

        // 새로운 popup 선정
        Popup popup = Popup.builder()
                .noti_id(noti)
                .build();

        popupRepository.save(popup);
        popupRepository.flush();
    }

    public NotiResDTO read() throws Exception {
        Popup popup = popupRepository.findAll().getFirst();
        Noti noti = notiRepository.findById(popup.getNoti_id().getId()).orElseThrow(() -> new RuntimeException("공지가 존재하지 않습니다."));

        return NotiResDTO.builder()
                .id(noti.getId())
                .title(noti.getTitle())
                .content(noti.getContent())
                .created_at(noti.getCreated_at())
                .build();
    }
}
