package com.fiap.workshop.shared.web;

import com.fiap.workshop.shared.application.PageResult;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <D, R> PageResponse<R> of(PageResult<D> result, Function<D, R> mapper) {
		PageResult<R> mapped = result.map(mapper);
		return new PageResponse<>(mapped.content(), mapped.page(), mapped.size(), mapped.totalElements(), mapped.totalPages());
	}
}
