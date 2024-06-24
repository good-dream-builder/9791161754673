package com.songko.recommendationservice.services;

import com.mongodb.DuplicateKeyException;
import com.songko.api.core.recommendation.Recommendation;
import com.songko.api.core.recommendation.RecommendationService;
import com.songko.recommendationservice.persistence.RecommendationEntity;
import com.songko.recommendationservice.persistence.RecommendationRepository;
import com.songko.util.exceptions.InvalidInputException;
import com.songko.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
public class RecommendationServiceImpl implements RecommendationService {
    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;
    private final ServiceUtil serviceUtil;

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        if (body.getProductId() < 1) throw new InvalidInputException("Invalid productId: " + body.getProductId());

        RecommendationEntity entity = mapper.dtoToEntity(body);
        Mono<Recommendation> newEntity = repository.save(entity)
                .log()
                .onErrorMap(
                        DuplicateKeyException.class,
                        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId()))
                .map(e -> mapper.entityToDto(e));

        return newEntity.block();
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        return repository.findByProductId(productId)
                .log()
                .map(e -> mapper.entityToDto(e))
                .map(e -> {
                    e.setServiceAddress(serviceUtil.getServiceAddress());
                    return e;
                });
    }

    @Override
    public void deleteRecommendations(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        log.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId)).block();
    }
}
