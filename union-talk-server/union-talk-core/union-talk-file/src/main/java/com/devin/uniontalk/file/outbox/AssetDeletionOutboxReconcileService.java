package com.devin.uniontalk.file.outbox;

import com.devin.uniontalk.file.dao.AssetFileDao;
import com.devin.uniontalk.file.dao.AssetOutboxDao;
import com.devin.uniontalk.file.domain.entity.AssetFile;
import com.devin.uniontalk.infrastructure.file.constant.AssetOutboxConstant;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/08/13 17:48.
 *
 * <p>
 * 资产删除发件箱事件对账服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AssetDeletionOutboxReconcileService {

    /**
     * 资产文件 Dao.
     */
    private final AssetFileDao assetFileDao;

    /**
     * 资产事件发件箱 Dao.
     */
    private final AssetOutboxDao assetOutboxDao;

    /**
     * 补齐历史删除文件缺失的删除事件.
     */
    @Scheduled(fixedDelayString = AssetOutboxConstant.DELETION_RECONCILE_FIXED_DELAY_EXPRESSION)
    @Transactional(rollbackFor = Exception.class)
    public void reconcile() {
        List<AssetFile> assetFileList = assetFileDao.getDeletedWithoutOutboxList(
                AssetOutboxConstant.DELETION_RECONCILE_BATCH_SIZE
        );
        assetFileList.forEach(assetFile -> assetOutboxDao.createDeletedEvent(
                assetFile,
                assetFile.getDeletedAt() == null ? new Date() : assetFile.getDeletedAt()
        ));
    }
}
