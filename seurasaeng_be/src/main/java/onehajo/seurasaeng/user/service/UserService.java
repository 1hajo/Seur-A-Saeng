package onehajo.seurasaeng.user.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import onehajo.seurasaeng.entity.Manager;
import onehajo.seurasaeng.entity.Newnoti;
import onehajo.seurasaeng.entity.Shuttle;
import onehajo.seurasaeng.entity.User;
import onehajo.seurasaeng.newnoti.repository.NewnotiRepository;
import onehajo.seurasaeng.qr.service.QRService;
import onehajo.seurasaeng.inquiry.repository.ManagerRepository;
import onehajo.seurasaeng.qr.exception.UserNotFoundException;
import onehajo.seurasaeng.redis.service.RedisTokenService;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import onehajo.seurasaeng.user.exception.*;
import onehajo.seurasaeng.user.repository.UserRepository;
import onehajo.seurasaeng.util.JwtUtil;
import onehajo.seurasaeng.user.dto.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;
    private final ShuttleRepository shuttleRepository;
    private final QRService qrService;
    private final NewnotiRepository newNotiRepository;


    public UserService(UserRepository userRepository, ManagerRepository managerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RedisTokenService redisTokenService, ShuttleRepository shuttleRepository,
                       QRService qrService, NewnotiRepository newNotiRepository) {
        this.userRepository = userRepository;
        this.managerRepository = managerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTokenService = redisTokenService;
        this.shuttleRepository = shuttleRepository;
        this.qrService = qrService;
        this.newNotiRepository = newNotiRepository;
    }

    @Transactional
    public LoginResDTO registerUser(SignUpReqDTO request) throws Exception {
        // 이메일 도메인 검사
        String email = request.getEmail();
        if (!email.endsWith("@gmail.com")) {
            throw new UnAuthenticatedEmailException("gmail.com 이메일만 가입할 수 있습니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 사용자입니다.");
        }

        Shuttle defaultWork = shuttleRepository.getReferenceById(4L);
        Shuttle defaultHome = shuttleRepository.getReferenceById(9L);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .favorites_work_id(defaultWork)
                .favorites_home_id(defaultHome)
                .build();

        userRepository.save(user);
        userRepository.flush();

        String token = jwtUtil.generateToken(user.getId(), user.getName(), user.getEmail(), request.getRole());
        qrService.generateQRCode(user.getId(), user.getEmail());

        redisTokenService.saveToken(user.getId(), token);

        return LoginResDTO.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .image(user.getImage())
                .role("user")
                .build();
    }

    @Transactional
    public LoginResDTO registerAdmin(SignUpReqDTO request) {
        // 이메일 도메인 검사
        String email = request.getEmail();
        if (!email.endsWith("@gmail.com")) {
            throw new UnAuthenticatedEmailException("gmail.com 이메일만 가입할 수 있습니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 사용자입니다.");
        }

        Manager manager = Manager.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        managerRepository.save(manager);
        managerRepository.flush();

        String token = jwtUtil.generateTokenAdmin(manager.getId(), manager.getEmail(), request.getRole());
        redisTokenService.saveToken(manager.getId(), token);

        return LoginResDTO.builder()
                .token(token)
                .id(manager.getId())
                .email(manager.getEmail())
                .role("admin")
                .build();
    }

    @Transactional
    public LoginResDTO loginUser(LoginReqDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String token = redisTokenService.getToken(user.getId());
        if (token == null || token.isBlank()) {
            throw new RuntimeException("토큰이 존재하지 않습니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return LoginResDTO.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .image(user.getImage())
                .role("user")
                .read_newnoti(user.isRead_newnoti())
                .build();
    }

    @Transactional
    public LoginResDTO loginAdmin(LoginReqDTO request) {
        Manager manager = managerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다."));

        String token = redisTokenService.getToken(manager.getId());
        if (token == null || token.isBlank()) {
            throw new RuntimeException("토큰이 존재하지 않습니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), manager.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return LoginResDTO.builder()
                .token(token)
                .id(manager.getId())
                .email(manager.getEmail())
                .role("admin")
                .read_newnoti(false)
                .build();
    }

    public String validateDuplicateUserEmail(String email) {
        Optional<User> findUser = userRepository.findByEmail(email);
        if (findUser.isPresent()) {
            throw new DuplicateUserException("이미 존재하는 회원입니다.");
        }
        return email;
    }

    public void updatePasswordByEmail(String password, String email) {
        Optional<User> findUsers = userRepository.findByEmail(email);
        if (findUsers.isEmpty()) {
            throw new EntityNotFoundException("email에 정보가 맞지않습니다.");
        }

        User user = findUsers.get();
        user.setPassword(passwordEncoder.encode(password)); // 비밀번호 설정
        userRepository.save(user);
        userRepository.flush();
    }

    public MyPageResDTO getMyUsers(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long id = jwtUtil.getIdFromToken(token);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        String name = user.getName();
        String email = user.getEmail();
        String image = user.getImage();

        return MyPageResDTO.builder()
                .id(id)
                .name(name)
                .email(email)
                .image(image)
                .favorites_work_id(user.getFavorites_work_id().getId())
                .favorites_home_id(user.getFavorites_home_id().getId())
                .build();
    }

    @Transactional
    public MyInfoResDTO getMyInfo(HttpServletRequest request, MyInfoReqDTO info) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long id = jwtUtil.getIdFromToken(token);

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        // 사용자 정보 수정
        if (info.getPassword()!=null) user.setPassword(passwordEncoder.encode(info.getPassword()));
        if (info.getImage()!=null) user.setImage(info.getImage());
        if (info.getFavorites_work_id()!=0) {
            user.setFavorites_work_id(shuttleRepository.getReferenceById(info.getFavorites_work_id()));
        }
        if (info.getFavorites_home_id()!=0) {
            user.setFavorites_home_id(shuttleRepository.getReferenceById(info.getFavorites_home_id()));
        }
        userRepository.save(user);
        userRepository.flush();

        return MyInfoResDTO.builder()
                .name(user.getName())
                .image(user.getImage())
                .favorites_work_id(user.getFavorites_work_id().getId())
                .favorites_home_id(user.getFavorites_home_id().getId())
                .build();
    }

    public FavoriteShuttleResDto getFavoriteShuttleIds(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long id = jwtUtil.getIdFromToken(token);

        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return FavoriteShuttleResDto.builder()
                .favoritesWorkId(user.getFavorites_work_id().getId())
                .favoritesHomeId(user.getFavorites_home_id().getId())
                .build();
    }

    public void readNoti(HttpServletRequest request, long id) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long userid = jwtUtil.getIdFromToken(token);

        User user = userRepository.findById(userid).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Optional<Newnoti> existingNewnoti = newNotiRepository.findFirstByOrderByIdAsc();
        if (existingNewnoti.isPresent() && existingNewnoti.get().getNoti_id().getId().equals(id)) {
            user.setRead_newnoti(true);
            userRepository.save(user);
            userRepository.flush();
        }

    }
}
