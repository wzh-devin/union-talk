package com.devin.uniontalk.file.service;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 2026/07/17.
 *
 * <p>
 * 头像缩略图服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class AvatarThumbnailService {

    /**
     * 头像缩略图宽度.
     */
    private static final int THUMBNAIL_WIDTH = 64;

    /**
     * 头像缩略图高度.
     */
    private static final int THUMBNAIL_HEIGHT = 64;

    /**
     * 头像缩略图 WebP 编码质量.
     */
    private static final double THUMBNAIL_QUALITY = 0.85D;

    /**
     * 创建头像缩略图.
     *
     * @param imageInputStream 原始图片输入流
     * @return WebP缩略图字节
     * @throws IOException 图片读取或写入失败
     */
    public byte[] createThumbnail(final InputStream imageInputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Thumbnails.of(imageInputStream)
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .crop(Positions.CENTER)
                    .outputFormat("webp")
                    .outputQuality(THUMBNAIL_QUALITY)
                    .toOutputStream(outputStream);
            return outputStream.toByteArray();
        }
    }
}
