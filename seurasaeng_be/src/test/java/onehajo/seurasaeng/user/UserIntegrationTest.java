package onehajo.seurasaeng.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import onehajo.seurasaeng.entity.Shuttle;
import onehajo.seurasaeng.entity.User;
import onehajo.seurasaeng.redis.service.RedisTokenService;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import onehajo.seurasaeng.user.dto.*;
import onehajo.seurasaeng.user.repository.UserRepository;
import onehajo.seurasaeng.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("사용자 통합 테스트")
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShuttleRepository shuttleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTokenService redisTokenService;

    private User testUser;
    private Shuttle defaultWorkShuttle;
    private Shuttle defaultHomeShuttle;

    @BeforeEach
    void setUp() {
        defaultWorkShuttle = Shuttle.builder()
                .id(4L)
                .shuttleName("출근용 셔틀")
                .build();

        defaultHomeShuttle = Shuttle.builder()
                .id(9L)
                .shuttleName("퇴근용 셔틀")
                .build();

        if (!shuttleRepository.existsById(4L)) {
            shuttleRepository.save(defaultWorkShuttle);
        }
        if (!shuttleRepository.existsById(9L)) {
            shuttleRepository.save(defaultHomeShuttle);
        }

        testUser = User.builder()
                .name("테스트사용자")
                .email("test@gmail.com")
                .password(passwordEncoder.encode("password123"))
                .favorites_work_id(defaultWorkShuttle)
                .favorites_home_id(defaultHomeShuttle)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void registerUser_Success() throws Exception {
        SignUpReqDTO signUpRequest = new SignUpReqDTO();
        signUpRequest.setName("새사용자");
        signUpRequest.setEmail("newuser@gmail.com");
        signUpRequest.setPassword("password123");
        signUpRequest.setRole("user");

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.name").value("새사용자"))
                .andExpect(jsonPath("$.email").value("newuser@gmail.com"))
                .andExpect(jsonPath("$.role").value("user"));
    }

    @Test
    @DisplayName("이메일 중복 확인 성공 테스트")
    void verifyEmail_Success() throws Exception {
        String email = "unique@gmail.com";

        mockMvc.perform(post("/api/users/verify-email")
                        .param("email", email))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() throws Exception {
        User savedUser = userRepository.save(testUser);

        String existingToken = jwtUtil.generateToken(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                "user"
        );
        redisTokenService.saveToken(savedUser.getId(), existingToken);

        LoginReqDTO loginRequest = createLoginRequest("test@gmail.com", "password123");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.name").value("테스트사용자"))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.role").value("user"));
    }

    @Test
    @DisplayName("마이페이지 조회 성공 테스트")
    void getMyPage_Success() throws Exception {
        User savedUser = userRepository.save(testUser);
        String token = jwtUtil.generateToken(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                "user"
        );

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("테스트사용자"))
                .andExpect(jsonPath("$.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.favorites_work_id").value(4))
                .andExpect(jsonPath("$.favorites_home_id").value(9));
    }

    @Test
    @DisplayName("개인정보 수정 성공 테스트")
    void updateUser_Success() throws Exception {
        User savedUser = userRepository.save(testUser);
        String token = jwtUtil.generateToken(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                "user"
        );

        MyInfoReqDTO updateRequest = MyInfoReqDTO.builder()
                .password("newPassword123")
                .image("new-profile-image.jpg")
                .favorites_work_id(5L)
                .favorites_home_id(10L)
                .build();

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("테스트사용자"))
                .andExpect(jsonPath("$.image").value("new-profile-image.jpg"));
    }

    @Test
    @DisplayName("즐겨찾기 셔틀 조회 성공 테스트")
    void getFavorites_Success() throws Exception {
        User savedUser = userRepository.save(testUser);
        String token = jwtUtil.generateToken(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                "user"
        );

        mockMvc.perform(get("/api/users/me/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritesWorkId").value(4))
                .andExpect(jsonPath("$.favoritesHomeId").value(9));
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 성공 테스트")
    void forgotPassword_Success() throws Exception {
        userRepository.save(testUser);
        String email = "test@gmail.com";

        mockMvc.perform(post("/api/users/forgot-password")
                        .param("email", email))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("이메일 인증 코드 전송 성공 테스트")
    void sendEmailCode_Success() throws Exception {
        String email = "verification@gmail.com";

        mockMvc.perform(post("/api/users/email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern("\\d{6}")));
    }

    @Test
    @DisplayName("잘못된 이메일 도메인으로 회원가입 실패 테스트")
    void registerUser_InvalidEmailDomain_ShouldFail() throws Exception {
        SignUpReqDTO signUpRequest = new SignUpReqDTO();
        signUpRequest.setName("테스트사용자");
        signUpRequest.setEmail("test@naver.com");  // gmail.com이 아닌 도메인
        signUpRequest.setPassword("password123");
        signUpRequest.setRole("user");

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isBadRequest());
    }

    private LoginReqDTO createLoginRequest(String email, String password) {
        try {
            LoginReqDTO loginRequest = new LoginReqDTO();

            java.lang.reflect.Field emailField = LoginReqDTO.class.getDeclaredField("email");
            emailField.setAccessible(true);
            emailField.set(loginRequest, email);

            java.lang.reflect.Field passwordField = LoginReqDTO.class.getDeclaredField("password");
            passwordField.setAccessible(true);
            passwordField.set(loginRequest, password);

            return loginRequest;
        } catch (Exception e) {
            throw new RuntimeException("LoginReqDTO 생성 실패", e);
        }
    }

    @Test
    @DisplayName("존재하지 않는 사용자 로그인 실패 테스트")
    void login_UserNotFound_ShouldFail() throws Exception {
        LoginReqDTO loginRequest = createLoginRequest("nonexistent@gmail.com", "password123");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());
    }
}