package com.xinsulu.service.impl;

import com.xinsulu.common.exception.BusinessException;
import com.xinsulu.dto.EnterpriseDTO;
import com.xinsulu.dto.PageQueryDTO;
import com.xinsulu.dto.PageResponse;
import com.xinsulu.entity.Enterprise;
import com.xinsulu.entity.FinancialReportArchive;
import com.xinsulu.entity.FinancialHealthScore;
import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.entity.OcrTask;
import com.xinsulu.entity.UploadedFile;
import com.xinsulu.repository.EnterpriseRepository;
import com.xinsulu.repository.FinancialReportArchiveRepository;
import com.xinsulu.repository.FinancialHealthScoreRepository;
import com.xinsulu.repository.OcrFieldResultRepository;
import com.xinsulu.repository.OcrTaskRepository;
import com.xinsulu.repository.UploadedFileRepository;
import com.xinsulu.service.EnterpriseService;
import com.xinsulu.vo.EnterpriseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 企业管理服务实现类
 * 提供企业的CRUD操作、分页查询、报表历史查询等功能
 *
 * @author xinsulu-team
 */
@Slf4j
@Service
public class EnterpriseServiceImpl implements EnterpriseService {

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private FinancialReportArchiveRepository archiveRepository;

    @Autowired
    private FinancialHealthScoreRepository healthScoreRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private OcrTaskRepository ocrTaskRepository;

    @Autowired
    private OcrFieldResultRepository ocrFieldResultRepository;

    /**
     * 创建企业
     *
     * @param enterpriseDTO 企业信息
     * @return 企业详情
     */
    @Override
    @Transactional
    public EnterpriseVO create(EnterpriseDTO enterpriseDTO) {
        log.info("创建企业：name={}", enterpriseDTO.getName());

        // 统一社会信用代码是永久唯一键。软删除的企业再次录入时恢复原记录，
        // 避免向数据库插入同一信用代码而触发唯一约束。
        Enterprise enterprise = null;
        if (StringUtils.hasText(enterpriseDTO.getCreditCode())) {
            Enterprise existing = enterpriseRepository.findByEnterpriseCodeIgnoreCase(
                    enterpriseDTO.getCreditCode().trim()).orElse(null);
            if (existing != null && existing.getDeleted() == 0) {
                throw new BusinessException(409, "当前企业已归档，无需重复操作");
            }
            if (existing != null) {
                enterprise = existing;
                enterprise.setDeleted(0);
            }
        }

        if (enterprise == null) {
            enterprise = new Enterprise();
        }
        enterprise.setEnterpriseName(enterpriseDTO.getName());
        enterprise.setEnterpriseCode(StringUtils.hasText(enterpriseDTO.getCreditCode())
                ? enterpriseDTO.getCreditCode().trim().toUpperCase() : null);
        enterprise.setLegalPerson(enterpriseDTO.getLegalPerson());
        enterprise.setRegisteredCapital(enterpriseDTO.getRegisteredCapital());
        enterprise.setIndustry(enterpriseDTO.getIndustry());
        enterprise.setAddress(enterpriseDTO.getAddress());
        enterprise.setManagerName(enterpriseDTO.getManagerName());
        enterprise.setContactPhone(enterpriseDTO.getPhone());
        // 企业建档并不等于已经完成财务评分。未计算前保持为空，避免把占位值
        // 当作真实健康度展示；完成报表勾稽校验后再同步最新一期评分。
        enterprise.setRiskLevel(null);
        enterprise.setHealthScore(null);
        enterprise.setDeleted(0);

        // 设置成立日期
        if (StringUtils.hasText(enterpriseDTO.getEstablishDate())) {
            try {
                enterprise.setEstablishDate(LocalDate.parse(enterpriseDTO.getEstablishDate()));
            } catch (Exception e) {
                log.warn("日期格式解析失败：{}", enterpriseDTO.getEstablishDate());
            }
        }

        if (enterprise.getCreatedTime() == null) {
            enterprise.setCreatedTime(LocalDateTime.now());
        }
        enterprise.setUpdatedTime(LocalDateTime.now());

        enterprise = enterpriseRepository.save(enterprise);

        log.info("企业创建成功：id={}, name={}", enterprise.getId(), enterprise.getEnterpriseName());
        return convertToVO(enterprise);
    }

