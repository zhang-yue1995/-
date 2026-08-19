package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.common.exception.BusinessException;
import com.xinsulu.config.ApiAuthInterceptor;
import com.xinsulu.dto.PageResponse;
import com.xinsulu.entity.FinancialReportArchive;
import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.entity.OcrTask;
import com.xinsulu.entity.UploadedFile;
import com.xinsulu.entity.User;
import com.xinsulu.repository.OcrFieldResultRepository;
import com.xinsulu.repository.OcrTaskRepository;
import com.xinsulu.repository.UploadedFileRepository;
import com.xinsulu.repository.FinancialReportArchiveRepository;
import com.xinsulu.repository.UserRepository;
import com.xinsulu.service.FinancialReportService;
import com.xinsulu.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;

/**
 * 文件上传控制器
 * 提供文件上传和信息查询功能
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@Api(tags = "文件管理")
public class FileController {

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private FinancialReportArchiveRepository archiveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OcrTaskRepository ocrTaskRepository;

    @Autowired
    private OcrFieldResultRepository ocrFieldResultRepository;

    @Autowired
    private FinancialReportService financialReportService;

    @Value("${xinsulu.storage.upload-dir:./uploads}")
    private String uploadDirectory;

    /**
     * 文件上传
     * 支持图片（JPG、PNG）、PDF和Excel（XLS、XLSX）格式
     *
     * @param file 上传的文件
     * @return 文件信息
     */
    @PostMapping("/upload")
    @ApiOperation(value = "文件上传", notes = "上传财务报表图片、PDF或Excel文件")
    public ApiResponse<UploadedFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(ApiAuthInterceptor.CURRENT_USER_ATTRIBUTE) UserVO currentUser) {
        log.info("文件上传请求：fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        Path target = null;
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("上传文件不能为空");
            }

            String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            int extensionIndex = originalFilename.lastIndexOf('.');
            if (originalFilename.isEmpty() || extensionIndex < 0) {
                throw new RuntimeException("文件名或扩展名无效");
            }

            String extension = originalFilename.substring(extensionIndex).toLowerCase();
            if (!isAllowedExtension(extension)) {
                throw new RuntimeException("不支持的文件格式，仅支持 JPG、PNG、PDF、XLS、XLSX");
            }

            if (file.getSize() > 30L * 1024 * 1024) {
                throw new RuntimeException("文件大小不能超过30MB");
            }

            Path storageRoot = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(storageRoot);
            String storedFilename = UUID.randomUUID() + extension;
            target = storageRoot.resolve(storedFilename).normalize();
            if (!storageRoot.equals(target.getParent())) {
                throw new RuntimeException("文件存储路径无效");
            }
            // MultipartFile 的底层临时文件在 Windows 上会被输入流锁定。
            // 必须在控制器返回前关闭输入流，否则 Tomcat 清理 multipart 临时文件时会失败。
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            UploadedFile uploadedFile = new UploadedFile();
            uploadedFile.setOriginalFilename(originalFilename);
            uploadedFile.setStoredFilename(storedFilename);
            uploadedFile.setFileSize(file.getSize());
            uploadedFile.setFileType(extension.replace(".", "").toUpperCase());
            uploadedFile.setMimeType(file.getContentType());
            uploadedFile.setFilePath(storedFilename);
            uploadedFile.setUploadStatus("UPLOADED");
            User uploader = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new BusinessException(401, "登录用户不存在，请重新登录"));
            uploadedFile.setUploadedBy(uploader);
            uploadedFile.setCreatedTime(LocalDateTime.now());
            uploadedFile.setUpdatedTime(LocalDateTime.now());
            uploadedFile.setDeleted(0);

            uploadedFile = uploadedFileRepository.save(uploadedFile);

            log.info("文件上传成功：fileId={}, fileName={}", uploadedFile.getId(), originalFilename);
            return ApiResponse.success(uploadedFile);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            if (target != null) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException cleanupException) {
                    log.warn("清理上传失败文件时出错：{}", target, cleanupException);
                }
            }
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取文件信息
     *
     * @param id 文件ID
     * @return 文件信息
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取文件信息", notes = "根据ID查询上传文件的详细信息")
    public ApiResponse<UploadedFile> getFile(@PathVariable Long id) {
        log.info("查询文件信息：fileId={}", id);
        UploadedFile file = uploadedFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文件不存在"));
        return ApiResponse.success(file);
    }

    @GetMapping
    @Transactional(readOnly = true)
    @ApiOperation(value = "我的上传记录", notes = "按上传时间倒序查询，支持日期、企业名称和待复核状态筛选")
    public ApiResponse<PageResponse<Map<String, Object>>> listUploads(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String uploadDate,
            @RequestParam(required = false) String enterpriseName,
            @RequestParam(required = false) String status,
            @RequestAttribute(ApiAuthInterceptor.CURRENT_USER_ATTRIBUTE) UserVO currentUser) {
        int safePage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safeSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        Specification<FinancialReportArchive> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            predicates.add(cb.equal(root.get("enterprise").get("deleted"), 0));
            predicates.add(cb.equal(root.get("uploadedBy").get("id"), currentUser.getId()));
            if (StringUtils.hasText(uploadDate)) {
                try {
                    LocalDate date = LocalDate.parse(uploadDate.trim());
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), date.atStartOfDay()));
                    predicates.add(cb.lessThan(root.get("createdTime"), date.plusDays(1).atStartOfDay()));
                } catch (Exception exception) {
                    throw new BusinessException("上传日期格式应为 yyyy-MM-dd");
                }
            }
            if (StringUtils.hasText(enterpriseName)) {
                predicates.add(cb.like(root.get("enterprise").get("enterpriseName"),
                        "%" + enterpriseName.trim() + "%"));
            }
            if (StringUtils.hasText(status)) {
                if ("PENDING".equalsIgnoreCase(status.trim())) {
                    predicates.add(root.get("filingStatus").in("DRAFT", "REVIEWED", "REJECTED"));
                } else {
                    predicates.add(cb.equal(root.get("filingStatus"), status.trim()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<FinancialReportArchive> page = archiveRepository.findAll(spec, PageRequest.of(
                safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdTime")));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<Map<String, Object>> records = new ArrayList<>();
        for (FinancialReportArchive archive : page.getContent()) {
            List<UploadedFile> files = uploadedFileRepository.findByArchiveId(archive.getId()).stream()
                    .filter(file -> Integer.valueOf(0).equals(file.getDeleted()))
                    .sorted(Comparator.comparing(UploadedFile::getCreatedTime))
                    .collect(Collectors.toList());
            if (files.isEmpty()) continue;
            UploadedFile primaryFile = files.get(0);
            LocalDateTime uploadTime = files.stream().map(UploadedFile::getCreatedTime)
                    .min(LocalDateTime::compareTo).orElse(archive.getCreatedTime());
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("fileId", primaryFile.getId());
            record.put("fileName", files.size() == 1 ? primaryFile.getOriginalFilename()
                    : "本次共上传" + files.size() + "个文件");
            record.put("fileNames", files.stream().map(UploadedFile::getOriginalFilename)
                    .collect(Collectors.toList()));
            record.put("fileCount", files.size());
            record.put("fileType", primaryFile.getFileType());
            record.put("fileSize", files.stream().mapToLong(UploadedFile::getFileSize).sum());
            record.put("uploadTime", uploadTime.format(formatter));
            record.put("ocrTaskId", primaryFile.getOcrTaskId());
            record.put("archiveId", archive.getId());
            record.put("enterpriseName", archive.getEnterprise().getEnterpriseName());
            record.put("reportPeriod", archive.getReportPeriod());
            record.put("filingStatus", archive.getFilingStatus());
            records.add(record);
        }
        return ApiResponse.success(PageResponse.of(records, page.getTotalElements(), safePage, safeSize));
    }

    @GetMapping("/archive/{archiveId}")
    @Transactional(readOnly = true)
    @ApiOperation(value = "查询报表全部原件", notes = "多图识别时返回同一次上传的全部图片/文件")
    public ApiResponse<List<Map<String, Object>>> listArchiveFiles(@PathVariable Long archiveId) {
        FinancialReportArchive archive = archiveRepository.findById(archiveId)
                .filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                .orElseThrow(() -> new BusinessException(404, "报表档案不存在或已删除"));
        List<Map<String, Object>> result = new ArrayList<>();
        uploadedFileRepository.findByArchiveId(archive.getId()).stream()
                .filter(file -> Integer.valueOf(0).equals(file.getDeleted()))
                .sorted(Comparator.comparing(UploadedFile::getCreatedTime))
                .forEach(file -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("fileId", file.getId());
                    item.put("fileName", file.getOriginalFilename());
                    item.put("fileType", file.getFileType());
                    item.put("mimeType", file.getMimeType());
                    item.put("fileSize", file.getFileSize());
                    result.add(item);
                });
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @ApiOperation(value = "删除上传记录", notes = "删除本人上传记录，并同步隐藏其报表和 OCR 结果")
    public ApiResponse<Void> deleteUpload(
            @PathVariable Long id,
            @RequestAttribute(ApiAuthInterceptor.CURRENT_USER_ATTRIBUTE) UserVO currentUser) {
        UploadedFile file = uploadedFileRepository.findById(id)
                .filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                .orElseThrow(() -> new BusinessException(404, "上传记录不存在或已删除"));
        boolean owner = file.getUploadedBy() != null && currentUser.getId().equals(file.getUploadedBy().getId());
        if (!owner && !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new BusinessException(403, "无权删除该上传记录");
        }
        if (file.getArchive() != null && Integer.valueOf(0).equals(file.getArchive().getDeleted())) {
            financialReportService.deleteArchive(file.getArchive().getId());
        } else {
            LocalDateTime now = LocalDateTime.now();
            file.setDeleted(1);
            file.setUpdatedTime(now);
            uploadedFileRepository.save(file);
            for (OcrTask task : ocrTaskRepository.findByFileId(file.getId())) {
                task.setDeleted(1);
                task.setUpdatedTime(now);
                ocrTaskRepository.save(task);
                List<OcrFieldResult> fields = ocrFieldResultRepository
                        .findByOcrTaskIdOrderByFieldCodeAsc(task.getId());
                for (OcrFieldResult field : fields) {
                    field.setDeleted(1);
                    field.setUpdatedTime(now);
                }
                ocrFieldResultRepository.saveAll(fields);
            }
        }
        return ApiResponse.success();
    }

    @GetMapping("/{id}/content")
    @ApiOperation(value = "下载文件内容", notes = "下载已上传的原始报表文件")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        UploadedFile file = uploadedFileRepository.findById(id)
                .filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                .orElseThrow(() -> new RuntimeException("文件不存在"));
        Path storageRoot = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        Path filePath = storageRoot.resolve(file.getFilePath()).normalize();
        if (!storageRoot.equals(filePath.getParent())) {
            throw new RuntimeException("文件存储路径无效");
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("文件内容不存在");
        }
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.getMimeType() != null) {
            try {
                mediaType = MediaType.parseMediaType(file.getMimeType());
            } catch (IllegalArgumentException ignored) {
                log.debug("无法解析文件媒体类型：{}", file.getMimeType());
            }
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getOriginalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    /**
     * 检查是否为允许的文件扩展名
     */
    private boolean isAllowedExtension(String extension) {
        return ".jpg".equals(extension) || ".jpeg".equals(extension)
                || ".png".equals(extension) || ".pdf".equals(extension)
                || ".xls".equals(extension) || ".xlsx".equals(extension);
    }
}
