package onehajo.seurasaeng.user.controller;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import onehajo.seurasaeng.entity.Manager;
import onehajo.seurasaeng.entity.User;
import onehajo.seurasaeng.inquiry.repository.ManagerRepository;
import onehajo.seurasaeng.mail.service.MailService;
import onehajo.seurasaeng.user.repository.UserRepository;
import onehajo.seurasaeng.user.service.UserService;
import onehajo.seurasaeng.util.JwtUtil;
import onehajo.seurasaeng.user.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    @Getter
    private final JwtUtil jwtUtil;
    private final MailService mailService;

    public UserController(UserService userService, UserRepository userRepository, JwtUtil jwtUtil, MailService mailService, ManagerRepository managerRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.mailService = mailService;
        this.managerRepository = managerRepository;
    }

    @Transactional
    @PostMapping("/signup")
    public ResponseEntity<String> register(@RequestBody SignUpReqDTO request) {
        log.info("회원가입 시도");
        String token = "token";
        if (Objects.equals(request.getRole(), "user")){
            token = userService.registerUser(request);
        }
        else if (Objects.equals(request.getRole(), "admin")){
            token = userService.registerAdmin(request);
        }
        return ResponseEntity.ok(token);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("email") String email) throws MessagingException {
        String token = userService.validateDuplicateUserEmail(email);
        mailService.joinEmail(email);
        log.info("이메일 중복 확인");
        return ResponseEntity.ok(token);
    }

    @PostMapping("/email")
    public ResponseEntity<String> sendEmailCode(@RequestParam("email") String email) throws MessagingException {
        String code = mailService.joinEmail(email);
        log.info("이메일 인증 코드 전송");

        return new ResponseEntity<String>(code, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReqDTO request) {
        String[] str = new String[2];
        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isPresent()) {
            str = userService.loginUser(request);
        }
        else {
            Optional<Manager> manager = managerRepository.findByEmail(request.getEmail());
            if (manager.isPresent()) {
                str = userService.loginAdmin(request);
            }
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "로그인 성공");
        response.put("token", str[0]);
        response.put("role", str[1]); // str[1] 포함

        return ResponseEntity.ok()
                //.header("Authorization", "Bearer " + str[0]) // ✅ JWT를 헤더에 포함
                .body(response); // 혹은 사용자 정보 등 추가 가능
    }

    @GetMapping("/auto-login")
    public ResponseEntity<?> autoLogin(@RequestBody AutoLoginReqDTO request) {
        String token = userService.remakeToken(request);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token) // ✅ JWT를 헤더에 포함
                .body("재로그인 성공"); // 혹은 사용자 정보 등 추가 가능
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> lostPassword(@RequestParam("email") String email) throws MessagingException {
        // mailService에서 임시 비밀번호 생성
        // 메일 전송
        // 해당 string으로 update
        String newPassword = mailService.tempPassword(email);
        userService.updatePasswordByEmail(newPassword, email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<?> myPage(HttpServletRequest request) {
        MyPageResDTO myPageResDTO = userService.getMyUsers(request);

        return new ResponseEntity<MyPageResDTO>(myPageResDTO, HttpStatus.OK);
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateUser(HttpServletRequest request, @RequestBody MyInfoReqDTO myInfoReqDTO) {
        MyInfoResDTO myInfoResDTO = userService.getMyInfo(request, myInfoReqDTO);

        return new ResponseEntity<MyInfoResDTO>(myInfoResDTO, HttpStatus.OK);
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<FavoriteShuttleResDto> getFavorites(HttpServletRequest request) {
        FavoriteShuttleResDto response = userService.getFavoriteShuttleIds(request);
        return ResponseEntity.ok(response);
    }
}