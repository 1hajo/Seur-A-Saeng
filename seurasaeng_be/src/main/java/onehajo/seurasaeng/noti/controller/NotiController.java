package onehajo.seurasaeng.noti.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import onehajo.seurasaeng.noti.dto.NotiReqDTO;
import onehajo.seurasaeng.noti.dto.NotiResDTO;
import onehajo.seurasaeng.noti.service.NotiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notices")
public class NotiController {

    private final NotiService notiService;

    public NotiController(NotiService notiService) {
        this.notiService = notiService;
    }

    @Transactional
    @PostMapping
    public ResponseEntity<?> writeNoti(HttpServletRequest request, @RequestBody NotiReqDTO notiReqDTO) throws Exception {
        NotiResDTO notiResDTO = notiService.write(request, notiReqDTO);

        return ResponseEntity.ok(notiResDTO);
    }

    @GetMapping
    public ResponseEntity<?> searchAllNoti(HttpServletRequest request) throws Exception {
        List<NotiResDTO> notiList = notiService.readAll();
        return ResponseEntity.ok(notiList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> searchNoti(@PathVariable Long id) throws Exception {
        NotiResDTO notiResDTO = notiService.read(id);
        return ResponseEntity.ok(notiResDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNoti(@PathVariable Long id) throws Exception {
        notiService.delete(id);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}