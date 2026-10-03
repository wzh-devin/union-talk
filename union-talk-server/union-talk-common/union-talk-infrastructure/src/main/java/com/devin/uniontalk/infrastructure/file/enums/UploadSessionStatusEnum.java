package com.devin.uniontalk.infrastructure.file.enums;

/**
 * 2026/06/30 18:00.
 *
 * <p>
 * 上传任务状态枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum UploadSessionStatusEnum {

    /**
     * 上传中.
     */
    UPLOADING,

    /**
     * 已暂停.
     */
    PAUSED,

    /**
     * 已完成.
     */
    COMPLETED,

    /**
     * 已取消.
     */
    CANCELED,

    /**
     * 上传失败.
     */
    FAILED
}
