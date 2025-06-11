package onehajo.seurasaeng.noti.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import onehajo.seurasaeng.entity.Manager;
import onehajo.seurasaeng.entity.Newnoti;
import onehajo.seurasaeng.entity.Noti;
import onehajo.seurasaeng.entity.Popup;
import onehajo.seurasaeng.inquiry.repository.ManagerRepository;
import onehajo.seurasaeng.newnoti.repository.NewnotiRepository;
import onehajo.seurasaeng.noti.dto.NotiReqDTO;
import onehajo.seurasaeng.noti.dto.NotiResDTO;
import onehajo.seurasaeng.noti.repository.NotiRepository;
import onehajo.seurasaeng.util.JwtUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotiService {
    private final ManagerRepository managerRepository;
    private final NotiRepository notiRepository;
    private final NewnotiRepository newNotiRepository;
    private final JwtUtil jwtUtil;

    public NotiService(ManagerRepository managerRepository, NotiRepository notiRepository, NewnotiRepository newNotiRepository, JwtUtil jwtUtil) {
        this.managerRepository = managerRepository;
        this.notiRepository = notiRepository;
        this.newNotiRepository = newNotiRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public NotiResDTO write(HttpServletRequest request, NotiReqDTO notiReqDTO) throws Exception {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        log.info("token: {}", token);
        Long id = jwtUtil.getIdFromToken(token);

        Manager manager = managerRepository.findById(id).orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다."));

        Noti noti = Noti.builder()
                .title(notiReqDTO.getTitle())
                .content(notiReqDTO.getContent())
                .manager_id(manager)
                .created_at(LocalDateTime.now())
                .build();

        notiRepository.save(noti);
        notiRepository.flush();

        return NotiResDTO.builder()
                .id(noti.getId())
                .title(noti.getTitle())
                .content(noti.getContent())
                .created_at(noti.getCreated_at())
                .build();
    }

    public NotiResDTO read(Long id) throws Exception {
        Noti noti = notiRepository.findById(id).orElseThrow(() -> new RuntimeException("공지가 존재하지 않습니다."));

        return NotiResDTO.builder()
                .id(noti.getId())
                .title(noti.getTitle())
                .content(noti.getContent())
                .created_at(noti.getCreated_at())
                .build();
    }

    public void delete(Long id) throws Exception {
        Noti noti = notiRepository.findById(id).orElseThrow(() -> new RuntimeException("공지가 존재하지 않습니다."));

        notiRepository.delete(noti);
        notiRepository.flush();
    }

    private NotiResDTO buildNotiResponseWithStatus(Noti noti) {
        return NotiResDTO.builder()
                .id(noti.getId())
                .title(noti.getTitle())
                .content(noti.getContent())
                .created_at(noti.getCreated_at())
                .build();
    }

    public List<NotiResDTO> readAll() throws Exception {
        List<Noti> notiList = notiRepository.findAll();

        return notiList.stream().map(this::buildNotiResponseWithStatus)
                .collect(Collectors.toList());
    }
}