    /**
     * 更新企业信息
     *
     * @param id            企业ID
     * @param enterpriseDTO 企业信息
     * @return 更新后的企业详情
     */
    @Override
    @Transactional
    public EnterpriseVO update(Long id, EnterpriseDTO enterpriseDTO) {
        log.info("更新企业：id={}", id);

        Enterprise enterprise = getEnterpriseById(id);

        // 更新字段
        if (StringUtils.hasText(enterpriseDTO.getName())) {
            enterprise.setEnterpriseName(enterpriseDTO.getName());
        }
        if (StringUtils.hasText(enterpriseDTO.getCreditCode())) {
            String normalizedCode = enterpriseDTO.getCreditCode().trim().toUpperCase();
            enterpriseRepository.findByEnterpriseCodeIgnoreCase(normalizedCode)
                    .filter(existing -> !existing.getId().equals(id))
                    .filter(existing -> Integer.valueOf(0).equals(existing.getDeleted()))
                    .ifPresent(existing -> {
                        throw new BusinessException(409, "当前企业已归档，无需重复操作");
                    });
            enterprise.setEnterpriseCode(normalizedCode);
        }
        if (StringUtils.hasText(enterpriseDTO.getLegalPerson())) {
            enterprise.setLegalPerson(enterpriseDTO.getLegalPerson());
        }
        if (enterpriseDTO.getRegisteredCapital() != null) {
            enterprise.setRegisteredCapital(enterpriseDTO.getRegisteredCapital());
        }
        if (StringUtils.hasText(enterpriseDTO.getIndustry())) {
            enterprise.setIndustry(enterpriseDTO.getIndustry());
        }
        if (StringUtils.hasText(enterpriseDTO.getAddress())) {
            enterprise.setAddress(enterpriseDTO.getAddress());
        }
        if (StringUtils.hasText(enterpriseDTO.getPhone())) {
            enterprise.setContactPhone(enterpriseDTO.getPhone());
        }
        if (StringUtils.hasText(enterpriseDTO.getManagerName())) {
            enterprise.setManagerName(enterpriseDTO.getManagerName().trim());
        }

        enterprise.setUpdatedTime(LocalDateTime.now());
        enterprise = enterpriseRepository.save(enterprise);

        log.info("企业更新成功：id={}", id);
        return convertToVO(enterprise);
    }

    /**
     * 根据ID查询企业详情
     *
     * @param id 企业ID
     * @return 企业详情
     */
    @Override
    public EnterpriseVO getById(Long id) {
        log.info("查询企业详情：id={}", id);
        Enterprise enterprise = getEnterpriseById(id);
        return convertToVO(enterprise);
    }

    @Override
    public EnterpriseVO findActiveByCreditCode(String creditCode) {
        if (!StringUtils.hasText(creditCode)) {
            return null;
        }
        return enterpriseRepository.findByEnterpriseCodeIgnoreCase(creditCode.trim())
                .filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                .map(this::convertToVO)
                .orElse(null);
    }

