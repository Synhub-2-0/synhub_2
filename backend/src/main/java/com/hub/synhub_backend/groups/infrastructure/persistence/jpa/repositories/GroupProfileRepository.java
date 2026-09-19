package com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupProfileRepository extends JpaRepository<GroupProfile, Long> {

    Optional<GroupProfile> findByGroup_IdAndUser_Id(Long groupId, Long userId);
    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);
    List<GroupProfile> findByUser_Id(Long userId);
    List<GroupProfile> findByGroup_Id(Long groupId);
}
