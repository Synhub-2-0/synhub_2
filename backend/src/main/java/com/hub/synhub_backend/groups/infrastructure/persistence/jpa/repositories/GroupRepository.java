package com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByLeader_Id(Long leaderId);

    boolean existsByCode(GroupCode code);

    Optional<Group> findByCode(GroupCode code);
}
