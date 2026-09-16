package com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    List<Invitation> findByGroup_Id(Long groupId);
    List<Invitation> findByUser_Id(Long userId);
    List<Invitation> findByUser_IdAndGroup_Id(Long userId, Long groupId);
    boolean existsByUser_IdAndGroup_Id(Long userId, Long groupId);
}
