package onehajo.seurasaeng.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import onehajo.seurasaeng.entity.*;
import onehajo.seurasaeng.qr.dto.QrReqDTO;
import onehajo.seurasaeng.qr.repository.QrRepository;
import onehajo.seurasaeng.qr.service.QRService;
import onehajo.seurasaeng.qr.util.AESUtil;
import onehajo.seurasaeng.shuttle.repository.LocationRepository;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import onehajo.seurasaeng.user.repository.UserRepository;
import onehajo.seurasaeng.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("QR 통합 테스트")
public class QRIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QRService qrService;

    @Autowired
    private QrRepository qrRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShuttleRepository shuttleRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Shuttle testShuttle;
    private String testToken;

    @BeforeEach
    void setUp() {
        // Shuttle ID 1: 정부과천청사역 -> 아이티센타워 (출근용)
        testShuttle = shuttleRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("테스트용 셔틀을 찾을 수 없습니다."));

        testUser = userRepository.save(User.builder()
                .name("테스트사용자")
                .email("test@example.com")
                .password("password123")
                .favorites_work_id(testShuttle)
                .favorites_home_id(testShuttle)
                .build());

        testToken = jwtUtil.generateToken(testUser.getId(), testUser.getName(), testUser.getEmail(), "user");
    }

    @Test
    @DisplayName("QR 코드 생성 통합 테스트")
    void generateQRCodeSuccess() throws Exception {
        qrService.generateQRCode(testUser.getId(), testUser.getEmail());

        List<Qr> qrList = qrRepository.findAll();
        assertThat(qrList).hasSize(1);
        assertThat(qrList.get(0).getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("QR 코드 조회 API 테스트")
    void getQRCodeSuccess() throws Exception {
        qrService.generateQRCode(testUser.getId(), testUser.getEmail());

        mockMvc.perform(get("/api/users/me/qr")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qr_code").exists());
    }

    @Test
    @DisplayName("QR 코드 검증 및 탑승 처리 테스트")
    void validateQRCodeAndBoardingSuccess() throws Exception {
        QrReqDTO qrData = new QrReqDTO(testUser.getId(), testUser.getEmail());
        String qrJsonData = objectMapper.writeValueAsString(qrData);
        String encryptedQRCode = aesUtil.encrypt(qrJsonData);

        mockMvc.perform(post("/api/users/me/qr/valid")
                        .param("qrCode", encryptedQRCode)
                        .param("shuttle_id", testShuttle.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("테스트사용자"))
                .andExpect(jsonPath("$.departure").value("정부과천청사역"))
                .andExpect(jsonPath("$.destination").value("아이티센타워"));
    }

    @Test
    @DisplayName("잘못된 QR 코드 검증 테스트")
    void validateInvalidQRCodeFail() throws Exception {
        String invalidQRCode = "invalid_qr_code";

        mockMvc.perform(post("/api/users/me/qr/valid")
                        .param("qrCode", invalidQRCode)
                        .param("shuttle_id", testShuttle.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 QR 검증 테스트")
    void validateNonExistentUserQRCodeFail() throws Exception {
        QrReqDTO qrData = new QrReqDTO(99999L, "nonexistent@example.com");
        String qrJsonData = objectMapper.writeValueAsString(qrData);
        String encryptedQRCode = aesUtil.encrypt(qrJsonData);

        mockMvc.perform(post("/api/users/me/qr/valid")
                        .param("qrCode", encryptedQRCode)
                        .param("shuttle_id", testShuttle.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("QR 검증 테스트 - 양재역")
    void validateQRCodeWithYangjaeShuttle() throws Exception {
        // 양재역 셔틀 (ID: 2) 사용
        Shuttle yangjaeShuttle = shuttleRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("양재역 셔틀을 찾을 수 없습니다."));

        QrReqDTO qrData = new QrReqDTO(testUser.getId(), testUser.getEmail());
        String qrJsonData = objectMapper.writeValueAsString(qrData);
        String encryptedQRCode = aesUtil.encrypt(qrJsonData);

        mockMvc.perform(post("/api/users/me/qr/valid")
                        .param("qrCode", encryptedQRCode)
                        .param("shuttle_id", yangjaeShuttle.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("테스트사용자"))
                .andExpect(jsonPath("$.departure").value("양재역"))
                .andExpect(jsonPath("$.destination").value("아이티센타워"));
    }

    @Test
    @DisplayName("퇴근 셔틀로 QR 검증 테스트")
    void validateQRCodeWithHomeShuttle() throws Exception {
        // ID: 6, 아이티센타워 -> 정부과천청사역
        Shuttle homeShuttle = shuttleRepository.findById(6L)
                .orElseThrow(() -> new RuntimeException("퇴근 셔틀을 찾을 수 없습니다."));

        QrReqDTO qrData = new QrReqDTO(testUser.getId(), testUser.getEmail());
        String qrJsonData = objectMapper.writeValueAsString(qrData);
        String encryptedQRCode = aesUtil.encrypt(qrJsonData);

        mockMvc.perform(post("/api/users/me/qr/valid")
                        .param("qrCode", encryptedQRCode)
                        .param("shuttle_id", homeShuttle.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("테스트사용자"))
                .andExpect(jsonPath("$.departure").value("아이티센타워"))
                .andExpect(jsonPath("$.destination").value("정부과천청사역"));
    }

    @Test
    @DisplayName("존재하지 않는 셔틀로 QR 검증 테스트")
    void validateQRCodeWithNonExistentShuttle() throws Exception {
        QrReqDTO qrData = new QrReqDTO(testUser.getId(), testUser.getEmail());
        String qrJsonData = objectMapper.writeValueAsString(qrData);
        String encryptedQRCode = aesUtil.encrypt(qrJsonData);

        mockMvc.perform(post("/api/users/me/qr/valid")
                        .param("qrCode", encryptedQRCode)
                        .param("shuttle_id", "99999"))
                .andExpect(status().isBadRequest());
    }
}