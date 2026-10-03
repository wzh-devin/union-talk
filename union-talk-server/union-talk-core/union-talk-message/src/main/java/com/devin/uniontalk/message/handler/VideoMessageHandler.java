package com.devin.uniontalk.message.handler;

import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 2026/07/22 13:30.
 *
 * <p>
 * 视频消息执行器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class VideoMessageHandler extends AbstractAssetMessageHandler {

    /**
     * 查询支持的消息类型.
     *
     * @return 视频消息类型
     */
    @Override
    public MessageTypeEnum getMessageType() {
        return MessageTypeEnum.VIDEO;
    }

    /**
     * 查询支持的资产文件类型集合.
     *
     * @return 视频资产文件类型集合
     */
    @Override
    public Set<AssetFileTypeEnum> getSupportedAssetFileTypeSet() {
        return Set.of(AssetFileTypeEnum.VIDEO);
    }
}
