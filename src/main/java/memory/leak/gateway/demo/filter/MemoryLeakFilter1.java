package memory.leak.gateway.demo.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class MemoryLeakFilter1 implements GlobalFilter, Ordered {

    public static final String CACHED_GATEWAY_CONTEXT_BODY = "cachedGatewayContextBody";

    private static final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        if (headers.getContentLength() > 0) {
            return readBody(exchange, chain);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> readBody(ServerWebExchange exchange, GatewayFilterChain chain) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    DataBufferUtils.retain(dataBuffer);
                    Flux<DataBuffer> cachedFlux = Flux.defer(() -> Flux.just(dataBuffer.slice(0, dataBuffer.readableByteCount())));
                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return cachedFlux;
                        }
                    };

                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                    MediaType contentType = exchange.getRequest().getHeaders().getContentType();
                    Class cls = String.class;
                    if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
                        cls = Resource.class;
                    }
                    return ServerRequest.create(mutatedExchange, messageReaders)
                            .bodyToMono(cls)
                            .doOnNext(value -> {
                                exchange.getAttributes().put(CACHED_GATEWAY_CONTEXT_BODY, value);
                            })
                            .then(chain.filter(mutatedExchange));
                });
    }

    @Override
    public int getOrder() {
        return AuthorizeFilter.AUTHORIZE_FILTER_ORDER - 2;
    }

}
