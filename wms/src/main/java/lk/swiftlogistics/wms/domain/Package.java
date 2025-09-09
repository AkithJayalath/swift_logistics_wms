package lk.swiftlogistics.wms.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Package {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String clientRef;

    @Enumerated(EnumType.STRING)
    private PackageStatus status;

    private String currentZone;
    private Instant lastUpdated = Instant.now();

    // getters, setters
}
