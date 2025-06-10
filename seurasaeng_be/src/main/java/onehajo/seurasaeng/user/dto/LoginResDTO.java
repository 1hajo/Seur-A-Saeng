package onehajo.seurasaeng.user.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoginResDTO {
    private String token;
    private long id;
    private String name;
    private String email;
    private String image;
    private String role;
}