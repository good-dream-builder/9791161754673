package com.songko.recommendationservice.services;

import com.songko.api.core.recommendation.Recommendation;
import com.songko.recommendationservice.persistence.RecommendationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {
    @Mappings({
            @Mapping(target = "rate", source = "entity.rating"),
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Recommendation entityToDto(RecommendationEntity entity);

    @Mappings({
            @Mapping(target = "rating", source = "dto.rate"),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    RecommendationEntity dtoToEntity(Recommendation dto);

    List<Recommendation> entityListToDtoList(List<RecommendationEntity> entity);

    List<RecommendationEntity> dtoListToEntityList(List<Recommendation> dto);
}
