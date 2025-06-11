package onehajo.seurasaeng.newnoti.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import onehajo.seurasaeng.entity.Newnoti;
import onehajo.seurasaeng.entity.Noti;
import onehajo.seurasaeng.newnoti.repository.NewnotiRepository;
import onehajo.seurasaeng.noti.repository.NotiRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class NewnotiService {

    private final NotiRepository notiRepository;
    private final NewnotiRepository newNotiRepository;

    public NewnotiService(NotiRepository notiRepository, NewnotiRepository newNotiRepository) {
        this.notiRepository = notiRepository;
        this.newNotiRepository = newNotiRepository;
    }


    @Transactional
    public void write(long id) throws Exception {
        Noti noti = notiRepository.findById(id).orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다."));

        // 새로운 공지 등록
        // 기존 new noti이 있다면 삭제
        Optional<Newnoti> existingNewnoti = newNotiRepository.findFirstByOrderByIdAsc();
        existingNewnoti.ifPresent(newnoti -> newNotiRepository.deleteById(newnoti.getId()));

        // 새로운 new noti 선정
        Newnoti newnoti = Newnoti.builder()
                .noti_id(noti)
                .build();

        newNotiRepository.save(newnoti);
        notiRepository.flush();

        log.info("{}:", newnoti.getNoti_id());
    }
}
