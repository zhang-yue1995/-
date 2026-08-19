package com.xinsulu.service;

import com.xinsulu.entity.OcrTask;
import com.xinsulu.entity.UploadedFile;

import java.util.List;

/**
 * OCR识别服务接口
 * 提供可配置的本地验收与 HTTP 生产识别能力
 *
 * @author xinsulu-team
 */
public interface OcrService {

    /**
     * 执行OCR识别任务
     *
     * @param file 上传的文件
     * @return OCR识别任务（包含识别结果）
     */
    OcrTask recognize(UploadedFile file);

    /**
     * 异步执行OCR识别任务
     *
     * @param file 上传的文件
     * @return OCR识别任务（异步处理）
     */
    OcrTask recognizeAsync(UploadedFile file);

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务对象
     */
    OcrTask getTaskStatus(Long taskId);

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     */
    void cancelTask(Long taskId);

    /**
     * 获取OCR任务的识别结果
     *
     * @param taskId 任务ID
     * @return 识别结果列表
     */
    Object getTaskResults(Long taskId);

    /** 将同批多张报表照片的识别结果合并到首个任务。 */
    OcrTask mergeTasks(List<Long> taskIds);
}
