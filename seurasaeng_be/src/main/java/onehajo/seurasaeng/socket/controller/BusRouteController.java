package onehajo.seurasaeng.socket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onehajo.seurasaeng.socket.dto.EndMessagePayloadDTO;
import onehajo.seurasaeng.socket.dto.MessagePayloadDTO;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class BusRouteController {

    // WebSocket 메시지 송신을 위한 템플릿
    private final SimpMessagingTemplate messagingTemplate;

    /**
     *  실시간 GPS 데이터 수신 후 브로드캐스트.
     * 클라이언트가 STOMP SEND로 위치 데이터를 전송하면
     * 서버는 구독자들에게 실시간 위치를 브로드캐스트.
     *
     * @param routeId  노선 ID (STOMP destination 경로 변수)
     * @param payload  클라이언트로부터 수신한 GPS 위치 데이터
     */
    @MessageMapping("/route/{routeId}")
    public void broadcastGps(@DestinationVariable Long routeId, MessagePayloadDTO payload) {
        log.info("GPS 수신: {}", payload);
        // 구독자들에게 실시간 위치 데이터 브로드캐스트
        messagingTemplate.convertAndSend("/topic/route/" + routeId, payload);
    }


    /**
     * 운행 종료 API
     * 서버는 운행 종료 신호를 WebSocket 브로드캐스트하여
     * 모든 구독자에게 운행 종료를 알림.
     *
     * @param routeId  종료할 노선 ID
     */
    @PostMapping("/{routeId}/end")
    public void endRoute(@PathVariable Long routeId) {
        log.info("운행 종료 요청 수신: routeId={}", routeId);

        // 운행 종료 메시지 생성
        EndMessagePayloadDTO endMessage = EndMessagePayloadDTO.create();
        // 운행 종료 신호를 모든 구독자들에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/route/" + routeId, endMessage);
    }
}