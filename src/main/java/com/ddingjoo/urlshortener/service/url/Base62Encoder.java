package com.ddingjoo.urlshortener.service.url;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {
	
	private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	private static final int BASE = ALPHABET.length;
	
	public String encode(long value) {
		if (value < 0) {
			throw new IllegalArgumentException("Base62 value must be non-negative");
		}
		
		if (value == 0) {
			return String.valueOf(ALPHABET[0]);
		}
		
		StringBuilder builder = new StringBuilder();
		long remaining = value;
		while (remaining > 0) {
			int index = (int) (remaining % BASE);
			builder.append(ALPHABET[index]);
			remaining /= BASE;
		}
		return builder.reverse().toString();
	}
	
	public long decode(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Base62 value must not be blank");
		}
		
		long result = 0L;
		for (char character : value.toCharArray()) {
			result = (result * BASE) + alphabetIndex(character);
		}
		return result;
	}
	
	private int alphabetIndex(char character) {
		if (character >= '0' && character <= '9') {
			return character - '0';
		}
		if (character >= 'a' && character <= 'z') {
			return 10 + (character - 'a');
		}
		if (character >= 'A' && character <= 'Z') {
			return 36 + (character - 'A');
		}
		throw new IllegalArgumentException("Unsupported Base62 character: " + character);
	}
}
