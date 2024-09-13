package com.starter.place.config.payload;

import com.querydsl.core.types.OrderSpecifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface OrderByParameterDTO {

    List<OrderSpecifier<?>> getSpecifiers();

    static OrderSpecifier<?>[] convertSpecifiers(List<? extends OrderByParameterDTO> orderByList, List<OrderSpecifier<?>> defaultSpecifiers) {
        return Optional.ofNullable(orderByList)
            .map(list -> list.stream()
                .map(OrderByParameterDTO::getSpecifiers)
                .reduce(new ArrayList<>(), (a, b) -> {
                a.addAll(b); return a;
            }))
            .orElse(defaultSpecifiers)
            .toArray(OrderSpecifier<?>[]::new);
    }

}
