package memory.leak.gateway.demo.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;

@Slf4j
// @Component
public class MemoryLeakFilter2 implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        DataBuffer body = exchange.getAttributeOrDefault(CACHED_REQUEST_BODY_ATTR, null);

        if (body != null) {
            return chain.filter(exchange);
        }

        return ServerWebExchangeUtils
                .cacheRequestBody(exchange,
                        (serverHttpRequest) -> chain.filter(
                                exchange.mutate().request(serverHttpRequest).build()))
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return AuthorizeFilter.AUTHORIZE_FILTER_ORDER - 2;
    }

}
