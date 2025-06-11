package onehajo.seurasaeng.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notices", schema = "seurasaeng_test")
public class Noti {

    @Id
    // Auto Increment
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "noti_id")
    private Long id;

    @NotNull
    @Column(name = "noti_title", columnDefinition = "varchar(50)")
    @NotBlank(message = "공지 제목은 필수 입력 값입니다.")
    private String title;

    @Column(name = "noti_content", columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Column(name = "noti_created_at", columnDefinition = "timestamp")
    private LocalDateTime created_at;

    // 외래키 manager_id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Manager manager_id;
}
