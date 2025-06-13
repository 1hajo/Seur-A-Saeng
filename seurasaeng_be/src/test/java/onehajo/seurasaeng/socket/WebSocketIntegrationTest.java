package onehajo.seurasaeng.socket;

import onehajo.seurasaeng.entity.Location;
import onehajo.seurasaeng.entity.Shuttle;
import onehajo.seurasaeng.entity.User;
import onehajo.seurasaeng.shuttle.repository.LocationRepository;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import onehajo.seurasaeng.socket.dto.EndMessagePayloadDTO;
import onehajo.seurasaeng.socket.dto.MessagePayloadDTO;
import onehajo.seurasaeng.socket.dto.MessageType;
import onehajo.seurasaeng.user.repository.UserRepository;
import onehajo.seurasaeng.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
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
import org.springframework.http.HttpHeaders;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // 실제 서버 띄우고 테스트 / 임의의 포트에서 실행됨, 테스트에서 @LocalServerPort로 접근 가능
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // 테스트 메서드 실행 순서를 Order(n) 에 따라 강제로 지정
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;
    // 위에서 설정한 랜덤 포트를 이 필드로 받아옴
    // WebSocket 연결 시 ws://localhost:{port}/ws 식으로 사용
    @Autowired
    private JwtUtil jwtUtil; // jwt 발급을 위한 유틸
    /* 테스트용 엔티티를 DB에 저장하기 위한 JPA Repository들 */
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShuttleRepository shuttleRepository;
    @Autowired
    private LocationRepository locationRepository;
    // REST API 호출 테스트 시 사용되는 Spring 기본 HTTP 클라이언트
    @Autowired
    private TestRestTemplate restTemplate;

    private WebSocketStompClient stompClient;
    private String jwtToken;
    private final Long routeId = 999L;

    // 테스트 준비 로직
    @BeforeEach
    void setUp() {
        /* WebSocket 클라이언트 초기화 */
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        stompClient.setMessageConverter(converter);

        /* 테스트 용 데이터 저장 */
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

    /* WebSocket 연결 함수
    *  - 주어진 jwt 토큰을 이용해 WebSocket 연결을 생성
    *  - Authorization 헤더에 토큰을 넣어서 서버에 전송
    *  - 연결이 완료되면 StompSession을 반환
    *  */
    private StompSession connectWithToken(String token) throws Exception {
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }

        return stompClient.connect(url, new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);
    }

    /* 정상 jwt 토큰을 넣고 WebSocket 연결을 시도
    *  - 연결 성공 여부를 검증
    *  */
    @Test
    @Order(1)
    @DisplayName("웹소켓 통합 테스트 - jwt 포함 연결 성공")
    void jwt포함_연결_성공() throws Exception {
        StompSession session = connectWithToken(jwtToken);
        assertTrue(session.isConnected());
    }

    /* 토큰 없이 WebSocket 연결을 시도
    *  - 서버에서 인증 실패 예외가 발생하는지 확인
    *   */
    @Test
    @Order(2)
    @DisplayName("웹소켓 통합 테스트 - jwt 토큰 없이 연결 시도 실패")
    void 토큰없이_연결시도_실패() {
        assertThrows(Exception.class, () -> {
            connectWithToken(null);
        });
    }

    @Test
    @Order(3)
    @DisplayName("웹소켓 통합 테스트 - GPS 메시지 전송 시 구독자는 수신")
    void gps_메시지_전송시_구독자는_수신() throws Exception {
        // jwt 토큰으로 WebSocket 서버에 연결해서 STOMP 세션 생성
        StompSession session = connectWithToken(jwtToken);
        // 테스트 스레드가 메시지 수신을 기다리기 위해 사용하는 비동기 future 객체
        CompletableFuture<MessagePayloadDTO> future = new CompletableFuture<>();
        session.subscribe("/topic/route/" + routeId, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) {
                return MessagePayloadDTO.class;
            }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                future.complete((MessagePayloadDTO) payload);
            }
        });

        MessagePayloadDTO dto = new MessagePayloadDTO();
        dto.setRouteId(routeId);
        dto.setLatitude(37.123);
        dto.setLongitude(127.456);
        dto.setType(MessageType.RUNNING);
        dto.setTimestamp("2025-06-11T12:00:00");

        session.send("/app/route/" + routeId, dto);
        MessagePayloadDTO received = future.get(3, TimeUnit.SECONDS);

        assertEquals(dto.getLatitude(), received.getLatitude());
        assertEquals(dto.getLongitude(), received.getLongitude());
    }

    @Test
    @Order(4)
    @DisplayName("웹소켓 통합 테스트 - 운행 종료 API 호출 시 종료 메시지 브로드캐스트")
    void 운행종료API_호출시_종료_메시지가_브로드캐스트() throws Exception {
        StompSession session = connectWithToken(jwtToken);

        CompletableFuture<EndMessagePayloadDTO> future = new CompletableFuture<>();
        session.subscribe("/topic/route/" + routeId, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) {
                return EndMessagePayloadDTO.class;
            }

            @Override public void handleFrame(StompHeaders headers, Object payload) {
                future.complete((EndMessagePayloadDTO) payload);
            }
        });

        Thread.sleep(300);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/route/" + routeId + "/end",
                request,
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        EndMessagePayloadDTO endMessage = future.get(5, TimeUnit.SECONDS);
        assertNotNull(endMessage);
    }
}
