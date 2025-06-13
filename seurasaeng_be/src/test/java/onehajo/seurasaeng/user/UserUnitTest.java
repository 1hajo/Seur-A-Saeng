package onehajo.seurasaeng.user;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import onehajo.seurasaeng.entity.Manager;
import onehajo.seurasaeng.entity.Shuttle;
import onehajo.seurasaeng.entity.User;
import onehajo.seurasaeng.inquiry.repository.ManagerRepository;
import onehajo.seurasaeng.qr.service.QRService;
import onehajo.seurasaeng.redis.service.RedisTokenService;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import onehajo.seurasaeng.user.dto.*;
import onehajo.seurasaeng.user.exception.DuplicateUserException;
import onehajo.seurasaeng.user.exception.UnAuthenticatedEmailException;
import onehajo.seurasaeng.user.repository.UserRepository;
import onehajo.seurasaeng.user.service.UserService;
import onehajo.seurasaeng.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 단위 테스트")
public class UserUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTokenService redisTokenService;

    @Mock
    private ShuttleRepository shuttleRepository;

    @Mock
    private QRService qrService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Manager testManager;
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

        testUser = User.builder()
                .id(1L)
                .name("테스트사용자")
                .email("test@gmail.com")
                .password("encodedPassword")
                .favorites_work_id(defaultWorkShuttle)
                .favorites_home_id(defaultHomeShuttle)
                .read_newnoti(false)
                .build();

        testManager = Manager.builder()
                .id(1L)
                .email("admin@gmail.com")
                .password("encodedPassword")
                .build();
    }

    @Test
    @DisplayName("사용자 회원가입 성공")
    void registerUser_Success() throws Exception {
        SignUpReqDTO request = createSignUpRequest("새사용자", "newuser@gmail.com", "password123", "user");

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
        given(shuttleRepository.getReferenceById(4L)).willReturn(defaultWorkShuttle);
        given(shuttleRepository.getReferenceById(9L)).willReturn(defaultHomeShuttle);

        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            try {
                java.lang.reflect.Field idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(user, 1L);
            } catch (Exception e) {
                throw new RuntimeException("ID 설정 실패", e);
            }
            return user;
        });

        given(jwtUtil.generateToken(eq(1L), eq("새사용자"), eq("newuser@gmail.com"), eq("user")))
                .willReturn("test-token");
        willDoNothing().given(qrService).generateQRCode(eq(1L), eq("newuser@gmail.com"));
        willDoNothing().given(redisTokenService).saveToken(eq(1L), eq("test-token"));

        LoginResDTO result = userService.registerUser(request);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("test-token");
        assertThat(result.getName()).isEqualTo("새사용자");
        assertThat(result.getEmail()).isEqualTo("newuser@gmail.com");
        assertThat(result.getRole()).isEqualTo("user");

        verify(userRepository).save(any(User.class));
        verify(userRepository).flush();
        verify(qrService).generateQRCode(1L, "newuser@gmail.com");
        verify(redisTokenService).saveToken(1L, "test-token");
    }

    @Test
    @DisplayName("관리자 회원가입 성공")
    void registerAdmin_Success() {
        SignUpReqDTO request = createSignUpRequest(null, "admin@gmail.com", "password123", "admin");

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");

        given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> {
            Manager manager = invocation.getArgument(0);
            try {
                Field idField = Manager.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(manager, 1L);
            } catch (Exception e) {
                throw new RuntimeException("ID 설정 실패", e);
            }
            return manager;
        });

        given(jwtUtil.generateTokenAdmin(eq(1L), eq("admin@gmail.com"), eq("admin"))).willReturn("admin-token");
        willDoNothing().given(redisTokenService).saveToken(eq(1L), eq("admin-token"));

        LoginResDTO result = userService.registerAdmin(request);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("admin-token");
        assertThat(result.getEmail()).isEqualTo("admin@gmail.com");
        assertThat(result.getRole()).isEqualTo("admin");

        verify(managerRepository).save(any(Manager.class));
        verify(managerRepository).flush();
        verify(redisTokenService).saveToken(1L, "admin-token");
    }

    @Test
    @DisplayName("잘못된 이메일 도메인으로 회원가입 실패")
    void registerUser_InvalidEmailDomain_ShouldThrowException() {
        SignUpReqDTO request = createSignUpRequest("테스트사용자", "test@naver.com", "password123", "user");

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(UnAuthenticatedEmailException.class)
                .hasMessage("gmail.com 이메일만 가입할 수 있습니다.");
    }

    @Test
    @DisplayName("토큰이 없는 사용자 로그인 실패")
    void loginUser_NoToken_ShouldThrowException() {
        LoginReqDTO request = createLoginRequest("test@gmail.com", "password123");
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(testUser));
        given(redisTokenService.getTokenUser(testUser.getId())).willReturn(null);

        assertThatThrownBy(() -> userService.loginUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("토큰이 존재하지 않습니다.");
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패")
    void loginUser_WrongPassword_ShouldThrowException() {
        LoginReqDTO request = createLoginRequest("test@gmail.com", "wrongPassword");
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(testUser));
        given(redisTokenService.getTokenUser(testUser.getId())).willReturn("existing-token");
        given(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.loginUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("이메일 중복 검증 성공")
    void validateDuplicateUserEmail_Success() {
        String email = "unique@gmail.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        String result = userService.validateDuplicateUserEmail(email);

        assertThat(result).isEqualTo(email);
    }

    @Test
    @DisplayName("이메일 중복 검증 실패")
    void validateDuplicateUserEmail_DuplicateFound_ShouldThrowException() {
        String email = "existing@gmail.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.validateDuplicateUserEmail(email))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessage("이미 존재하는 회원입니다.");
    }

    @Test
    @DisplayName("이메일로 비밀번호 업데이트 성공")
    void updatePasswordByEmail_Success() {
        String newPassword = "newPassword123";
        String email = "test@gmail.com";

        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
        given(userRepository.save(any(User.class))).willReturn(testUser);

        userService.updatePasswordByEmail(newPassword, email);

        verify(userRepository).save(testUser);
        assertThat(testUser.getPassword()).isEqualTo(newPassword);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 비밀번호 업데이트 실패")
    void updatePasswordByEmail_UserNotFound_ShouldThrowException() {
        String newPassword = "newPassword123";
        String email = "nonexistent@gmail.com";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePasswordByEmail(newPassword, email))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("email에 정보가 맞지않습니다.");
    }

    @Test
    @DisplayName("마이페이지 조회 성공")
    void getMyUsers_Success() {
        String token = "Bearer test-token";
        given(httpServletRequest.getHeader("Authorization")).willReturn(token);
        given(jwtUtil.getIdFromToken("test-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        MyPageResDTO result = userService.getMyUsers(httpServletRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("테스트사용자");
        assertThat(result.getEmail()).isEqualTo("test@gmail.com");
        assertThat(result.getFavorites_work_id()).isEqualTo(4L);
        assertThat(result.getFavorites_home_id()).isEqualTo(9L);
    }

    @Test
    @DisplayName("개인정보 수정 성공")
    void getMyInfo_Success() {
        String token = "Bearer test-token";
        MyInfoReqDTO request = MyInfoReqDTO.builder()
                .password("newPassword")
                .image("new-image.jpg")
                .favorites_work_id(5L)
                .favorites_home_id(10L)
                .build();

        Shuttle newWorkShuttle = Shuttle.builder().id(5L).build();
        Shuttle newHomeShuttle = Shuttle.builder().id(10L).build();

        given(httpServletRequest.getHeader("Authorization")).willReturn(token);
        given(jwtUtil.getIdFromToken("test-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(shuttleRepository.getReferenceById(5L)).willReturn(newWorkShuttle);
        given(shuttleRepository.getReferenceById(10L)).willReturn(newHomeShuttle);
        given(userRepository.save(any(User.class))).willReturn(testUser);

        MyInfoResDTO result = userService.getMyInfo(httpServletRequest, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트사용자");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("즐겨찾기 셔틀 조회 성공")
    void getFavoriteShuttleIds_Success() {
        String token = "Bearer test-token";
        given(httpServletRequest.getHeader("Authorization")).willReturn(token);
        given(jwtUtil.getIdFromToken("test-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        FavoriteShuttleResDto result = userService.getFavoriteShuttleIds(httpServletRequest);

        assertThat(result).isNotNull();
        assertThat(result.getFavoritesWorkId()).isEqualTo(4L);
        assertThat(result.getFavoritesHomeId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 마이페이지 조회 실패")
    void getMyUsers_UserNotFound_ShouldThrowException() {
        String token = "Bearer test-token";
        given(httpServletRequest.getHeader("Authorization")).willReturn(token);
        given(jwtUtil.getIdFromToken("test-token")).willReturn(999L);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyUsers(httpServletRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    private LoginReqDTO createLoginRequest(String email, String password) {
        try {
            LoginReqDTO loginRequest = new LoginReqDTO();

            Field emailField = LoginReqDTO.class.getDeclaredField("email");
            emailField.setAccessible(true);
            emailField.set(loginRequest, email);

            Field passwordField = LoginReqDTO.class.getDeclaredField("password");
            passwordField.setAccessible(true);
            passwordField.set(loginRequest, password);

            return loginRequest;
        } catch (Exception e) {
            throw new RuntimeException("LoginReqDTO 생성 실패", e);
        }
    }

    private SignUpReqDTO createSignUpRequest(String name, String email, String password, String role) {
        try {
            SignUpReqDTO request = new SignUpReqDTO();

            if (name != null) {
                Field nameField = SignUpReqDTO.class.getDeclaredField("name");
                nameField.setAccessible(true);
                nameField.set(request, name);
            }

            Field emailField = SignUpReqDTO.class.getDeclaredField("email");
            emailField.setAccessible(true);
            emailField.set(request, email);

            Field passwordField = SignUpReqDTO.class.getDeclaredField("password");
            passwordField.setAccessible(true);
            passwordField.set(request, password);

            Field roleField = SignUpReqDTO.class.getDeclaredField("role");
            roleField.setAccessible(true);
            roleField.set(request, role);

            return request;
        } catch (Exception e) {
            throw new RuntimeException("SignUpReqDTO 생성 실패", e);
        }
    }
}