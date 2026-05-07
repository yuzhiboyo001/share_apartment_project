package com.example.apartment.repository;

import com.example.apartment.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // 根据公寓ID查询所有事务
    List<Task> findByApartmentId(Long apartmentId);

    // 根据公寓ID分页查询事务
    Page<Task> findByApartmentId(Long apartmentId, Pageable pageable);

    // 根据责任人ID查询事务
    List<Task> findByAssigneeId(Long assigneeId);

    // 根据责任人ID分页查询事务
    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    // 根据发起人ID查询事务
    List<Task> findByCreatorId(Long creatorId);

    // 根据状态查询事务
    List<Task> findByStatus(Integer status);

    // 根据公寓ID和状态查询事务
    List<Task> findByApartmentIdAndStatus(Long apartmentId, Integer status);

    // 根据关键词搜索事务（标题或描述）
    @Query("SELECT t FROM Task t WHERE t.apartmentId = :apartmentId AND " +
            "(:keyword IS NULL OR :keyword = '' OR t.title LIKE %:keyword% OR t.description LIKE %:keyword%)")
    Page<Task> searchByKeyword(@Param("apartmentId") Long apartmentId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    // 查询即将到期的事务
    @Query("SELECT t FROM Task t WHERE t.deadline BETWEEN :now AND :deadline AND t.status IN (0, 1)")
    List<Task> findUpcomingTasks(@Param("now") LocalDateTime now,
                                 @Param("deadline") LocalDateTime deadline);

    // 统计公寓内各状态的事务数量
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.apartmentId = :apartmentId GROUP BY t.status")
    List<Object[]> countByStatus(@Param("apartmentId") Long apartmentId);

    // 更新事务状态
    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.status = :status WHERE t.id = :taskId")
    int updateStatus(@Param("taskId") Long taskId, @Param("status") Integer status);

    // 批量删除已完成的事务
    @Modifying
    @Transactional
    @Query("DELETE FROM Task t WHERE t.apartmentId = :apartmentId AND t.status = 2")
    int deleteCompletedTasks(@Param("apartmentId") Long apartmentId);

    // 查询指定责任人的待处理事务
    @Query("SELECT t FROM Task t WHERE t.assigneeId = :assigneeId AND t.status IN (0, 1) ORDER BY t.deadline ASC")
    List<Task> findPendingTasksByAssignee(@Param("assigneeId") Long assigneeId);
}