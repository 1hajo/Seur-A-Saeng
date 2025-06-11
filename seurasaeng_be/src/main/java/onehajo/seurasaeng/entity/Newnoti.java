package onehajo.seurasaeng.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "newnotices", schema = "seurasaeng_test")
public class Newnoti {

    @Id
    // Auto Increment
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "newnoti_id")
    private Long id;

    // 외래키 noti_id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noti_id")
    private Noti noti_id;
}
