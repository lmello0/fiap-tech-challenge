package com.fiap.workshop.support;

import java.util.concurrent.ThreadLocalRandom;

public final class TestDocuments {

	private TestDocuments() {
	}

	public static String cpf() {
		int[] d = new int[11];
		do {
			for (int i = 0; i < 9; i++) {
				d[i] = ThreadLocalRandom.current().nextInt(10);
			}
		} while (java.util.Arrays.stream(d, 0, 9).distinct().count() == 1);
		d[9] = digit(d, 9, 10);
		d[10] = digit(d, 10, 11);
		StringBuilder sb = new StringBuilder();
		for (int v : d) {
			sb.append(v);
		}
		return sb.toString();
	}

	public static String plate() {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		return "" + (char) ('A' + r.nextInt(26)) + (char) ('A' + r.nextInt(26)) + (char) ('A' + r.nextInt(26))
				+ r.nextInt(10) + (char) ('A' + r.nextInt(26)) + r.nextInt(10) + r.nextInt(10);
	}

	public static String email() {
		return "user" + ThreadLocalRandom.current().nextLong(1_000_000_000L) + "@example.com";
	}

	private static int digit(int[] d, int length, int startWeight) {
		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum += d[i] * (startWeight - i);
		}
		int r = (sum * 10) % 11;
		return r == 10 ? 0 : r;
	}
}
