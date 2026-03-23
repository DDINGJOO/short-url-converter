package com.ddingjoo.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddingjoo.urlshortener.service.url.Base62Encoder;
import org.junit.jupiter.api.Test;

class Base62EncoderTest {

    private final Base62Encoder base62Encoder = new Base62Encoder();

    @Test
    void encodesAndDecodesLongValues() {
        long value = 912344L;

        String encoded = base62Encoder.encode(value);

        assertThat(encoded).isEqualTo("3Ple");
        assertThat(base62Encoder.decode(encoded)).isEqualTo(value);
    }

    @Test
    void encodesZeroAsSingleDigit() {
        assertThat(base62Encoder.encode(0)).isEqualTo("0");
    }
}
