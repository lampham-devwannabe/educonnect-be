package com.sep.educonnect.utils;

import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public record QueryRule<T>(Predicate<T> predicate, Function<T, Query> function) {

    public static <T> QueryRule<T> of(Predicate<T> predicate, Function<T, Query> function) {
        return new QueryRule<>(predicate, function);
    }
    public Optional<Query> build(T params) {
        return Optional.of(params)
                .filter(this.predicate())
                .map(this.function());
    }
}
