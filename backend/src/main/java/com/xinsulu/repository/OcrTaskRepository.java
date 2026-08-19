package com.xinsulu.repository;

import com.xinsulu.entity.OcrTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OCR任务数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface OcrTaskRepository extends JpaRepository<OcrTask, Long> {

    /**
     * 根据企业ID查询OCR任务列表（分页）
     *
     * @param enterpriseId 企业ID
     * @param pageable     分页参数
     * @return 任务分页列表
     */
    Page<OcrTask> findByTaskStatusOrderByCreatedTimeDesc(String status, Pageable pageable);

    /**
     * 根据文件ID查询任务
     *
     * @param fileId 文件ID
     * @return 任务列表
     */
    List<OcrTask> findByFileId(Long fileId);

    long countByTaskStatus(String status);

    long countByDeleted(Integer deleted);

    long countByTaskStatusAndDeleted(String taskStatus, Integer deleted);

    @Query("SELECT COUNT(t) FROM OcrTask t JOIN t.file f JOIN f.archive a JOIN a.enterprise e " +
            "WHERE t.deleted = 0 AND f.deleted = 0 AND a.deleted = 0 AND e.deleted = 0")
    long countArchivedActiveTasks();

    @Query("SELECT COUNT(t) FROM OcrTask t JOIN t.file f JOIN f.archive a JOIN a.enterprise e " +
            "WHERE t.deleted = 0 AND f.deleted = 0 AND a.deleted = 0 AND e.deleted = 0 " +
            "AND t.taskStatus = :status")
    long countArchivedActiveTasksByStatus(String status);

    /**
     * 根据删除标记查询任务（分页）
     *
     * @param deleted  删除标记
     * @param pageable 分页参数
     * @return 任务分页列表
     */
    Page<OcrTask> findByDeleted(Integer deleted, Pageable pageable);

    /**
     * 查询最近的任务
     *
     * @param limit 限制数量
     * @return 任务列表
     */
    @Query("SELECT t FROM OcrTask t WHERE t.deleted = 0 ORDER BY t.createdTime DESC")
    List<OcrTask> findRecentTasks(Pageable pageable);
}
