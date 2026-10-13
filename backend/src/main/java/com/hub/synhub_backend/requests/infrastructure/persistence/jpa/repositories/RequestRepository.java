package com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByTaskId(Long taskId);
    void deleteByTaskId(Long taskId);
    List<Request> findByTask_Profile_Group_Id(Long groupId);

    String GROUP_REQUESTS = "select r from Request r where r.task.profile.group.id = :groupId"
            + " and (:status is null or r.requestStatus = :status) and (:type is null or r.requestType = :type)";

    /** Requests de un grupo, opcionalmente filtradas, de la más nueva a la más antigua (filtro en SQL, AUDIT PERF-01). */
    @Query(GROUP_REQUESTS + " order by r.id desc")
    List<Request> findByGroupFiltered(@Param("groupId") Long groupId, @Param("status") RequestStatus status, @Param("type") RequestType type);

    @Query(value = GROUP_REQUESTS + " order by r.id desc",
           countQuery = "select count(r) from Request r where r.task.profile.group.id = :groupId"
                   + " and (:status is null or r.requestStatus = :status) and (:type is null or r.requestType = :type)")
    Page<Request> findByGroupFiltered(@Param("groupId") Long groupId, @Param("status") RequestStatus status, @Param("type") RequestType type, Pageable pageable);
    boolean existsByTask_IdAndRequestTypeAndRequestStatus(Long taskId, RequestType requestType, RequestStatus requestStatus);
}
