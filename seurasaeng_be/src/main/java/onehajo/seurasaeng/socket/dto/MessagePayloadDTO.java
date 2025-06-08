package onehajo.seurasaeng.socket.dto;

import lombok.Data;

/**
 * 소켓 통신 시 클라이언트와 서버 간 송수신하는 공통 메시지 포맷
 */
@Data
public class MessagePayloadDTO {
    private String type;
    private Long routeId;
    private Double latitude;
    private Double longitude;
    private String timestamp;
}
