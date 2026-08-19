package com.xinsulu.repository;

import com.xinsulu.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 上传文件数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long>, JpaSpecificationExecutor<UploadedFile> {

    /**
     * 根据企业ID查询上传的文件列表（分页）
     *
     * @param enterpriseId 企业ID
     * @param pageable     分页参数
     * @return 文件分页列表
     */
    List<UploadedFile> findByOcrTaskId(Long ocrTaskId);

    UploadedFile findByMd5Hash(String md5Hash);

    List<UploadedFile> findByArchiveEnterpriseIdOrderByCreatedTimeDesc(Long enterpriseId);

    Optional<UploadedFile> findFirstByArchiveIdOrderByCreatedTimeDesc(Long archiveId);

    List<UploadedFile> findByArchiveId(Long archiveId);
}
