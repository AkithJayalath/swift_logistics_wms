package lk.swiftlogistics.wms.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "wms.exchange";

    public static final String ORDERS_QUEUE = "orders.inbound";
    public static final String ORDERS_DLQ = "orders.inbound.dlq";

    public static final String ROUTE_QUEUE = "route.assignments";
    public static final String ROUTE_DLQ = "route.assignments.dlq";

    public static final String EVENTS_QUEUE = "wms.events";

    // Main exchange
    @Bean
    public DirectExchange appExchange() {
        return new DirectExchange(EXCHANGE);
    }

    // Dead Letter Exchange
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(EXCHANGE + ".dlx");
    }

    // Queues with DLQ
    @Bean
    public Queue ordersQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE + ".dlx");
        args.put("x-dead-letter-routing-key", ORDERS_DLQ);
        return new Queue(ORDERS_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue ordersDlq() {
        return new Queue(ORDERS_DLQ, true);
    }

    @Bean
    public Queue routeQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE + ".dlx");
        args.put("x-dead-letter-routing-key", ROUTE_DLQ);
        return new Queue(ROUTE_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue routeDlq() {
        return new Queue(ROUTE_DLQ, true);
    }

    @Bean
    public Queue eventsQueue() {
        return new Queue(EVENTS_QUEUE, true);
    }

    // Bindings
    @Bean
    public Binding ordersBinding() {
        return BindingBuilder.bind(ordersQueue()).to(appExchange()).with(ORDERS_QUEUE);
    }

    @Bean
    public Binding routeBinding() {
        return BindingBuilder.bind(routeQueue()).to(appExchange()).with(ROUTE_QUEUE);
    }

    @Bean
    public Binding eventsBinding() {
        return BindingBuilder.bind(eventsQueue()).to(appExchange()).with(EVENTS_QUEUE);
    }

    @Bean
    public Binding ordersDlqBinding() {
        return BindingBuilder.bind(ordersDlq()).to(dlxExchange()).with(ORDERS_DLQ);
    }

    @Bean
    public Binding routeDlqBinding() {
        return BindingBuilder.bind(routeDlq()).to(dlxExchange()).with(ROUTE_DLQ);
    }
}
