package com.xinsulu.repository;

import com.xinsulu.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /**
     * 根据用户ID查询审计日志（分页）
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 审计日志分页列表
     */
    Page<AuditLog> findByUserIdOrderByCreatedTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据模块查询审计日志（分页）
     *
     * @param module   模块名称
     * @param pageable 分页参数
     * @return 审计日志分页列表
     */
    Page<AuditLog> findByModuleOrderByCreatedTimeDesc(String module, Pageable pageable);

    /**
     * 根据模块和操作查询
     *
     * @param module 模块
     * @param action 操作
     * @param pageable 分页参数
     * @return 审计日志分页列表
     */
    Page<AuditLog> findByModuleAndActionOrderByCreatedTimeDesc(String module, String action, Pageable pageable);

    /**
     * 根据时间范围查询审计日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 审计日志分页列表
     */
    Page<AuditLog> findByCreatedTimeBetweenOrderByCreatedTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据是否成功查询
     *
     * @param isSuccess 是否成功
     * @param pageable  分页参数
     * @return 审计日志分页列表
     */
    Page<AuditLog> findByIsSuccessOrderByCreatedTimeDesc(Boolean isSuccess, Pageable pageable);

    /**
     * 多条件组合查询
     *
     * @param userId    用户ID（可选）
     * @param module    模块（可选）
     * @param action    操作（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param pageable  分页参数
     * @return 审计日志分页列表
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:startTime IS NULL OR a.createdTime >= :startTime) AND " +
           "(:endTime IS NULL OR a.createdTime <= :endTime) " +
           "ORDER BY a.createdTime DESC")
    Page<AuditLog> findByConditions(
            @Param("userId") Long userId,
            @Param("module") String module,
            @Param("action") String action,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 清理指定日期之前的日志
     *
     * @param beforeTime 时间点
     * @return 删除数量
     */
    long deleteByCreatedTimeBefore(LocalDateTime beforeTime);

    /**
     * 按操作类型分组统计数量
     *
     * @return 操作类型和数量的数组列表
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.deleted = 0 GROUP BY a.action")
    List<Object[]> countGroupByOperationType();

    /**
     * 按操作人分组统计数量（限制返回条数）
     *
     * @param limit 返回条数限制
     * @return 用户名和数量的数组列表
     */
    @Query(value = "SELECT username, COUNT(*) FROM audit_log WHERE deleted = 0 " +
            "GROUP BY username ORDER BY COUNT(*) DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> countGroupByOperatorName(@Param("limit") Integer limit);
}
