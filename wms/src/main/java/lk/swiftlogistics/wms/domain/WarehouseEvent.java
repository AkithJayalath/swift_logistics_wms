package lk.swiftlogistics.wms.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class WarehouseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String payload;  // JSON string

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;

    public enum Status { NEW, SENT, FAILED }

    // getters, setters
}
