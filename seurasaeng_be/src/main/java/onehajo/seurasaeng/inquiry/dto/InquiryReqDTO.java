package onehajo.seurasaeng.inquiry.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryReqDTO {
    private String title;
    private String content;
}
