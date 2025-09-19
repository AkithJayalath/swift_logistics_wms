package lk.swiftlogistics.wms.config;

import lk.swiftlogistics.wms.domain.Driver;
import lk.swiftlogistics.wms.repo.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DriverRepository driverRepository;

    @Override
    public void run(String... args) throws Exception {
        if (driverRepository.count() == 0) {
            log.info("Initializing sample driver data...");
            initializeDrivers();
        }
    }

    private void initializeDrivers() {
        Driver driver1 = new Driver();
        driver1.setName("Kamal Perera");
        driver1.setLicenseNumber("B1234567");
        driver1.setPhoneNumber("+94 77 123 4567");
        driver1.setEmail("kamal.perera@swiftlogistics.lk");
        driver1.setAvailable(true);

        Driver driver2 = new Driver();
        driver2.setName("Nimal Silva");
        driver2.setLicenseNumber("B2345678");
        driver2.setPhoneNumber("+94 71 234 5678");
        driver2.setEmail("nimal.silva@swiftlogistics.lk");
        driver2.setAvailable(true);

        Driver driver3 = new Driver();
        driver3.setName("Sunil Fernando");
        driver3.setLicenseNumber("B3456789");
        driver3.setPhoneNumber("+94 76 345 6789");
        driver3.setEmail("sunil.fernando@swiftlogistics.lk");
        driver3.setAvailable(false);

        Driver driver4 = new Driver();
        driver4.setName("Chaminda Rajapakse");
        driver4.setLicenseNumber("B4567890");
        driver4.setPhoneNumber("+94 78 456 7890");
        driver4.setEmail("chaminda.rajapakse@swiftlogistics.lk");
        driver4.setAvailable(true);

        Driver driver5 = new Driver();
        driver5.setName("Pradeep Mendis");
        driver5.setLicenseNumber("B5678901");
        driver5.setPhoneNumber("+94 72 567 8901");
        driver5.setEmail("pradeep.mendis@swiftlogistics.lk");
        driver5.setAvailable(false);

        driverRepository.save(driver1);
        driverRepository.save(driver2);
        driverRepository.save(driver3);
        driverRepository.save(driver4);
        driverRepository.save(driver5);

        log.info("Sample driver data initialized successfully");
    }
}