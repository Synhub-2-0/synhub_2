package com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupProfileRepository extends JpaRepository<GroupProfile, Long> {

    Optional<GroupProfile> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    List<GroupProfile> findByUser_Id(Long userId);

    /** Membresías del usuario con ese rol: con GROUP_LEADER, los grupos que lidera (activos y bloqueados). */
    List<GroupProfile> findByUser_IdAndRoleInGroup(Long userId, GroupRole roleInGroup);

    List<GroupProfile> findByGroup_Id(Long groupId);
}
