package com.fiap.workshop.shared.application;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

	public int totalPages() {
		return (int) Math.ceil((double) totalElements / size);
	}

	public <R> PageResult<R> map(Function<T, R> mapper) {
		return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
	}
}
