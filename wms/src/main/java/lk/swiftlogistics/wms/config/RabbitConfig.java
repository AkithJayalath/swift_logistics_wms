@Configuration
public class RabbitConfig {
    public static final String ORDERS_QUEUE = "orders.inbound";
    public static final String ROUTE_QUEUE = "route.assignments";
    public static final String EVENTS_QUEUE = "wms.events";

    @Bean
    public Queue ordersQueue() { return new Queue(ORDERS_QUEUE, true); }

    @Bean
    public Queue routeQueue() { return new Queue(ROUTE_QUEUE, true); }

    @Bean
    public Queue eventsQueue() { return new Queue(EVENTS_QUEUE, true); }
}
