package onehajo.seurasaeng.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import onehajo.seurasaeng.entity.*;
import onehajo.seurasaeng.qr.exception.DuplicateBoardingException;
import onehajo.seurasaeng.qr.repository.BoardingRepository;
import onehajo.seurasaeng.qr.service.BoardingService;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("탑승 통합 테스트")
public class BoardingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardingService boardingService;

    @Autowired
    private BoardingRepository boardingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShuttleRepository shuttleRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Shuttle testShuttle;
    private String testToken;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Location ID 1: 정부과천청사역, ID 6: 아이티센타워
        // Shuttle ID 1: 정부과천청사 -> 아이티센타워 (출근용)
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
    @DisplayName("탑승 내역 조회 API 테스트")
    void getBoardingRecordsSuccess() throws Exception {
        boardingService.saveBoardingRecord(testUser.getId(), testShuttle.getId());

        mockMvc.perform(get("/api/shuttle/rides")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].departure").value("정부과천청사역"))
                .andExpect(jsonPath("$[0].destination").value("아이티센타워"));
    }

    @Test
    @DisplayName("탑승 인원 조회 API 테스트")
    void getBoardingCountSuccess() throws Exception {
        boardingService.incrementBoardingCount(testShuttle.getId());

        mockMvc.perform(get("/api/shuttle/count/{shuttleId}", testShuttle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @DisplayName("탑승 인원 초기화 API 테스트")
    void resetBoardingCountSuccess() throws Exception {
        boardingService.incrementBoardingCount(testShuttle.getId());

        mockMvc.perform(delete("/api/shuttle/count/{shuttleId}", testShuttle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("탑승인원이 삭제되었습니다."));

        Long count = boardingService.getCurrentBoardingCount(testShuttle.getId());
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("중복 탑승 방지 테스트")
    void preventDuplicateBoarding() {
        boardingService.saveBoardingRecord(testUser.getId(), testShuttle.getId());

        assertThat(boardingRepository.findAll()).hasSize(1);

        assertThrows(
                DuplicateBoardingException.class,
                () -> boardingService.checkDuplicateBoarding(testUser.getId(), testShuttle.getId())
        );
    }

    @Test
    @DisplayName("탑승 기록 저장 및 Redis 인원 증가 테스트")
    void saveBoardingRecordAndIncrementCount() {
        Boarding boarding = boardingService.saveBoardingRecord(testUser.getId(), testShuttle.getId());
        boardingService.incrementBoardingCount(testShuttle.getId());

        assertThat(boarding).isNotNull();
        assertThat(boarding.getUser_id()).isEqualTo(testUser.getId());

        Long count = boardingService.getCurrentBoardingCount(testShuttle.getId());
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("셔틀 라인 테스트 - 양재역")
    void testYangjaeShuttle() throws Exception {
        // 양재역 셔틀 (ID: 2)
        Shuttle yangjaeShuttle = shuttleRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("양재역 셔틀을 찾을 수 없습니다."));

        boardingService.saveBoardingRecord(testUser.getId(), yangjaeShuttle.getId());
        boardingService.incrementBoardingCount(yangjaeShuttle.getId());

        Long count = boardingService.getCurrentBoardingCount(yangjaeShuttle.getId());
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("퇴근 셔틀 테스트")
    void testHomeShuttle() throws Exception {
        // ID: 6, 아이티센타워 -> 정부과천청사역)
        Shuttle homeShuttle = shuttleRepository.findById(6L)
                .orElseThrow(() -> new RuntimeException("퇴근 셔틀을 찾을 수 없습니다."));

        boardingService.saveBoardingRecord(testUser.getId(), homeShuttle.getId());
        boardingService.incrementBoardingCount(homeShuttle.getId());

        Long count = boardingService.getCurrentBoardingCount(homeShuttle.getId());
        assertThat(count).isEqualTo(1L);

        assertThat(homeShuttle.getIsCommute()).isFalse();
    }
}