package lk.swiftlogistics.wms.repo;

import lk.swiftlogistics.wms.domain.WarehouseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseEventRepository extends JpaRepository<WarehouseEvent, Long> {
    List<WarehouseEvent> findTop50ByStatusOrderByCreatedAtAsc(WarehouseEvent.Status status);
}
