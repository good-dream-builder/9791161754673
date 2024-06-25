package com.songko.productcompositeservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.songko.api.core.product.Product;
import com.songko.api.core.product.ProductService;
import com.songko.api.core.recommendation.Recommendation;
import com.songko.api.core.recommendation.RecommendationService;
import com.songko.api.core.review.Review;
import com.songko.api.core.review.ReviewService;
import com.songko.api.event.Event;
import com.songko.util.exceptions.InvalidInputException;
import com.songko.util.exceptions.NotFoundException;
import com.songko.util.http.HttpErrorInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;


@Slf4j
// 이벤트를 다른 토픽에 게시하려면 토픽별 MessageChannel을 선언한 자바 인터페이스를 만들고, EnableBinding 애노테이션을 선언해 활성화해야 한다
@EnableBinding(ProductCompositeIntegration.MessageSources.class)
@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private final WebClient webClient;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    private MessageSources messageSources;

    public interface MessageSources {
        String OUTPUT_PRODUCTS = "output-products";
        String OUTPUT_RECOMMENDATIONS = "output-recommendations";
        String OUTPUT_REVIEWS = "output-reviews";

        @Output(OUTPUT_PRODUCTS)
        MessageChannel outputProducts();

        @Output(OUTPUT_RECOMMENDATIONS)
        MessageChannel outputRecommendations();

        @Output(OUTPUT_REVIEWS)
        MessageChannel outputReviews();
    }

    @Autowired
    public ProductCompositeIntegration(
            WebClient.Builder webClient,
            ObjectMapper mapper,
            MessageSources messageSources,

            @Value("${app.product-service.host}") String productServiceHost,
            @Value("${app.product-service.port}") int productServicePort,

            @Value("${app.recommendation-service.host}") String recommendationServiceHost,
            @Value("${app.recommendation-service.port}") int recommendationServicePort,

            @Value("${app.review-service.host}") String reviewServiceHost,
            @Value("${app.review-service.port}") int reviewServicePort
    ) {
        this.webClient = webClient.build();
        this.mapper = mapper;
        this.messageSources = messageSources;

        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort;
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort;
        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort;
    }

    // about Product
    @Override
    public Product createProduct(Product body) {
        messageSources.outputProducts()
                .send(MessageBuilder.withPayload(
                        new Event(Event.Type.CREATE, body.getProductId(), body)
                ).build());
        return body;
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        String url = productServiceUrl + "/product/" + productId;
        log.debug("Will call the getProduct API on URL: {}", url);
        return webClient.get().uri(url)
                .retrieve()
                .bodyToMono(Product.class)
                .log()
                // HTTP 계층에서 발생한 예외를 자체 예외(예: NotFoundException, InvalidInput Exception)로 변경
                .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    @Override
    public void deleteProduct(int productId) {
        messageSources.outputProducts()
                .send(MessageBuilder.withPayload(
                        new Event(Event.Type.DELETE, productId, null)
                ).build());
    }

    // about Recommendation
    @Override
    public Recommendation createRecommendation(Recommendation body) {
        messageSources.outputRecommendations().send(MessageBuilder.withPayload(new Event(Event.Type.CREATE, body.getProductId(), body)).build());
        return body;
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

        String url = recommendationServiceUrl + "/recommendation?productId=" + productId;

        log.debug("Will call the getRecommendations API on URL: {}", url);

        // product 서비스를 성공적으로 호출하고 review나 recommendation API 호출에 실패했을 때는 전체 요청이 실패한 것으로 처리하지 않는다
        // Return an empty result if something goes wrong to make it possible for the composite service to return partial responses
        return webClient.get().uri(url)
                .retrieve()
                .bodyToFlux(Recommendation.class)
                .log()
                // 예외를 전파하는 대신 가능한 많은 정보를 호출자에게 돌려주고자 onErrorResume(error-> empty()) 메서드를 사용
                .onErrorResume(error -> Flux.empty());
    }

    @Override
    public void deleteRecommendations(int productId) {
        messageSources.outputRecommendations().send(MessageBuilder.withPayload(new Event(Event.Type.DELETE, productId, null)).build());
    }

    // about Review
    @Override
    public Review createReview(Review body) {
        messageSources.outputReviews().send(MessageBuilder.withPayload(new Event(Event.Type.CREATE, body.getProductId(), body)).build());
        return body;
    }

    @Override
    public Flux<Review> getReviews(int productId) {

        String url = reviewServiceUrl + "/review?productId=" + productId;

        log.debug("Will call the getReviews API on URL: {}", url);

        // Return an empty result if something goes wrong to make it possible for the composite service to return partial responses
        return webClient.get().uri(url).retrieve().bodyToFlux(Review.class).log().onErrorResume(error -> Flux.empty());

    }

    @Override
    public void deleteReviews(int productId) {
        messageSources.outputReviews().send(MessageBuilder.withPayload(new Event(Event.Type.DELETE, productId, null)).build());
    }

    // 액추에이터 기반의 health
    // 엔드포인트는 마이크로서비스와 마이크로서비스가 의존하는 모든 의존성(데이터베이스, 메시징 시스템 등)이 정상인 경우에 UP으로 응답. HTTP 상태 코드 200을 반환.
    // 그 외 DOWN으로 응답하며, HTTP 상태 코드 500을 반환
    private Mono<Health> getHealth(String url) {
        url += "/actuator/health";
        log.debug("Will call the Health API on URL: {}", url);
        return webClient.get().uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(s -> new Health.Builder().up().build())
                .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
                .log();
    }

    public Mono<Health> getProductHealth() {
        return getHealth(productServiceUrl);
    }

    public Mono<Health> getRecommendationHealth() {
        return getHealth(recommendationServiceUrl);
    }

    public Mono<Health> getReviewHealth() {
        return getHealth(reviewServiceUrl);
    }

    private Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException)) {
            log.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException) ex;

        switch (wcre.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                log.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                log.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }
}
