package com.songko.productservice.services;

import com.songko.api.core.product.Product;
import com.songko.productservice.persistence.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Product entityToDto(ProductEntity entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    ProductEntity dtoToEntity(Product dto);
}
