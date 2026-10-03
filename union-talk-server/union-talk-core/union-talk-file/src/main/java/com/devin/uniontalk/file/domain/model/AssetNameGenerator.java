package com.devin.uniontalk.file.domain.model;

import java.util.Set;

/**
 * 2026/07/22 15:30.
 *
 * <p>
 * 资产名称生成器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AssetNameGenerator {

    /**
     * 文件名称最大长度.
     */
    private static final int FILE_NAME_MAX_LENGTH = 255;

    /**
     * 目录名称最大长度.
     */
    private static final int FOLDER_NAME_MAX_LENGTH = 100;

    /**
     * 禁止实例化.
     */
    private AssetNameGenerator() {
    }

    /**
     * 获取可用文件名称.
     *
     * @param fileName        原文件名称
     * @param existingNameSet 已存在名称集合
     * @return 可用文件名称
     */
    public static String getAvailableFileName(final String fileName, final Set<String> existingNameSet) {
        if (!existingNameSet.contains(fileName)) {
            return fileName;
        }
        int dotIndex = fileName.lastIndexOf('.');
        boolean hasFileExt = dotIndex >= 0 && dotIndex < fileName.length() - 1;
        String nameBody = hasFileExt ? fileName.substring(0, dotIndex) : fileName;
        String fileExt = hasFileExt ? fileName.substring(dotIndex) : "";
        return getAvailableName(nameBody, fileExt, existingNameSet, FILE_NAME_MAX_LENGTH);
    }

    /**
     * 获取可用目录名称.
     *
     * @param folderName      原目录名称
     * @param existingNameSet 已存在名称集合
     * @return 可用目录名称
     */
    public static String getAvailableFolderName(final String folderName, final Set<String> existingNameSet) {
        if (!existingNameSet.contains(folderName)) {
            return folderName;
        }
        return getAvailableName(folderName, "", existingNameSet, FOLDER_NAME_MAX_LENGTH);
    }

    /**
     * 按序号获取可用名称.
     *
     * @param nameBody        名称主体
     * @param fileExt         文件扩展名
     * @param existingNameSet 已存在名称集合
     * @param maxLength       名称最大长度
     * @return 可用名称
     */
    private static String getAvailableName(
            final String nameBody,
            final String fileExt,
            final Set<String> existingNameSet,
            final int maxLength
    ) {
        int sequence = 1;
        while (true) {
            String suffix = "(" + sequence + ")";
            int maxBodyLength = Math.max(0, maxLength - suffix.length() - fileExt.length());
            int bodyEndIndex = Math.min(nameBody.length(), maxBodyLength);
            if (bodyEndIndex > 0
                    && bodyEndIndex < nameBody.length()
                    && Character.isHighSurrogate(nameBody.charAt(bodyEndIndex - 1))
                    && Character.isLowSurrogate(nameBody.charAt(bodyEndIndex))) {
                bodyEndIndex--;
            }
            String availableNameBody = nameBody.substring(0, bodyEndIndex);
            String availableName = availableNameBody + suffix + fileExt;
            if (!existingNameSet.contains(availableName)) {
                return availableName;
            }
            sequence++;
        }
    }
}
