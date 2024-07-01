package com.songko.productcompositeservice.services;

import com.songko.api.composite.product.*;
import com.songko.api.core.product.Product;
import com.songko.api.core.recommendation.Recommendation;
import com.songko.api.core.review.Review;
import com.songko.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {
//    private final SecurityContext nullSC = new SecurityContextImpl();
    private final ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;

    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

//    @Override
//    public Mono<Void> createCompositeProduct(ProductAggregate body) {
//        return ReactiveSecurityContextHolder.getContext()
//                .doOnSuccess(sc -> internalCreateCompositeProduct(sc, body)).then();
//
//    }

    @Override
    public void createCompositeProduct(ProductAggregate body) {
        try {
//            logAuthorizationInfo(sc);

            log.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            integration.createProduct(product);

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
                    integration.createRecommendation(recommendation);
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
                    integration.createReview(review);
                });
            }

            log.debug("createCompositeProduct: composite entites created for productId: {}", body.getProductId());
        } catch (RuntimeException re) {
            log.warn("createCompositeProduct failed", re);
            throw re;
        }
    }

    @Override
    public Mono<ProductAggregate> getCompositeProduct(int productId) {
        // zip 메서드는 다수의 병렬 요청을 처리. 처리가 완료되면 하나로 압축.
        return Mono.zip(
                        // 첫 번째 매개 변수 : 배열로 값을 받는 람다 함수
                        values -> createProductAggregate(
//                                (SecurityContext) values[0],
                                (Product) values[0],
                                (List<Recommendation>) values[1],
                                (List<Review>) values[2],
                                serviceUtil.getServiceAddress()),
                        // 두 번째 이후 매개 변수 : 병렬로 호출할 요청의 목록. 각 요청은 Mono 객체를 반환.
//                        ReactiveSecurityContextHolder.getContext().defaultIfEmpty(nullSC),
                        integration.getProduct(productId),
                        integration.getRecommendations(productId).collectList(),
                        integration.getReviews(productId).collectList()
                )
                .doOnError(ex -> log.warn("getCompositeProduct failed: {}", ex.toString()))
                .log();
    }

//    @Override
//    public Mono<Void> deleteCompositeProduct(int productId) {
//        return ReactiveSecurityContextHolder.getContext()
//                .doOnSuccess(sc -> internalDeleteCompositeProduct(sc, productId)).then();
//    }

    @Override
    public void deleteCompositeProduct(int productId) {
        try {
//            logAuthorizationInfo(sc);
            log.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

            integration.deleteProduct(productId);
            integration.deleteRecommendations(productId);
            integration.deleteReviews(productId);

            log.debug("deleteCompositeProduct: aggregate entities deleted for productId: {}", productId);

        } catch (RuntimeException re) {
            log.warn("deleteCompositeProduct failed: {}", re.toString());
            throw re;
        }
    }

    private ProductAggregate createProductAggregate(
            Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {
//        logAuthorizationInfo(sc);

        // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
                recommendations.stream()
                        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
                        .collect(Collectors.toList());

        // 3. Copy summary review info, if available
        List<ReviewSummary> reviewSummaries = (reviews == null) ? null :
                reviews.stream()
                        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
                        .collect(Collectors.toList());

        // 4. Create info regarding the involved microservices addresses
        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }

//    private void logAuthorizationInfo(SecurityContext sc) {
//        if (sc != null && sc.getAuthentication() != null && sc.getAuthentication() instanceof JwtAuthenticationToken) {
//            Jwt jwt = ((JwtAuthenticationToken) sc.getAuthentication()).getToken();
//            logAuthorizationInfo(jwt);
//        } else {
//            log.warn("No JWT based Authentication supplied, running tests are we?");
//        }
//    }
//
//    /**
//     * API를 호출할 때마다 관련된 JWT 접근 토큰을 기록
//     * @param jwt
//     */
//    private void logAuthorizationInfo(Jwt jwt) {
//        if (jwt == null) {
//            log.warn("No JWT supplied, running tests are we?");
//        } else {
//            if (log.isDebugEnabled()) {
//                URL issuer = jwt.getIssuer();
//                List<String> audience = jwt.getAudience();
//                Object subject = jwt.getClaims().get("sub");
//                Object scopes = jwt.getClaims().get("scope");
//                Object expires = jwt.getClaims().get("exp");
//
//                log.debug("Authorization info: Subject: {}, scopes: {}, expires {}: issuer: {}, audience: {}", subject, scopes, expires, issuer, audience);
//            }
//        }
//    }

}