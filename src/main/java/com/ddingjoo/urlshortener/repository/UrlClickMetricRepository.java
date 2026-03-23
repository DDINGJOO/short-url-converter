package com.ddingjoo.urlshortener.repository;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import com.ddingjoo.urlshortener.domain.UrlClickMetric;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlClickMetricRepository extends JpaRepository<UrlClickMetric, Long> {

    Optional<UrlClickMetric> findByUrlIdAndGranularityAndBucketStart(
            Long urlId,
            ClickMetricGranularity granularity,
            OffsetDateTime bucketStart
    );

    List<UrlClickMetric> findAllByUrlIdAndGranularityAndBucketStartBetweenOrderByBucketStartAsc(
            Long urlId,
            ClickMetricGranularity granularity,
            OffsetDateTime from,
            OffsetDateTime to
    );
}

