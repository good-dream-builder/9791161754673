package com.songko.recommendationservice.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;

public interface RecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {
    // List -> Flux로 교체
    Flux<RecommendationEntity> findByProductId(int productId);
}
