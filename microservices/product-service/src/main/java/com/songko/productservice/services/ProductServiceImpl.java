package com.songko.productservice.services;

import com.mongodb.DuplicateKeyException;
import com.songko.api.core.product.Product;
import com.songko.api.core.product.ProductService;
import com.songko.productservice.persistence.ProductEntity;
import com.songko.productservice.persistence.ProductRepository;
import com.songko.util.exceptions.InvalidInputException;
import com.songko.util.exceptions.NotFoundException;
import com.songko.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
public class ProductServiceImpl implements ProductService {
    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    public Product createProduct(Product body) {
        if (body.getProductId() < 1) throw new InvalidInputException("Invalid productId: " + body.getProductId());

        ProductEntity entity = mapper.dtoToEntity(body);
        Mono<Product> newEntity = repository.save(entity)
                .log()
                .onErrorMap(
                        DuplicateKeyException.class,
                        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
                .map(e -> mapper.entityToDto(e));

        return newEntity.block();
    }

    /**
     * 서비스 요청을 받으면 웹플럭스 프레임워크에 의해 트러거.
     * 처리에 대한 선언을 하는 것이지, 직접 트리거 하지 않음.
     *
     * @param productId
     * @return Mono 객체 반환.
     */
    @Override
    public Mono<Product> getProduct(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        return repository.findByProductId(productId)
                .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
                .log()
                .map(e -> mapper.entityToDto(e))
                .map(e -> {
                    e.setServiceAddress(serviceUtil.getServiceAddress());
                    return e;
                });
    }

    @Override
    public void deleteProduct(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        log.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        repository.findByProductId(productId).log().map(e -> repository.delete(e)).flatMap(e -> e).block();
    }
}