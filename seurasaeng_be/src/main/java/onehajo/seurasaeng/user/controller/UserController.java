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
    public ResponseEntity<?> register(@RequestBody SignUpReqDTO request) throws Exception {
        LoginResDTO loginResDTO = null;
        if (Objects.equals(request.getRole(), "user")){
            loginResDTO = userService.registerUser(request);
        }
        else if (Objects.equals(request.getRole(), "admin")){
            loginResDTO = userService.registerAdmin(request);
        }

        return ResponseEntity.ok(loginResDTO);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("email") String email) throws MessagingException {
        String token = userService.validateDuplicateUserEmail(email);
        log.info("이메일 중복 확인");

        return ResponseEntity.ok(token);
    }

    @PostMapping("/email")
    public ResponseEntity<String> sendEmailCode(@RequestParam("email") String email) throws MessagingException {
        String code = mailService.joinEmail(email);
        log.info("이메일 인증 코드 전송");

        return new ResponseEntity<>(code, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReqDTO request) {
        LoginResDTO loginResDTO;

        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isPresent()) {
            loginResDTO = userService.loginUser(request);
            return ResponseEntity.ok(loginResDTO);
        }

        Optional<Manager> manager = managerRepository.findByEmail(request.getEmail());
        if (manager.isPresent()) {
            loginResDTO = userService.loginAdmin(request);
            return ResponseEntity.ok(loginResDTO);
        }

        throw new RuntimeException("사용자를 찾을 수 없습니다.");
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

        return new ResponseEntity<>(myPageResDTO, HttpStatus.OK);
    }

    @GetMapping("/me/noti")
    public ResponseEntity<?> myNoti(HttpServletRequest request) {
        Boolean read = userService.getMyNoti(request);

        return new ResponseEntity<>(read, HttpStatus.OK);
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateUser(HttpServletRequest request, @RequestBody MyInfoReqDTO myInfoReqDTO) {
        MyInfoResDTO myInfoResDTO = userService.getMyInfo(request, myInfoReqDTO);

        return new ResponseEntity<>(myInfoResDTO, HttpStatus.OK);
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<FavoriteShuttleResDto> getFavorites(HttpServletRequest request) {
        FavoriteShuttleResDto response = userService.getFavoriteShuttleIds(request);
        return ResponseEntity.ok(response);
    }
}