    /**
     * 分页查询企业列表
     * 支持企业名称搜索、风险等级筛选
     *
     * @param pageQueryDTO 分页参数
     * @return 企业分页列表
     */
    @Override
    public PageResponse<EnterpriseVO> getPage(PageQueryDTO pageQueryDTO) {
        log.info("分页查询企业列表：page={}, size={}, keyword={}",
                pageQueryDTO.getPageNum(), pageQueryDTO.getPageSize(), pageQueryDTO.getKeyword());

        // 构建动态查询条件
        Specification<Enterprise> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除条件
            predicates.add(cb.equal(root.get("deleted"), 0));

            // 关键词搜索（企业名称或统一社会信用代码）
            if (StringUtils.hasText(pageQueryDTO.getKeyword())) {
                String keyword = "%" + pageQueryDTO.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("enterpriseName"), keyword),
                        cb.like(root.get("enterpriseCode"), keyword)));
            }

            if (StringUtils.hasText(pageQueryDTO.getRiskLevel())) {
                predicates.add(cb.equal(root.get("riskLevel"), pageQueryDTO.getRiskLevel()));
            }

            if (Boolean.TRUE.equals(pageQueryDTO.getActiveReportsOnly())
                    || StringUtils.hasText(pageQueryDTO.getPeriod())
                    || StringUtils.hasText(pageQueryDTO.getStatus())) {
                Subquery<Long> archiveQuery = query.subquery(Long.class);
                Root<FinancialReportArchive> archive = archiveQuery.from(FinancialReportArchive.class);
                List<Predicate> archivePredicates = new ArrayList<>();
                archivePredicates.add(cb.equal(archive.get("enterprise").get("id"), root.get("id")));
                archivePredicates.add(cb.equal(archive.get("deleted"), 0));
                if (StringUtils.hasText(pageQueryDTO.getPeriod())) {
                    archivePredicates.add(cb.equal(archive.get("reportPeriod"), pageQueryDTO.getPeriod()));
                }
                if (StringUtils.hasText(pageQueryDTO.getStatus())) {
                    archivePredicates.add(cb.equal(archive.get("filingStatus"), pageQueryDTO.getStatus()));
                }
                archiveQuery.select(archive.get("enterprise").get("id"))
                        .where(archivePredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(archiveQuery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 构建分页和排序
        Sort sort = Sort.by(Sort.Direction.DESC, "createdTime");
        PageRequest pageRequest = PageRequest.of(
                pageQueryDTO.getPageNum() - 1,
                pageQueryDTO.getPageSize(),
                sort
        );

        // 执行查询
        Page<Enterprise> page = enterpriseRepository.findAll(spec, pageRequest);

        // 转换为VO
        List<EnterpriseVO> voList = page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        log.info("查询完成：total={}, pages={}", page.getTotalElements(), page.getTotalPages());
        return PageResponse.of(voList, page.getTotalElements(),
                pageQueryDTO.getPageNum(), pageQueryDTO.getPageSize());
    }

    /**
     * 删除企业（逻辑删除）
     *
     * @param id 企业ID
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("删除企业：id={}", id);
        Enterprise enterprise = getEnterpriseById(id);
        List<FinancialReportArchive> archives = archiveRepository.findAllByEnterpriseIdAndDeleted(id, 0);
        if (!archives.isEmpty()) {
            throw new BusinessException(409, "该企业仍有在线报表，请先删除全部报表后再删除企业档案");
        }
        enterprise.setDeleted(1);
        enterprise.setUpdatedTime(LocalDateTime.now());
        enterpriseRepository.save(enterprise);
        log.info("企业删除成功：id={}", id);
    }

    /**
     * 根据ID获取企业实体（未删除的）
     */
    private Enterprise getEnterpriseById(Long id) {
        return enterpriseRepository.findById(id)
                .filter(e -> e.getDeleted() == 0)
                .orElseThrow(() -> new BusinessException(404, "企业不存在或已被删除"));
    }

    /**
     * 将Entity转换为VO
     */
    private EnterpriseVO convertToVO(Enterprise enterprise) {
        EnterpriseVO vo = new EnterpriseVO();
        vo.setId(enterprise.getId());
        vo.setName(enterprise.getEnterpriseName());
        vo.setCreditCode(enterprise.getEnterpriseCode());
        vo.setLegalPerson(enterprise.getLegalPerson());
        vo.setRegisteredCapital(enterprise.getRegisteredCapital());
        vo.setEstablishDate(enterprise.getEstablishDate() != null ?
                enterprise.getEstablishDate().toString() : null);
        vo.setPhone(enterprise.getContactPhone());
        vo.setAddress(enterprise.getAddress());
        vo.setIndustry(enterprise.getIndustry());
        vo.setLatestRiskLevel(null);
        vo.setLatestHealthScore(null);
        vo.setLastReportDate(enterprise.getLastReportDate());
        vo.setManagerName(enterprise.getManagerName());
        vo.setCreatedTime(enterprise.getCreatedTime());

        // 统计报表数量
        Long reportCount = archiveRepository.countByEnterpriseIdAndDeleted(enterprise.getId(), 0);
        vo.setReportCount(reportCount);

        List<FinancialReportArchive> latestArchives = archiveRepository
                .findTopByEnterpriseIdAndDeletedOrderByReportDateDesc(
                        enterprise.getId(), 0, PageRequest.of(0, 1));
        if (!latestArchives.isEmpty()) {
            FinancialReportArchive latest = latestArchives.get(0);
            vo.setLatestReportPeriod(latest.getReportPeriod());
            healthScoreRepository.findFirstByReportIdAndDeletedOrderByIdDesc(latest.getId(), 0)
                    .ifPresent(score -> {
                        vo.setLatestHealthScore(score.getTotalScore());
                        vo.setLatestRiskLevel(score.getRiskLevel());
                    });
            if (StringUtils.hasText(latest.getManagerName())) {
                vo.setManagerName(latest.getManagerName());
            }
        }

        return vo;
    }
}
