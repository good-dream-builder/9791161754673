package com.songko.reviewservice.services;

import com.songko.api.core.review.Review;
import com.songko.api.core.review.ReviewService;
import com.songko.reviewservice.persistence.ReviewEntity;
import com.songko.reviewservice.persistence.ReviewRepository;
import com.songko.util.exceptions.InvalidInputException;
import com.songko.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

@Slf4j
@RequiredArgsConstructor
@RestController
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository repository;
    private final ReviewMapper mapper;
    private final ServiceUtil serviceUtil;

    // 스레드 풀에서 블로킹 코드를 실행
    private final Scheduler scheduler;

    @Override
    public Review createReview(Review body) {
        try {
            ReviewEntity entity = mapper.dtoToEntity(body);
            ReviewEntity newEntity = repository.save(entity);

            log.debug("createReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
            return mapper.entityToDto(newEntity);

        } catch (DataIntegrityViolationException dive) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Review Id:" + body.getReviewId());
        }
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        log.info("Will get reviews for product with id={}", productId);

        //  스레드 풀의 스레드에서 블로킹 코드를 실행
        return asyncFlux(
                () -> Flux.fromIterable(getByProductId(productId))
                        .log(null, Level.FINE)
        );
    }

    protected List<Review> getByProductId(int productId) {
        // 블로킹 코드
        List<ReviewEntity> entityList = repository.findByProductId(productId);
        List<Review> list = mapper.entityListToDtoList(entityList);
        list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));
        log.debug("getReviews: response size: {}", list.size());

        return list;
    }


    private <T> Flux<T> asyncFlux(Supplier<Publisher<T>> publisherSupplier) {
        return Flux.defer(publisherSupplier).subscribeOn(scheduler);
    }

    @Override
    public void deleteReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        log.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
