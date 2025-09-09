@RestController
@RequestMapping("/api/wms/packages")
@RequiredArgsConstructor
public class PackageController {
    private final PackageRepository repo;
    private final EventPublisher publisher;

    @PostMapping("/receive")
    public Package receive(@RequestBody Map<String, String> body) {
        Package pkg = new Package();
        pkg.setClientRef(body.get("clientRef"));
        pkg.setStatus(PackageStatus.RECEIVED);
        pkg.setLastUpdated(Instant.now());
        repo.save(pkg);

        publisher.publishEvent("{\"type\":\"PKG_RECEIVED\",\"ref\":\"" + pkg.getClientRef() + "\"}");
        return pkg;
    }

    @GetMapping("/{ref}")
    public Package find(@PathVariable String ref) {
        return repo.findByClientRef(ref).orElseThrow();
    }
}
