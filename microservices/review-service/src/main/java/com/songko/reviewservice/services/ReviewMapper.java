package com.songko.reviewservice.services;

import com.songko.api.core.review.Review;
import com.songko.reviewservice.persistence.ReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReviewMapper {
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Review entityToDto(ReviewEntity entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    ReviewEntity dtoToEntity(Review dto);

    List<Review> entityListToDtoList(List<ReviewEntity> entity);
    List<ReviewEntity> dtoListToEntityList(List<Review> dto);
}
