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

@Slf4j
@RequiredArgsConstructor
@RestController
public class ProductServiceImpl implements ProductService {
    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    public Product createProduct(Product body) {
        try {
            ProductEntity entity = mapper.dtoToEntity(body);
            ProductEntity newEntity = repository.save(entity);

            log.debug("createProduct: entity created for productId: {}", body.getProductId());
            return mapper.entityToDto(newEntity);

        } catch (DuplicateKeyException dke) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId());
        }
    }

    @Override
    public Product getProduct(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        ProductEntity entity = repository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));
                
        Product response = mapper.entityToDto(entity);
        response.setServiceAddress(serviceUtil.getServiceAddress());

        log.debug("getProduct: found productId: {}", response.getProductId());

        return response;
    }

    @Override
    public void deleteProduct(int productId) {
        log.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        repository.findByProductId(productId).ifPresent(entity -> repository.delete(entity));
    }
}