package onehajo.seurasaeng.Inquiry;

import com.fasterxml.jackson.databind.ObjectMapper;
import onehajo.seurasaeng.entity.Inquiry;
import onehajo.seurasaeng.entity.Manager;
import onehajo.seurasaeng.entity.Shuttle;
import onehajo.seurasaeng.entity.User;
import onehajo.seurasaeng.inquiry.dto.AnswerReqDTO;
import onehajo.seurasaeng.inquiry.dto.InquiryReqDTO;
import onehajo.seurasaeng.inquiry.repository.AnswerRepository;
import onehajo.seurasaeng.inquiry.repository.InquiryRepository;
import onehajo.seurasaeng.inquiry.repository.ManagerRepository;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import onehajo.seurasaeng.user.repository.UserRepository;
import onehajo.seurasaeng.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("문의 통합 테스트")
public class InquiryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ShuttleRepository shuttleRepository;

    private User testUser;
    private Manager testManager;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        testUser = createTestUser("테스트사용자", "test@example.com");

        testManager = Manager.builder()
                .email("admin@example.com")
                .password("adminpassword")
                .build();
        testManager = managerRepository.save(testManager);

        userToken = "Bearer " + jwtUtil.generateToken(
                testUser.getId(),
                testUser.getName(),
                testUser.getEmail(),
                "user"
        );
        adminToken = "Bearer " + jwtUtil.generateTokenAdmin(
                testManager.getId(),
                testManager.getEmail(),
                "admin"
        );
    }

    @Test
    @DisplayName("문의 작성 성공")
    void createInquiry_Success() throws Exception {
        InquiryReqDTO requestDto = InquiryReqDTO.builder()
                .title("문의 제목")
                .content("문의 내용입니다.")
                .build();

        mockMvc.perform(post("/api/inquiries")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inquiry_id").exists())
                .andExpect(jsonPath("$.user_name").value("테스트사용자"))
                .andExpect(jsonPath("$.title").value("문의 제목"))
                .andExpect(jsonPath("$.content").value("문의 내용입니다."))
                .andExpect(jsonPath("$.answer_status").value(false))
                .andExpect(jsonPath("$.answer").doesNotExist());
    }

    @Test
    @DisplayName("사용자별 문의 목록 조회 성공")
    void getInquiriesByUserId_Success() throws Exception {
        Inquiry inquiry1 = createTestInquiry("첫 번째 문의", "첫 번째 내용");
        Inquiry inquiry2 = createTestInquiry("두 번째 문의", "두 번째 내용");

        mockMvc.perform(get("/api/inquiries")
                        .header("Authorization", userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("두 번째 문의"))
                .andExpect(jsonPath("$[1].title").value("첫 번째 문의"));
    }

    @Test
    @DisplayName("관리자 문의 목록 조회 성공")
    void getInquiriesByManagerId_Success() throws Exception {
        createTestInquiry("관리자 확인용 문의", "관리자가 볼 내용");

        mockMvc.perform(get("/api/inquiries/admin")
                        .header("Authorization", adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("관리자 확인용 문의"));
    }

    @Test
    @DisplayName("관리자가 아닌 사용자의 관리자 문의 목록 조회 실패")
    void getInquiriesByManagerId_Forbidden() throws Exception {
        mockMvc.perform(get("/api/inquiries/admin")
                        .header("Authorization", userToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("관리자가 아닙니다"));
    }

    @Test
    @DisplayName("문의 상세 조회 성공 - 사용자")
    void getInquiryDetail_User_Success() throws Exception {
        Inquiry inquiry = createTestInquiry("상세 조회 테스트", "상세 내용");

        mockMvc.perform(get("/api/inquiries/{id}", inquiry.getId())
                        .header("Authorization", userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inquiry_id").value(inquiry.getId()))
                .andExpect(jsonPath("$.title").value("상세 조회 테스트"))
                .andExpect(jsonPath("$.content").value("상세 내용"))
                .andExpect(jsonPath("$.answer_status").value(false));
    }

    @Test
    @DisplayName("문의 상세 조회 성공 - 관리자")
    void getInquiryDetail_Admin_Success() throws Exception {
        Inquiry inquiry = createTestInquiry("관리자 조회 테스트", "관리자용 내용");

        mockMvc.perform(get("/api/inquiries/{id}", inquiry.getId())
                        .header("Authorization", adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inquiry_id").value(inquiry.getId()))
                .andExpect(jsonPath("$.title").value("관리자 조회 테스트"));
    }

    @Test
    @DisplayName("다른 사용자의 문의 상세 조회 실패")
    void getInquiryDetail_UnauthorizedAccess() throws Exception {
        User anotherUser = createTestUser("다른사용자", "another@example.com");

        Inquiry inquiry = Inquiry.builder()
                .user(anotherUser)
                .title("다른 사용자 문의")
                .content("접근 불가 내용")
                .created_at(LocalDateTime.now())
                .answer_status(false)
                .build();
        inquiry = inquiryRepository.save(inquiry);

        mockMvc.perform(get("/api/inquiries/{id}", inquiry.getId())
                        .header("Authorization", userToken))
                .andDo(print())
                .andExpect(status().isForbidden()); // UnauthorizedAccessException 처리 확인
    }

    @Test
    @DisplayName("문의 삭제 성공")
    void deleteInquiry_Success() throws Exception {
        Inquiry inquiry = createTestInquiry("삭제할 문의", "삭제될 내용");

        mockMvc.perform(delete("/api/inquiries/{id}", inquiry.getId())
                        .header("Authorization", userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("문의를 삭제했습니다."));

        assert inquiryRepository.findById(inquiry.getId()).isEmpty();
    }

    @Test
    @DisplayName("답변 작성 성공")
    void createAnswer_Success() throws Exception {
        Inquiry inquiry = createTestInquiry("답변 대상 문의", "답변을 기다리는 내용");
        AnswerReqDTO answerRequest = AnswerReqDTO.builder()
                .content("문의에 대한 답변입니다.")
                .build();

        mockMvc.perform(post("/api/inquiries/{id}/answer", inquiry.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answer_status").value(true))
                .andExpect(jsonPath("$.answer.answer_content").value("문의에 대한 답변입니다."));

        Inquiry updatedInquiry = inquiryRepository.findById(inquiry.getId()).orElseThrow();
        assert updatedInquiry.isAnswer_status();
    }

    @Test
    @DisplayName("관리자가 아닌 사용자의 답변 작성 실패")
    void createAnswer_Forbidden() throws Exception {
        Inquiry inquiry = createTestInquiry("답변 불가 문의", "답변 불가 내용");
        AnswerReqDTO answerRequest = AnswerReqDTO.builder()
                .content("권한 없는 답변")
                .build();

        mockMvc.perform(post("/api/inquiries/{id}/answer", inquiry.getId())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("관리자가 아닙니다"));
    }

    @Test
    @DisplayName("존재하지 않는 문의에 대한 답변 작성 실패")
    void createAnswer_InquiryNotFound() throws Exception {
        Long nonExistentInquiryId = 999L;
        AnswerReqDTO answerRequest = AnswerReqDTO.builder()
                .content("존재하지 않는 문의에 대한 답변")
                .build();

        mockMvc.perform(post("/api/inquiries/{id}/answer", nonExistentInquiryId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerRequest)))
                .andDo(print())
                .andExpect(status().isNotFound()); 
    }

    private User createTestUser(String name, String email) {
        Shuttle workShuttle = shuttleRepository.findById(4L).orElse(createDefaultShuttle());
        Shuttle homeShuttle = shuttleRepository.findById(9L).orElse(createDefaultShuttle());

        User user = User.builder()
                .name(name)
                .email(email)
                .password("password123")
                .read_newnoti(false)
                .favorites_work_id(workShuttle)
                .favorites_home_id(homeShuttle)
                .build();

        return userRepository.save(user);
    }

    private Shuttle createDefaultShuttle() {
        return Shuttle.builder()
                .shuttleName("기본 셔틀")
                .isCommute(true)
                .build();
    }

    private Inquiry createTestInquiry(String title, String content) {
        Inquiry inquiry = Inquiry.builder()
                .user(testUser)
                .title(title)
                .content(content)
                .created_at(LocalDateTime.now())
                .answer_status(false)
                .build();
        return inquiryRepository.save(inquiry);
    }
}