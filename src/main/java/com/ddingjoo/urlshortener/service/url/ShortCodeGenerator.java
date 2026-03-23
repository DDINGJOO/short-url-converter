package com.ddingjoo.urlshortener.service.url;

import com.ddingjoo.urlshortener.config.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    private final Base62Encoder base62Encoder;
    private final long obfuscationKey;

    public ShortCodeGenerator(Base62Encoder base62Encoder, AppProperties appProperties) {
        this.base62Encoder = base62Encoder;
        this.obfuscationKey = appProperties.shortCodeObfuscationKey();
    }

    public String generate(Long id) {
        long obfuscatedId = id ^ obfuscationKey;
        return base62Encoder.encode(obfuscatedId);
    }

    public long decode(String shortCode) {
        long obfuscatedId = base62Encoder.decode(shortCode);
        return obfuscatedId ^ obfuscationKey;
    }
}
