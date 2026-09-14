package com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaderRepository extends JpaRepository<Leader, Long> {
}
