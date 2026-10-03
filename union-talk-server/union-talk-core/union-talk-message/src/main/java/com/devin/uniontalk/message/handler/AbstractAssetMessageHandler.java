package com.devin.uniontalk.message.handler;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import java.math.BigInteger;
import java.util.Set;

/**
 * 2026/07/22 13:30.
 *
 * <p>
 * 抽象资产消息执行器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class AbstractAssetMessageHandler extends AbstractMessageHandler {

    /**
     * 资产文件id最大十进制长度.
     */
    private static final int ASSET_ID_MAX_LENGTH = 19;

    /**
     * 查询支持的资产文件类型集合.
     *
     * @return 资产文件类型集合
     */
    @Override
    public abstract Set<AssetFileTypeEnum> getSupportedAssetFileTypeSet();

    /**
     * 校验资产消息内容.
     *
     * @param content 资产文件id
     */
    @Override
    protected final void validateContent(final String content) {
        AssertUtils.isTrue(
                content.length() <= ASSET_ID_MAX_LENGTH
                        && content.charAt(0) >= '1'
                        && content.charAt(0) <= '9'
                        && content.chars().allMatch(character -> character >= '0' && character <= '9')
                        && new BigInteger(content).compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0,
                BizErrorEnum.MESSAGE_ASSET_ID_INVALID
        );
    }
}
