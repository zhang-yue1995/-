package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.entity.OcrTask;
import com.xinsulu.entity.UploadedFile;
import com.xinsulu.repository.OcrTaskRepository;
import com.xinsulu.repository.UploadedFileRepository;
import com.xinsulu.service.OcrService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OCR识别控制器
 * 提供OCR识别任务管理和结果查询功能
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/ocr")
@Api(tags = "OCR识别管理")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private OcrTaskRepository ocrTaskRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    /**
     * 发起OCR识别任务
     * 上传文件后调用此接口进行识别
     *
     * @param fileId 文件ID
     * @return OCR识别任务
     */
    @PostMapping("/recognize")
    @ApiOperation(value = "发起OCR识别", notes = "对上传的财务报表图片/PDF进行OCR识别")
    public ApiResponse<OcrTask> recognize(@RequestParam Long fileId) {
        log.info("发起OCR识别：fileId={}", fileId);

        // 获取文件信息
        UploadedFile file = getUploadedFile(fileId);

        // 执行OCR识别（同步方式）
        OcrTask task = ocrService.recognize(file);
        return ApiResponse.success(task);
    }

    /**
     * 异步发起OCR识别任务
     * 立即返回任务ID，后台异步处理
     *
     * @param fileId 文件ID
     * @return OCR识别任务（包含任务ID）
     */
    @PostMapping("/recognize-async")
    @ApiOperation(value = "异步OCR识别", notes = "异步执行OCR识别，立即返回任务ID用于轮询进度")
    public ApiResponse<OcrTask> recognizeAsync(@RequestParam Long fileId) {
        log.info("异步OCR识别：fileId={}", fileId);

        UploadedFile file = getUploadedFile(fileId);
        OcrTask task = ocrService.recognizeAsync(file);
        return ApiResponse.success(task);
    }

    /**
     * 查询OCR任务状态
     *
     * @param taskId 任务ID
     * @return 任务对象（包含状态和进度）
     */
    @GetMapping("/tasks/{taskId}")
    @ApiOperation(value = "查询任务状态", notes = "查询OCR任务的执行状态和进度")
    public ApiResponse<OcrTask> getTaskStatus(@PathVariable Long taskId) {
        log.info("查询OCR任务状态：taskId={}", taskId);
        OcrTask task = ocrService.getTaskStatus(taskId);
        return ApiResponse.success(task);
    }

    /**
     * 获取OCR识别结果
     * 包含所有识别出的字段及置信度
     *
     * @param taskId 任务ID
     * @return 识别结果列表
     */
    @GetMapping("/tasks/{taskId}/results")
    @ApiOperation(value = "获取识别结果", notes = "获取OCR识别的所有字段结果和置信度")
    public ApiResponse<Object> getResults(@PathVariable Long taskId) {
        log.info("获取OCR识别结果：taskId={}", taskId);
        Object results = ocrService.getTaskResults(taskId);
        return ApiResponse.success(results);
    }

    @PostMapping("/tasks/merge")
    @ApiOperation(value = "合并多张报表照片的OCR结果")
    public ApiResponse<OcrTask> mergeTasks(@RequestBody List<Long> taskIds) {
        return ApiResponse.success(ocrService.mergeTasks(taskIds));
    }

    /**
     * OCR任务列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 任务列表
     */
    @GetMapping("/tasks")
    @ApiOperation(value = "OCR任务列表", notes = "分页查询所有的OCR识别任务")
    public ApiResponse<Page<OcrTask>> listTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("查询OCR任务列表：page={}, size={}", page, size);

        Sort sort = Sort.by(Sort.Direction.DESC, "createdTime");
        PageRequest pageRequest = PageRequest.of(page - 1, size, sort);
        Page<OcrTask> tasks = ocrTaskRepository.findByDeleted(0, pageRequest);

        return ApiResponse.success(tasks);
    }

    /**
     * 取消OCR任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @DeleteMapping("/tasks/{taskId}")
    @ApiOperation(value = "取消任务", notes = "取消正在执行的OCR识别任务")
    public ApiResponse<Void> cancelTask(@PathVariable Long taskId) {
        log.info("取消OCR任务：taskId={}", taskId);
        ocrService.cancelTask(taskId);
        return ApiResponse.success();
    }

    /**
     * 获取上传文件实体
     */
    private UploadedFile getUploadedFile(Long fileId) {
        return uploadedFileRepository.findById(fileId)
                .filter(file -> Integer.valueOf(0).equals(file.getDeleted()))
                .orElseThrow(() -> new RuntimeException("上传文件不存在"));
    }
}
