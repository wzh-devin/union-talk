"""Agent 控制面 HTTP 路由。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 19:00
"""

from typing import Annotated

from fastapi import APIRouter, Depends

from union_talk_agent.access_control.application.ports.conversation_permission_reader import (
    ConversationPermissionReader,
)
from union_talk_agent.agent_config.application.agent_config_service import (
    AgentControlPlaneService,
    ReplaceAgentCredentialCommand,
    SaveAgentDefinitionCommand,
)
from union_talk_agent.interfaces.http.request_principal import (
    RequestPrincipal,
    require_authenticated_user,
    require_conversation_member,
)
from union_talk_agent.interfaces.http.schemas.agent_config_request import (
    AgentCredentialRequest,
    AgentDefinitionRequest,
)
from union_talk_agent.interfaces.http.schemas.agent_config_response import (
    AgentContextResponse,
)
from union_talk_agent.interfaces.http.schemas.api_result import ApiResult, api_success


def create_agent_control_plane_router(
    service: AgentControlPlaneService,
    permission_reader: ConversationPermissionReader,
) -> APIRouter:
    """
    创建显式注入控制面服务的路由

    :param service: Agent 控制面应用服务
    :param permission_reader: 会话权限读取端口
    :return: Agent 控制面 Router
    """

    router = APIRouter(prefix="/api/v1/conversations", tags=["agent-control-plane"])

    @router.get(
        "/{conversation_id}/context",
        response_model=ApiResult[AgentContextResponse],
    )
    async def get_context(
        conversation_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[AgentContextResponse]:
        """
        查询当前会话 Agent 状态和服务端授权能力

        :param conversation_id: 会话 ID
        :param principal: 当前请求身份
        :return: 会话 Agent 上下文响应
        """

        permission = await require_conversation_member(
            permission_reader,
            principal,
            conversation_id,
        )
        view = await service.get_context(conversation_id, permission)
        return api_success(AgentContextResponse.from_view(view))

    @router.put(
        "/{conversation_id}/definition",
        response_model=ApiResult[AgentContextResponse],
    )
    async def save_definition(
        conversation_id: int,
        request: AgentDefinitionRequest,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[AgentContextResponse]:
        """
        保存 Agent 定义新版本且不改变凭证所有权

        :param conversation_id: 会话 ID
        :param request: Agent 定义请求
        :param principal: 当前请求身份
        :return: 保存后的会话 Agent 上下文
        """

        permission = await require_conversation_member(
            permission_reader,
            principal,
            conversation_id,
        )
        view = await service.save_definition(
            SaveAgentDefinitionCommand(
                conversation_id=conversation_id,
                updated_by=principal.user_id,
                agent_version=request.agent_version,
                binding_version=request.binding_version,
                display_name=request.display_name,
                model_id=request.model_id,
                timeout_ms=request.timeout_ms,
                max_retries=request.max_retries,
                system_prompt=request.system_prompt,
                is_enabled=request.enabled,
                is_history_enabled=request.history_enabled,
                is_resource_enabled=request.resource_enabled,
                max_context_tokens=request.max_context_tokens,
                max_output_tokens=request.max_output_tokens,
                recent_message_tokens=request.recent_message_tokens,
                message_top_k=request.message_top_k,
                resource_top_k=request.resource_top_k,
                temperature=request.temperature,
                thinking_enabled=request.thinking_enabled,
                thinking_effort=request.thinking_effort,
            ),
            permission,
        )
        return api_success(AgentContextResponse.from_view(view))

    @router.put(
        "/{conversation_id}/credential",
        response_model=ApiResult[AgentContextResponse],
    )
    async def replace_credential(
        conversation_id: int,
        request: AgentCredentialRequest,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[AgentContextResponse]:
        """
        测试新 Key 并替换当前会话 Provider 凭证

        :param conversation_id: 会话 ID
        :param request: Provider 凭证请求
        :param principal: 当前请求身份
        :return: 替换后的会话 Agent 上下文
        """

        permission = await require_conversation_member(
            permission_reader,
            principal,
            conversation_id,
        )
        view = await service.replace_credential(
            ReplaceAgentCredentialCommand(
                conversation_id=conversation_id,
                updated_by=principal.user_id,
                binding_version=request.binding_version,
                api_base=str(request.api_base),
                api_key=request.api_key.get_secret_value(),
            ),
            permission,
        )
        return api_success(AgentContextResponse.from_view(view))

    @router.post(
        "/{conversation_id}/credential/test",
        response_model=ApiResult[None],
    )
    async def test_credential(
        conversation_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[None]:
        """
        测试当前已保存的 Provider 凭证

        :param conversation_id: 会话 ID
        :param principal: 当前请求身份
        :return: 无返回值
        """

        permission = await require_conversation_member(
            permission_reader,
            principal,
            conversation_id,
        )
        await service.test_current_credential(conversation_id, permission)
        return api_success(None)

    @router.delete(
        "/{conversation_id}/credential",
        response_model=ApiResult[AgentContextResponse],
    )
    async def revoke_credential(
        conversation_id: int,
        principal: Annotated[RequestPrincipal, Depends(require_authenticated_user)],
    ) -> ApiResult[AgentContextResponse]:
        """
        撤销当前会话绑定的 Provider 凭证

        :param conversation_id: 会话 ID
        :param principal: 当前请求身份
        :return: 撤销后的会话 Agent 上下文
        """

        permission = await require_conversation_member(
            permission_reader,
            principal,
            conversation_id,
        )
        view = await service.revoke_current_credential(conversation_id, permission)
        return api_success(AgentContextResponse.from_view(view))

    return router
