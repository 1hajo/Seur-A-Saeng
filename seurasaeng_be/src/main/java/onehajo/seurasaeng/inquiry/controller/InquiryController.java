package onehajo.seurasaeng.inquiry.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onehajo.seurasaeng.entity.Inquiry;
import onehajo.seurasaeng.inquiry.dto.AnswerReqDTO;
import onehajo.seurasaeng.inquiry.dto.InquiryDetailResDTO;
import onehajo.seurasaeng.inquiry.dto.InquiryReqDTO;
import onehajo.seurasaeng.inquiry.dto.InquiryResDTO;
import onehajo.seurasaeng.inquiry.service.InquiryService;
import onehajo.seurasaeng.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {
    private final InquiryService inquiryService;
    private final JwtUtil jwtUtil;

    // 문의 생성
    @PostMapping
    public ResponseEntity<?> createInquiry(
            @RequestHeader("Authorization") String authHeader, @RequestBody InquiryReqDTO inquiryReqDTO) {
            String token = authHeader.replace("Bearer ", "");
            Long user_id = jwtUtil.getIdFromToken(token);

            InquiryDetailResDTO response = inquiryService.createInquiry(user_id, inquiryReqDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 사용자 별 문의 목록 조회
    @GetMapping
    public ResponseEntity<?> getInquiriesByUserId(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long user_id = jwtUtil.getIdFromToken(token);

        List<InquiryResDTO> inquiryList = inquiryService.getInquiriesByUserId(user_id);

        return ResponseEntity.ok(inquiryList);
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getInquiriesByManagerId(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long manager_id = jwtUtil.getIdFromToken(token);

        List<InquiryResDTO> inquiryList = inquiryService.getInquiriesByManagerId(manager_id);

        return ResponseEntity.ok(inquiryList);
    }

    // 문의 상세 조회 - 사용자, 관리자
    @GetMapping("/{id}")
    public ResponseEntity<?> getInquiryDetail(
            @RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        String token = authHeader.replace("Bearer ", "");
        Long user_id = jwtUtil.getIdFromToken(token);
        
        /* TODO : ROLE 확인 -> 사용자 : 본인이 작성한 문의만 조회 가능, 관리자 : 모든 문의 조회 가능
        * ROLE = USER -> inquiryService.getInquiryDetail(id, user_id);
        * ROLE = ADMIN -> inquiryService.getAdminInquiryDetail(id, manager_id);
        * */

        InquiryDetailResDTO inquiry = inquiryService.getInquiryDetail(id, user_id);

        return ResponseEntity.ok(inquiry);
    }
    
    // 문의 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInquiryById(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long user_id = jwtUtil.getIdFromToken(token);

        inquiryService.deleteInquiryById(id, user_id);

        Map<String, String> response = Map.of("message", "문의를 삭제했습니다.");

        return ResponseEntity.ok(response);
    }

    // 답변 작성
    @PostMapping("/{id}/answer")
    public ResponseEntity<?> createAnswer(
            @PathVariable("id") Long id, @RequestHeader("Authorization") String authHeader, @RequestBody AnswerReqDTO request) {
        String token = authHeader.replace("Bearer ", "");
        Long manager_id = jwtUtil.getIdFromToken(token);

        // TODO : 관리자 ROLE 확인 (관리자 아닐 경우 Exception)

        InquiryDetailResDTO response = inquiryService.saveAnswer(id, manager_id, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
