package memory.leak.gateway.demo;

import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

public class MemoryLeakTest {

    @Test
    public void test() {
        while (true) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForLocation("http://10.82.3.246:8080", "ssid=abc123");
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (Exception e) {

            }
        }
    }

}
