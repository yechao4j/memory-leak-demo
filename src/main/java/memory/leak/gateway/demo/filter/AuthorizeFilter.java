package memory.leak.gateway.demo.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class AuthorizeFilter extends AbstractGlobalFilter {

    public static final int AUTHORIZE_FILTER_ORDER = 0;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        AtomicReference<String> ssid = getParam(exchange, request, "ssid");
        log.info("ssid: {}", ssid);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return AUTHORIZE_FILTER_ORDER;
    }

}
