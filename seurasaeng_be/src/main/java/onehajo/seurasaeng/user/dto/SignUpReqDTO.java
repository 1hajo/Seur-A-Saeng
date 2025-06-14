package onehajo.seurasaeng.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
@AllArgsConstructor
public class SignUpReqDTO {

    private String name;
    private String email;
    private String password;
    private String role;

    // 기본 생성자 (필수)
    public SignUpReqDTO() {}
}

