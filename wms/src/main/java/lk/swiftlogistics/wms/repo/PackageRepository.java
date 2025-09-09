package lk.swiftlogistics.wms.repo;

import lk.swiftlogistics.wms.domain.Package;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PackageRepository extends JpaRepository<Package, Long> {
    Optional<Package> findByClientRef(String clientRef);
}
