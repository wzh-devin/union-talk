package com.devin.uniontalk.file.domain.model;

import java.math.BigInteger;

/**
 * 2026/08/13 00:10.
 *
 * <p>
 * Agent索引资源内容快照
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public record AgentResourceContent(
        BigInteger assetId,
        BigInteger conversationId,
        Integer resourceVersion,
        String fileName,
        String mimeType,
        String sha256,
        String etag,
        BigInteger folderId,
        String pathText,
        String downloadUrl
) {
}
