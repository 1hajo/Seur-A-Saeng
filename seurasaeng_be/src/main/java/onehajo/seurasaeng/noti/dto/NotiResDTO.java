package onehajo.seurasaeng.noti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
public class NotiResDTO {
    private long id;
    private String title;
    private String content;
    private LocalDateTime created_at;

    // 기본 생성자 (필수)
    public NotiResDTO() {}
}