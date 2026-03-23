package com.ddingjoo.urlshortener.service.click;

import java.util.Set;

public interface ClickCountService {

    void increment(String shortCode);

    ClickBufferState getBufferedState(String shortCode);

    Set<String> findTrackedShortCodes();

    ClickSyncBatch reserveBufferedClicks(String shortCode);

    boolean acknowledgeBufferedClicks(String shortCode, String token);
}
