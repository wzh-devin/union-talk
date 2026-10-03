package com.devin.uniontalk.message.handler;

import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import java.util.Set;

/**
 * 2026/07/22 13:30.
 *
 * <p>
 * 消息执行器接口
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface MessageHandler {

    /**
     * 查询支持的消息类型.
     *
     * @return 消息类型
     */
    MessageTypeEnum getMessageType();

    /**
     * 查询支持的资产文件类型集合.
     *
     * @return 资产文件类型集合，非资产消息返回空集合
     */
    default Set<AssetFileTypeEnum> getSupportedAssetFileTypeSet() {
        return Set.of();
    }

    /**
     * 执行消息类型专属处理.
     *
     * @param content 消息内容
     */
    void handle(String content);
}
