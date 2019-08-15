package memory.leak.gateway.demo.filter;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractGlobalFilter implements GlobalFilter, Ordered {

    protected AtomicReference<String> getParam(ServerWebExchange exchange, ServerHttpRequest request, String key) {
        // sessionId获取顺序：header -> url参数 -> body
        HttpHeaders headers = request.getHeaders();
        AtomicReference<String> valueRef = new AtomicReference<>(headers.getFirst(key));
        if (valueRef.get() == null) {
            valueRef.set(request.getQueryParams().getFirst(key));
        }
        if (valueRef.get() == null) {
            Map<String, Object> payloadMap = parsePayloadMap(exchange);
            if (payloadMap != null) {
                valueRef.set((String) payloadMap.get(key));
            }
        }
        return valueRef;
    }

    protected Map<String, Object> parsePayloadMap(ServerWebExchange exchange) {
        Object payload = exchange.getAttributeOrDefault(MemoryLeakFilter1.CACHED_GATEWAY_CONTEXT_BODY, null);
        if (payload == null) {
            return null;
        }
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        Map<String, Object> payloads = Maps.newHashMap();
        if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {

        } else if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                || MediaType.APPLICATION_JSON_UTF8.isCompatibleWith(contentType)) {
            try {
                payloads = JSON.parseObject(payload.toString());
            } catch (Exception e) {
            }
        } else {
            try {
                List<NameValuePair> pairs = URLEncodedUtils.parse(payload.toString(), Charset.defaultCharset());
                payloads = pairs.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (o, n) -> n));
            } catch (Exception e) {
            }
        }
        return payloads;
    }

}
