package com.devin.uniontalk.file.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.common.storage.domain.model.StorageObjectWriteResult;
import com.devin.uniontalk.common.storage.properties.StorageProperties;
import com.devin.uniontalk.common.storage.service.ObjectStorageService;
import com.devin.uniontalk.file.dao.AssetFileDao;
import com.devin.uniontalk.file.dao.AssetFolderDao;
import com.devin.uniontalk.file.dao.AssetOutboxDao;
import com.devin.uniontalk.file.dao.UploadChunkDao;
import com.devin.uniontalk.file.dao.UploadSessionDao;
import com.devin.uniontalk.file.domain.entity.AssetFile;
import com.devin.uniontalk.file.domain.entity.AssetFolder;
import com.devin.uniontalk.file.domain.entity.UploadChunk;
import com.devin.uniontalk.file.domain.entity.UploadSession;
import com.devin.uniontalk.file.domain.entity.convertor.UploadSessionConvertor;
import com.devin.uniontalk.file.domain.model.AssetNameGenerator;
import com.devin.uniontalk.file.domain.model.ConversationAssetAccess;
import com.devin.uniontalk.file.domain.vo.req.InitUploadReqVO;
import com.devin.uniontalk.file.domain.vo.resp.UploadSessionRespVO;
import com.devin.uniontalk.file.grpc.client.AssetAccessGrpcClient;
import com.devin.uniontalk.file.service.AssetUploadService;
import com.devin.uniontalk.infrastructure.file.constant.AssetOutboxConstant;
import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import com.devin.uniontalk.infrastructure.file.enums.AssetOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.file.enums.StorageTypeEnum;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import com.devin.uniontalk.rabbitmq.domain.event.AssetContentChangedEvent;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 2026/06/30 18:50.
 *
 * <p>
 * 资产上传任务ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetUploadServiceImpl implements AssetUploadService {

    /**
     * 默认 MIME 类型.
     */
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /**
     * 上传任务 Dao.
     */
    private final UploadSessionDao uploadSessionDao;

    /**
     * 上传分片 Dao.
     */
    private final UploadChunkDao uploadChunkDao;

    /**
     * 资产文件 Dao.
     */
    private final AssetFileDao assetFileDao;

    /**
     * 资产目录 Dao.
     */
    private final AssetFolderDao assetFolderDao;

    /**
     * 资产发件箱 Dao.
     */
    private final AssetOutboxDao assetOutboxDao;

    /**
     * 对象存储服务.
     */
    private final ObjectStorageService objectStorageService;

    /**
     * 存储配置.
     */
    private final StorageProperties storageProperties;

    /**
     * 会话资产访问上下文 Grpc 客户端.
     */
    private final AssetAccessGrpcClient assetAccessGrpcClient;

    /**
     * 初始化上传任务.
     *
     * @param userId 用户id
     * @param reqVO  初始化上传任务请求参数
     * @return 上传任务响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadSessionRespVO init(final BigInteger userId, final InitUploadReqVO reqVO) {
        validateInitReq(reqVO);
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(reqVO.getConversationId(), userId);
        AssetFolder folder = Objects.isNull(reqVO.getFolderId())
                ? assetFolderDao.getOrCreateMemberHome(reqVO.getConversationId(), userId)
                : assetFolderDao.getFolderOrRoot(reqVO.getConversationId(), reqVO.getFolderId());
        AssertUtils.nonNull(folder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        AssertUtils.isFalse(folder.isMemberRoot(), BizErrorEnum.ASSET_TARGET_FOLDER_INVALID);
        AssertUtils.isTrue(folder.canWriteContent(access), BizErrorEnum.ASSET_PERMISSION_DENIED);

        BigInteger chunkSize = reqVO.getChunkSize();
        int chunkCount = getChunkCount(reqVO.getFileSize(), chunkSize);
        UploadSession session = uploadSessionDao.createUploadSession(
                reqVO.getConversationId(),
                folder.getId(),
                reqVO.getFileName(),
                getFileExt(reqVO.getFileName()),
                getMimeType(reqVO.getMimeType()),
                reqVO.getFileSize(),
                reqVO.getFileSha256(),
                chunkSize,
                chunkCount,
                storageProperties.getBucketName(),
                userId
        );
        return toRespVO(session);
    }

    /**
     * 上传分片.
     *
     * @param userId      用户id
     * @param sessionId   上传任务id
     * @param chunkIndex  分片序号
     * @param file        分片文件
     * @param chunkSha256 分片SHA256
     * @return 上传任务响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadSessionRespVO uploadChunk(
            final BigInteger userId,
            final BigInteger sessionId,
            final Integer chunkIndex,
            final MultipartFile file,
            final String chunkSha256
    ) {
        UploadSession session = getSession(sessionId, userId);
        session.validateCanUploadChunk();
        validateChunk(session, chunkIndex, file);
        String realChunkSha256 = getChunkSha256(file);
        if (StringUtils.hasText(chunkSha256)) {
            AssertUtils.isTrue(realChunkSha256.equalsIgnoreCase(chunkSha256), BizErrorEnum.UPLOAD_CHUNK_HASH_MISMATCH);
        }
        UploadChunk existsChunk = uploadChunkDao.getBySessionIdAndChunkIndex(sessionId, chunkIndex);
        if (Objects.nonNull(existsChunk)) {
            AssertUtils.isTrue(existsChunk.isSameChunk(realChunkSha256), BizErrorEnum.UPLOAD_CHUNK_HASH_MISMATCH);
            return toRespVO(session);
        }

        String storageKey = session.getChunkStorageKey(chunkIndex);
        StorageObjectWriteResult result = putChunkObject(session, storageKey, file);
        uploadChunkDao.createUploadChunk(
                sessionId,
                chunkIndex,
                BigInteger.valueOf(file.getSize()),
                realChunkSha256,
                storageKey,
                result.getEtag()
        );
        uploadSessionDao.increaseUploadedChunkCount(sessionId);
        return toRespVO(getSession(sessionId, userId));
    }

    /**
     * 完成上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadSessionRespVO complete(final BigInteger userId, final BigInteger sessionId) {
        UploadSession session = uploadSessionDao.getByIdAndUserIdForUpdate(sessionId, userId);
        AssertUtils.nonNull(session, BizErrorEnum.UPLOAD_SESSION_NOT_FOUND);
        session.validateCanComplete();
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(session.getConversationId(), userId);
        AssetFolder folder = assetFolderDao.getFolderOrRoot(session.getConversationId(), session.getFolderId());
        AssertUtils.nonNull(folder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        AssertUtils.isFalse(folder.isMemberRoot(), BizErrorEnum.ASSET_TARGET_FOLDER_INVALID);
        AssertUtils.isTrue(folder.canWriteContent(access), BizErrorEnum.ASSET_PERMISSION_DENIED);
        List<UploadChunk> chunkList = uploadChunkDao.getChunkListBySessionId(sessionId);
        AssertUtils.isTrue(chunkList.size() == session.getChunkCount(), BizErrorEnum.UPLOAD_CHUNK_MISSING);
        AssertUtils.isTrue(isFullChunkList(chunkList, session.getChunkCount()), BizErrorEnum.UPLOAD_CHUNK_MISSING);
        session.syncUploadedChunkCount(chunkList.size());

        BigInteger assetId = IdGenerator.nextIdBigInteger();
        String storageKey = session.getAssetStorageKey(assetId);
        try {
            StorageObjectWriteResult result = objectStorageService.composeObject(
                    session.getBucketName(),
                    storageKey,
                    chunkList.stream().map(UploadChunk::getStorageKey).toList(),
                    session.getMimeType()
            );
            assetFileDao.lockFolderName(session.getConversationId(), session.getFolderId());
            String finalFileName = AssetNameGenerator.getAvailableFileName(
                    session.getFileName(),
                    assetFileDao.getNormalNameSetByFolderId(
                            session.getConversationId(),
                            session.getFolderId()
                    )
            );
            session.applyFinalFileName(finalFileName);
            AssetFile assetFile = assetFileDao.createByUploadSession(
                    assetId,
                    session,
                    AssetFileTypeEnum.getByMimeType(session.getMimeType()),
                    StorageTypeEnum.MINIO,
                    storageKey,
                    result.getEtag(),
                    userId
            );
            session.complete(assetFile.getId());
            uploadSessionDao.updateSession(session);
            String eventId = AssetOutboxConstant.ASSET_CONTENT_CHANGED_EVENT_ID_PREFIX + assetFile.getId()
                    + ":" + assetFile.getResourceVersion();
            AssetContentChangedEvent event = AssetContentChangedEvent.builder()
                    .eventId(eventId)
                    .eventType(AssetOutboxEventTypeEnum.ASSET_CONTENT_CHANGED)
                    .schemaVersion(RabbitMqConstant.ASSET_CONTENT_CHANGED_SCHEMA_VERSION)
                    .assetFileId(assetFile.getId())
                    .conversationId(assetFile.getConversationId())
                    .resourceVersion(assetFile.getResourceVersion())
                    .occurredAt(new Date())
                    .build();
            assetOutboxDao.createEvent(
                    eventId,
                    assetFile.getId(),
                    assetFile.getResourceVersion(),
                    AssetOutboxEventTypeEnum.ASSET_CONTENT_CHANGED,
                    event
            );
            objectStorageService.removeObjectList(
                    session.getBucketName(),
                    uploadChunkDao.getChunkStorageKeyList(sessionId)
            );
            return toRespVO(session);
        } catch (BizException e) {
            session.fail(e.getMessage());
            uploadSessionDao.updateSession(session);
            throw e;
        } catch (Exception e) {
            session.fail(e.getMessage());
            uploadSessionDao.updateSession(session);
            log.error("完成上传任务失败, sessionId={}", sessionId, e);
            throw new BizException(BizErrorEnum.STORAGE_OBJECT_FAILED);
        }
    }

    /**
     * 取消上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(final BigInteger userId, final BigInteger sessionId) {
        UploadSession session = getSession(sessionId, userId);
        List<String> storageKeyList = uploadChunkDao.getChunkStorageKeyList(sessionId);
        session.cancel();
        uploadSessionDao.updateSession(session);
        objectStorageService.removeObjectList(session.getBucketName(), storageKeyList);
        uploadChunkDao.deleteBySessionId(sessionId);
    }

    /**
     * 暂停上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadSessionRespVO pause(final BigInteger userId, final BigInteger sessionId) {
        UploadSession session = getSession(sessionId, userId);
        session.pause();
        uploadSessionDao.updateSession(session);
        return toRespVO(session);
    }

    /**
     * 恢复上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadSessionRespVO resume(final BigInteger userId, final BigInteger sessionId) {
        UploadSession session = getSession(sessionId, userId);
        session.resume();
        uploadSessionDao.updateSession(session);
        return toRespVO(session);
    }

    /**
     * 查询上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    @Override
    public UploadSessionRespVO getDetail(final BigInteger userId, final BigInteger sessionId) {
        return toRespVO(getSession(sessionId, userId));
    }

    /**
     * 校验初始化上传任务请求.
     *
     * @param reqVO 初始化上传任务请求参数
     */
    private void validateInitReq(final InitUploadReqVO reqVO) {
        AssertUtils.isTrue(reqVO.getFileSize().compareTo(BigInteger.ZERO) > 0, BizErrorEnum.UPLOAD_PARAM_INVALID);
        AssertUtils.isTrue(reqVO.getChunkSize().compareTo(BigInteger.ZERO) > 0, BizErrorEnum.UPLOAD_PARAM_INVALID);
    }

    /**
     * 校验分片参数.
     *
     * @param session    上传任务
     * @param chunkIndex 分片序号
     * @param file       分片文件
     */
    private void validateChunk(final UploadSession session, final Integer chunkIndex, final MultipartFile file) {
        AssertUtils.nonNull(chunkIndex, BizErrorEnum.UPLOAD_PARAM_INVALID);
        AssertUtils.nonNull(file, BizErrorEnum.UPLOAD_PARAM_INVALID);
        AssertUtils.isTrue(chunkIndex >= 0 && chunkIndex < session.getChunkCount(), BizErrorEnum.UPLOAD_PARAM_INVALID);
        AssertUtils.isTrue(file.getSize() > 0, BizErrorEnum.UPLOAD_PARAM_INVALID);
        if (chunkIndex < session.getChunkCount() - 1) {
            AssertUtils.isTrue(
                    BigInteger.valueOf(file.getSize()).equals(session.getChunkSize()),
                    BizErrorEnum.UPLOAD_PARAM_INVALID
            );
        }
    }

    /**
     * 查询上传任务.
     *
     * @param sessionId 上传任务id
     * @param userId    用户id
     * @return 上传任务
     */
    private UploadSession getSession(final BigInteger sessionId, final BigInteger userId) {
        UploadSession session = uploadSessionDao.getByIdAndUserId(sessionId, userId);
        AssertUtils.nonNull(session, BizErrorEnum.UPLOAD_SESSION_NOT_FOUND);
        return session;
    }

    /**
     * 上传分片对象.
     *
     * @param session    上传任务
     * @param storageKey 存储Key
     * @param file       分片文件
     * @return 对象写入结果
     */
    private StorageObjectWriteResult putChunkObject(
            final UploadSession session,
            final String storageKey,
            final MultipartFile file
    ) {
        try (InputStream inputStream = file.getInputStream()) {
            return objectStorageService.putObject(
                    session.getBucketName(),
                    storageKey,
                    inputStream,
                    file.getSize(),
                    DEFAULT_MIME_TYPE
            );
        } catch (Exception e) {
            log.error("上传分片对象失败, sessionId={}, storageKey={}", session.getId(), storageKey, e);
            throw new BizException(BizErrorEnum.STORAGE_OBJECT_FAILED);
        }
    }

    /**
     * 计算分片SHA256.
     *
     * @param file 分片文件
     * @return 分片SHA256
     */
    private String getChunkSha256(final MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > -1) {
                messageDigest.update(buffer, 0, length);
            }
            return HexFormat.of().formatHex(messageDigest.digest()).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            log.error("计算分片SHA256失败, fileName={}", file.getOriginalFilename(), e);
            throw new BizException(BizErrorEnum.UPLOAD_PARAM_INVALID);
        }
    }

    /**
     * 判断分片列表是否完整.
     *
     * @param chunkList  分片列表
     * @param chunkCount 分片数量
     * @return true表示完整
     */
    private boolean isFullChunkList(final List<UploadChunk> chunkList, final Integer chunkCount) {
        Set<Integer> chunkIndexSet = chunkList.stream()
                .map(UploadChunk::getChunkIndex)
                .collect(Collectors.toSet());
        for (int index = 0; index < chunkCount; index++) {
            if (!chunkIndexSet.contains(index)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取分片数量.
     *
     * @param fileSize  文件大小
     * @param chunkSize 分片大小
     * @return 分片数量
     */
    private int getChunkCount(final BigInteger fileSize, final BigInteger chunkSize) {
        return fileSize.add(chunkSize).subtract(BigInteger.ONE).divide(chunkSize).intValue();
    }

    /**
     * 获取文件扩展名.
     *
     * @param fileName 文件名称
     * @return 文件扩展名
     */
    private String getFileExt(final String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 获取MIME类型.
     *
     * @param mimeType MIME类型
     * @return MIME类型
     */
    private String getMimeType(final String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return DEFAULT_MIME_TYPE;
        }
        return mimeType;
    }

    /**
     * 转换上传任务响应参数.
     *
     * @param session 上传任务
     * @return 上传任务响应参数
     */
    private UploadSessionRespVO toRespVO(final UploadSession session) {
        List<Integer> chunkIndexList = uploadChunkDao.getChunkListBySessionId(session.getId())
                .stream()
                .map(UploadChunk::getChunkIndex)
                .toList();
        return UploadSessionConvertor.INSTANCE.toRespVO(session, chunkIndexList);
    }
}
