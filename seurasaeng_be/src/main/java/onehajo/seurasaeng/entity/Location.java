package onehajo.seurasaeng.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@Table(name = "location", schema = "seurasaeng_test")
public class Location {

    @Id
    @Column(name = "location_id")
    private Long locationId;

    @NotNull
    @Column(name = "location_name")
    private String locationName;

    @NotNull
    @Column(name = "latitude")
    private Double latitude;

    @NotNull
    @Column(name = "longitude")
    private Double longitude;
}