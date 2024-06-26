package com.songko.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;

@Slf4j
@Configuration
public class HealthCheckConfiguration {
    private HealthAggregator healthAggregator;

    private final WebClient.Builder webClientBuilder;   // GatewayApplication에서 생성된 Bean.
    private WebClient webClient;

    @Autowired
    public HealthCheckConfiguration(
            WebClient.Builder webClientBuilder,
            HealthAggregator healthAggregator
    ) {
        this.webClientBuilder = webClientBuilder;
        this.healthAggregator = healthAggregator;
    }

    /**
     * ProductCompositeIntegration에 정의된 헬퍼 메서드를 사용해 세 가지 핵심 마이크로서비스의 상태 정보를 등록
     *
     * @return
     */
    @Bean
    ReactiveHealthIndicator healthcheckMicroservices() {
        ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry(new LinkedHashMap<>());

        registry.register("product", () -> getHealth("http://product")); // ProductCompositeIntegration.getProductHealth() 호출
        registry.register("recommendation", () -> getHealth("http://recommendation"));  // ProductCompositeIntegration.getRecommendationHealth() 호출
        registry.register("review", () -> getHealth("http://review"));  // ProductCompositeIntegration.getReviewHealth() 호출
        registry.register("product-composite", () -> getHealth("http://product-composite"));

        return new CompositeReactiveHealthIndicator(healthAggregator, registry);
    }

    private Mono<Health> getHealth(String url) {
        url += "/actuator/health";
        log.debug("Will call the Health API on URL: {}", url);
        return getWebClient().get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(s -> new Health.Builder().up().build())
                .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
                .log();
    }

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder.build();
        }
        return webClient;
    }
}