"""资源索引元数据 DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:16
"""

from datetime import UTC, datetime

import sqlalchemy as sa
from sqlalchemy.engine import RowMapping
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.infrastructure.postgres.column_expression import table_column
from union_talk_agent.infrastructure.postgres.query_cardinality import select_one_or_none
from union_talk_agent.infrastructure.postgres.tables import (
    agent_ingest_job_table,
    agent_resource_chunk_table,
    agent_resource_table,
)
from union_talk_agent.knowledge.domain.enums import (
    ChunkKind,
    IngestJobStage,
    IngestJobStatus,
    ResourceStatus,
    VectorStatus,
)
from union_talk_agent.knowledge.domain.indexed_resource import (
    IndexedResource,
    ResourceDescriptor,
)
from union_talk_agent.knowledge.domain.resource_chunk import ResourceChunk


class ResourceIndexDao:
    """资源、切块和 Ingest Job 的事务写入。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化 ResourceIndexDao

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def get_by_asset_version(
        self,
        asset_file_id: int,
        resource_version: int,
    ) -> IndexedResource | None:
        """
        按资产和版本查询最早创建的索引记录

        :param asset_file_id: 资源文件 ID
        :param resource_version: 资源版本
        :return: 指定资产版本的索引资源，不存在时返回空
        """

        row = await select_one_or_none(
            self._session,
            sa.select(agent_resource_table)
            .where(table_column(agent_resource_table, "asset_file_id") == asset_file_id)
            .where(table_column(agent_resource_table, "resource_version") == resource_version)
            .order_by(agent_resource_table.c["created_at"]),
            "同一资产版本存在重复资源索引记录",
        )
        return self._to_domain(row) if row is not None else None

    async def replace_prepared_resource(
        self,
        resource: IndexedResource,
        chunk_list: list[ResourceChunk],
        ingest_job_id: int,
        worker_id: str,
    ) -> None:
        """
        写入或更新 INDEXING 资源并替换该资源的切块

        :param resource: 资源索引领域对象
        :param chunk_list: 资源切块列表
        :param ingest_job_id: 资源入库任务 ID
        :param worker_id: 当前 Worker 标识
        :return: 无返回值
        """

        now = datetime.now(UTC)
        existing = await self.get_by_asset_version(
            resource.descriptor.asset_file_id,
            resource.descriptor.resource_version,
        )
        value_map = self._resource_values(resource, now)
        if existing is None:
            await self._session.execute(
                sa.insert(agent_resource_table).values(
                    id=resource.resource_id,
                    created_at=now,
                    **value_map,
                )
            )
        else:
            await self._session.execute(
                sa.update(agent_resource_table)
                .where(table_column(agent_resource_table, "id") == existing.resource_id)
                .values(**value_map)
            )
        await self._session.execute(
            sa.delete(agent_resource_chunk_table).where(
                table_column(agent_resource_chunk_table, "resource_id") == resource.resource_id
            )
        )
        if chunk_list:
            await self._session.execute(
                sa.insert(agent_resource_chunk_table),
                [
                    {
                        "id": chunk.chunk_id,
                        "resource_id": chunk.resource_id,
                        "conversation_id": chunk.conversation_id,
                        "parent_chunk_id": chunk.parent_chunk_id,
                        "chunk_type": str(chunk.chunk_kind),
                        "chunk_no": chunk.chunk_no,
                        "text_content": chunk.text_content,
                        "token_count": chunk.token_count,
                        "page_from": chunk.page_from,
                        "page_to": chunk.page_to,
                        "heading_path": chunk.heading_path,
                        "metadata_json": chunk.metadata_map,
                        "milvus_pk": f"resource:{chunk.chunk_id}",
                        "vector_status": (
                            str(VectorStatus.PENDING)
                            if chunk.chunk_kind in {ChunkKind.CHILD, ChunkKind.TABLE}
                            else str(VectorStatus.NOT_APPLICABLE)
                        ),
                        "created_at": now,
                    }
                    for chunk in chunk_list
                ],
            )
        await self._session.execute(
            sa.insert(agent_ingest_job_table).values(
                id=ingest_job_id,
                asset_file_id=resource.descriptor.asset_file_id,
                resource_version=resource.descriptor.resource_version,
                stage=str(IngestJobStage.EMBEDDING),
                status=str(IngestJobStatus.RUNNING),
                attempt=1,
                worker_id=worker_id,
                started_at=now,
                created_at=now,
                updated_at=now,
            )
        )

    async def mark_ready(
        self,
        resource: IndexedResource,
        ingest_job_id: int,
    ) -> None:
        """
        原子切换当前版本、资源状态、向量状态和任务状态

        :param resource: 资源索引领域对象
        :param ingest_job_id: 资源入库任务 ID
        :return: 无返回值
        """

        now = datetime.now(UTC)
        await self._session.execute(
            sa.update(agent_resource_table)
            .where(
                table_column(agent_resource_table, "asset_file_id")
                == resource.descriptor.asset_file_id
            )
            .values(is_current=False, updated_at=now)
        )
        await self._session.execute(
            sa.update(agent_resource_table)
            .where(table_column(agent_resource_table, "id") == resource.resource_id)
            .values(
                is_current=True,
                status=str(ResourceStatus.READY),
                chunk_count=resource.chunk_count,
                token_count=resource.token_count,
                error_code=None,
                error_message=None,
                updated_at=now,
            )
        )
        await self._session.execute(
            sa.update(agent_resource_chunk_table)
            .where(table_column(agent_resource_chunk_table, "resource_id") == resource.resource_id)
            .where(
                table_column(agent_resource_chunk_table, "chunk_type").in_(
                    [str(ChunkKind.CHILD), str(ChunkKind.TABLE)]
                )
            )
            .values(vector_status=str(VectorStatus.READY))
        )
        await self._session.execute(
            sa.update(agent_ingest_job_table)
            .where(table_column(agent_ingest_job_table, "id") == ingest_job_id)
            .values(
                stage=str(IngestJobStage.FINALIZING),
                status=str(IngestJobStatus.SUCCEEDED),
                finished_at=now,
                updated_at=now,
            )
        )

    async def mark_failed(
        self,
        resource_id: int,
        ingest_job_id: int,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        记录资源和任务失败

        :param resource_id: 资源索引 ID
        :param ingest_job_id: 资源入库任务 ID
        :param error_code: 错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        now = datetime.now(UTC)
        await self._session.execute(
            sa.update(agent_resource_table)
            .where(table_column(agent_resource_table, "id") == resource_id)
            .values(
                status=str(ResourceStatus.FAILED),
                error_code=error_code,
                error_message=error_message[:500],
                updated_at=now,
            )
        )
        await self._session.execute(
            sa.update(agent_ingest_job_table)
            .where(table_column(agent_ingest_job_table, "id") == ingest_job_id)
            .values(
                status=str(IngestJobStatus.FAILED),
                error_code=error_code,
                error_message=error_message[:500],
                finished_at=now,
                updated_at=now,
            )
        )

    async def mark_deleted(self, asset_file_id: int) -> None:
        """
        使资产全部资源版本、切块和运行中任务失效

        :param asset_file_id: 资产文件 ID
        :return: 无返回值
        """

        now = datetime.now(UTC)
        resource_id_query = sa.select(agent_resource_table.c["id"]).where(
            table_column(agent_resource_table, "asset_file_id") == asset_file_id
        )
        await self._session.execute(
            sa.update(agent_resource_table)
            .where(table_column(agent_resource_table, "asset_file_id") == asset_file_id)
            .values(
                is_current=False,
                status=str(ResourceStatus.DELETED),
                updated_at=now,
            )
        )
        await self._session.execute(
            sa.update(agent_resource_chunk_table)
            .where(table_column(agent_resource_chunk_table, "resource_id").in_(resource_id_query))
            .values(vector_status=str(VectorStatus.DELETED))
        )
        await self._session.execute(
            sa.update(agent_ingest_job_table)
            .where(table_column(agent_ingest_job_table, "asset_file_id") == asset_file_id)
            .where(table_column(agent_ingest_job_table, "status") == str(IngestJobStatus.RUNNING))
            .values(
                status=str(IngestJobStatus.CANCELLED),
                error_code="RESOURCE_DELETED",
                error_message="资源已删除",
                finished_at=now,
                updated_at=now,
            )
        )

    @staticmethod
    def _resource_values(
        resource: IndexedResource,
        now: datetime,
    ) -> dict[str, object]:
        """
        构建资源索引数据库字段映射

        :param resource: 资源索引领域对象
        :param now: 当前 UTC 时间
        :return: 资源数据库字段映射
        """

        descriptor = resource.descriptor
        return {
            "conversation_id": descriptor.conversation_id,
            "asset_file_id": descriptor.asset_file_id,
            "resource_version": descriptor.resource_version,
            "is_current": False,
            "file_name": descriptor.file_name,
            "folder_id": descriptor.folder_id,
            "path_text": descriptor.path_text,
            "mime_type": descriptor.mime_type,
            "sha256": descriptor.sha256,
            "etag": descriptor.etag,
            "parser_version": resource.parser_version,
            "chunker_version": resource.chunker_version,
            "embedding_model_version": resource.embedding_model_version,
            "status": str(ResourceStatus.INDEXING),
            "error_code": None,
            "error_message": None,
            "chunk_count": resource.chunk_count,
            "token_count": resource.token_count,
            "updated_at": now,
        }

    @staticmethod
    def _to_domain(row: RowMapping) -> IndexedResource:
        """
        将数据库行转换为领域对象

        :param row: 数据库查询行
        :return: 转换后的领域对象
        """

        return IndexedResource(
            resource_id=int(row["id"]),
            descriptor=ResourceDescriptor(
                asset_file_id=int(row["asset_file_id"]),
                conversation_id=int(row["conversation_id"]),
                resource_version=int(row["resource_version"]),
                file_name=str(row["file_name"]),
                mime_type=str(row["mime_type"]),
                sha256=str(row["sha256"]),
                etag=str(row["etag"] or ""),
                folder_id=(int(row["folder_id"]) if row["folder_id"] is not None else None),
                path_text=str(row["path_text"] or ""),
            ),
            status=ResourceStatus(str(row["status"])),
            parser_version=str(row["parser_version"]),
            chunker_version=str(row["chunker_version"]),
            embedding_model_version=str(row["embedding_model_version"]),
            chunk_count=int(row["chunk_count"] or 0),
            token_count=int(row["token_count"] or 0),
            error_code=str(row["error_code"]) if row["error_code"] else None,
            error_message=str(row["error_message"]) if row["error_message"] else None,
        )
