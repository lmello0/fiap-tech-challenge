package com.fiap.workshop.shared.domain;

public record Document(String value) {

	public enum Type { CPF, CNPJ }

	public Document {
		String digits = value == null ? "" : value.replaceAll("\\D", "");
		if (digits.length() == 11 ? !isValidCpf(digits) : digits.length() != 14 || !isValidCnpj(digits)) {
			throw new DomainException("Invalid CPF/CNPJ: " + value);
		}
		value = digits;
	}

	public static Document of(String raw) {
		return new Document(raw);
	}

	public Type type() {
		return value.length() == 11 ? Type.CPF : Type.CNPJ;
	}

	private static boolean isValidCpf(String d) {
		if (allSameDigit(d)) {
			return false;
		}
		int first = checkDigit(d, 9, 10, 11);
		int second = checkDigit(d, 10, 11, 11);
		return first == d.charAt(9) - '0' && second == d.charAt(10) - '0';
	}

	private static boolean isValidCnpj(String d) {
		if (allSameDigit(d)) {
			return false;
		}
		int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
		int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
		return cnpjDigit(d, w1) == d.charAt(12) - '0' && cnpjDigit(d, w2) == d.charAt(13) - '0';
	}

	private static int checkDigit(String d, int length, int startWeight, int mod) {
		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum += (d.charAt(i) - '0') * (startWeight - i);
		}
		int r = (sum * 10) % mod;
		return r == 10 ? 0 : r;
	}

	private static int cnpjDigit(String d, int[] weights) {
		int sum = 0;
		for (int i = 0; i < weights.length; i++) {
			sum += (d.charAt(i) - '0') * weights[i];
		}
		int r = sum % 11;
		return r < 2 ? 0 : 11 - r;
	}

	private static boolean allSameDigit(String d) {
		return d.chars().allMatch(c -> c == d.charAt(0));
	}
}
