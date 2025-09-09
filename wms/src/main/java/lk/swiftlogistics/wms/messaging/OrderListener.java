@Service
@RequiredArgsConstructor
public class OrderListener {
    private final PackageRepository repo;

    @RabbitListener(queues = RabbitConfig.ORDERS_QUEUE)
    public void handleOrder(String payload) {
        // parse JSON (use Jackson)
        Package pkg = new Package();
        pkg.setClientRef(UUID.randomUUID().toString());
        pkg.setStatus(PackageStatus.RECEIVED);
        repo.save(pkg);
        System.out.println("Order received -> " + pkg.getClientRef());
    }
}
