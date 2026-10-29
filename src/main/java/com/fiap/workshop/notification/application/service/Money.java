package com.fiap.workshop.notification.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {

	private Money() {
	}

	static String format(BigDecimal amount) {
		return "BRL " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}
}
