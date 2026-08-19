package com.xinsulu.service;

import com.xinsulu.dto.EnterpriseDTO;
import com.xinsulu.dto.PageQueryDTO;
import com.xinsulu.dto.PageResponse;
import com.xinsulu.vo.EnterpriseVO;

/**
 * 企业管理服务接口
 *
 * @author xinsulu-team
 */
public interface EnterpriseService {

    /**
     * 创建企业
     *
     * @param enterpriseDTO 企业信息
     * @return 企业详情
     */
    EnterpriseVO create(EnterpriseDTO enterpriseDTO);

    /**
     * 更新企业信息
     *
     * @param id            企业ID
     * @param enterpriseDTO 企业信息
     * @return 更新后的企业详情
     */
    EnterpriseVO update(Long id, EnterpriseDTO enterpriseDTO);

    /**
     * 根据ID查询企业详情
     *
     * @param id 企业ID
     * @return 企业详情
     */
    EnterpriseVO getById(Long id);

    EnterpriseVO findActiveByCreditCode(String creditCode);

    /**
     * 分页查询企业列表
     *
     * @param pageQueryDTO 分页参数
     * @return 企业分页列表
     */
    PageResponse<EnterpriseVO> getPage(PageQueryDTO pageQueryDTO);

    /**
     * 删除企业（逻辑删除）
     *
     * @param id 企业ID
     */
    void delete(Long id);
}
