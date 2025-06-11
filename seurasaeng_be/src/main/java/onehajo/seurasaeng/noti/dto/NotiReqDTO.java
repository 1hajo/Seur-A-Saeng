package onehajo.seurasaeng.noti.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NotiReqDTO {

    private String title;
    private String content;

    // 기본 생성자 (필수)
    public NotiReqDTO() {}
}
