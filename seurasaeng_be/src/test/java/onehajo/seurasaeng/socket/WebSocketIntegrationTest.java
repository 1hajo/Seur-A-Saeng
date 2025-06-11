package onehajo.seurasaeng.socket;

import lombok.RequiredArgsConstructor;
import onehajo.seurasaeng.entity.Location;
import onehajo.seurasaeng.entity.Shuttle;
import onehajo.seurasaeng.entity.User;
import onehajo.seurasaeng.shuttle.repository.LocationRepository;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import onehajo.seurasaeng.socket.dto.MessagePayloadDTO;
import onehajo.seurasaeng.user.repository.UserRepository;
import onehajo.seurasaeng.util.JwtUtil;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShuttleRepository shuttleRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private TestRestTemplate restTemplate;

    private WebSocketStompClient stompClient;
    private String jwtToken;
    private final Long routeId = 999L;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        stompClient.setMessageConverter(converter);

        // 유저 생성 및 JWT 발급
        Location location = locationRepository.save(Location.builder()
                        .locationId(999L)
                        .locationName("장소 이름")
                        .longitude(33.1)
                        .latitude(125.1)
                        .build());

        Shuttle shuttle = shuttleRepository.save(Shuttle.builder()
                        .id(999L)
                        .shuttleName("셔틀 이름")
                        .departure(location)
                        .destination(location)
                        .isCommute(true)
                        .build());

        User user = userRepository.save(User.builder()
                .id(999L)
                .name("남예준")
                .email("yejun@example.com")
                .password("yejun_pw")
                .favorites_work_id(shuttle)
                .favorites_home_id(shuttle)
                .build());
        jwtToken = jwtUtil.generateToken(user.getId(), user.getName(), user.getEmail(), "user");
    }

    private StompSession connectWithToken(String token) throws Exception {
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }

        return stompClient.connect(url, new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);
    }

    @Test
    @Order(1)
    @DisplayName("웹소켓 통합 테스트 - jwt 포함 연결 성공")
    void jwt포함_연결_성공해야함() throws Exception {
        StompSession session = connectWithToken(jwtToken);
        assertTrue(session.isConnected());
    }

    @Test
    @Order(2)
    @DisplayName("웹소켓 통합 테스트 - jwt 토큰 없이 연결 시도 실패")
    void 토큰없이_연결시도_실패해야함() {
        assertThrows(Exception.class, () -> {
            connectWithToken(null);
        });
    }
}
