package com.ddingjoo.urlshortener.repository;

import com.ddingjoo.urlshortener.domain.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {
	
	Optional<Url> findByShortCode(String shortCode);
	
	List<Url> findAllByShortCodeIn(Collection<String> shortCodes);
	
	@Query(value = "SELECT nextval('urls_id_seq')", nativeQuery = true)
	Long nextId();
}
