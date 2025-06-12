package onehajo.seurasaeng.noti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
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