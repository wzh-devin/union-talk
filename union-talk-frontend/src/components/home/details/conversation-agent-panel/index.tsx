import {
  ChevronRight,
  Circle,
  CircleCheck,
  CircleX,
  Eye,
  EyeOff,
  KeyRound,
  Link2,
  LoaderCircle,
  RefreshCw,
} from 'lucide-react'
import { useState } from 'react'
import type { UIEvent } from 'react'

import { Button } from '@/components/shadcn-ui/button'
import { AgentConfigSelect } from '@/components/home/details/agent-config-select'
import { AgentRunStatusBadge } from '@/components/home/details/agent-run-status-badge'
import { Field, FieldGroup, FieldLabel } from '@/components/shadcn-ui/field'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
  InputGroupText,
} from '@/components/shadcn-ui/input-group'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { Switch } from '@/components/shadcn-ui/switch'
import type { ConversationAgentController } from '@/hooks/use-conversation-agent'
import {
  getAgentRunDurationLabel,
  type AgentRun,
} from '@/pages/home/model/agent-run'
import type { Conversation } from '@/pages/home/model/types'
import { DEEPSEEK_MODEL_OPTION_LIST } from '@/services/agent/agent-constant'
import { cn } from '@/utils/class-name'

interface ConversationAgentPanelProps {
  conversation: Conversation
  controller: ConversationAgentController
}

/**
 * 格式化执行记录时间.
 * @param value ISO 时间
 * @return 时分秒文本
 */
const formatRunTime = (value: string | null): string => {
  if (!value) {
    return '刚刚'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '刚刚'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

/**
 * 渲染会话 Agent 配置和执行记录.
 * @param props Agent 面板属性
 * @return Agent 详情面板
 */
export const ConversationAgentPanel = ({
  conversation,
  controller,
}: ConversationAgentPanelProps) => {
  const [isApiKeyVisible, setIsApiKeyVisible] = useState(false)
  const draft = controller.definitionDraft
  const credentialDraft = controller.credentialDraft
  const canManageDefinition = Boolean(controller.context?.canManageDefinition)
  const canReplaceCredential = Boolean(controller.context?.canReplaceCredential)
  const credentialOwnerName = controller.context?.credential
    ? (conversation.memberList.find(
        (member) => member.id === controller.context?.credential?.ownerUserId,
      )?.name ?? `用户 ${controller.context.credential.ownerUserId.slice(-4)}`)
    : null
  const connectionStatusLabel =
    controller.connectionError ||
    (controller.connectionLatency !== null
      ? `连接正常 · ${controller.connectionLatency}ms`
      : controller.isConnectionTesting
        ? '正在测试连接'
        : controller.context?.credential?.connectionTestStatus === 'SUCCEEDED'
          ? '当前凭证已通过测试'
          : controller.context?.credential?.connectionTestStatus === 'FAILED'
            ? '当前凭证测试失败'
            : '尚未配置可用凭证')
  const ConnectionStatusIcon = controller.connectionError
    ? CircleX
    : controller.connectionLatency !== null
      ? CircleCheck
      : controller.isConnectionTesting
        ? LoaderCircle
        : controller.context?.credential?.connectionTestStatus === 'SUCCEEDED'
          ? CircleCheck
          : controller.context?.credential?.connectionTestStatus === 'FAILED'
            ? CircleX
            : Circle

  /**
   * 查询 Run 对应请求用户名称.
   * @param run Agent Run
   * @return 请求用户名称
   */
  const getRequesterName = (run: AgentRun): string =>
    conversation.memberList.find((member) => member.id === run.requesterUserId)
      ?.name ||
    (run.requesterUserId ? `用户 ${run.requesterUserId.slice(-4)}` : '会话成员')

  /**
   * 查询触发消息摘要.
   * @param run Agent Run
   * @return 去除 @AI 前缀后的问题摘要
   */
  const getQuestionSummary = (run: AgentRun): string => {
    const triggerMessage = conversation.messages.find(
      (message) => message.id === run.triggerMessageId,
    )
    return (
      triggerMessage?.body.replace(/(^|\s)@AI\s*/iu, ' ').trim() ||
      run.errorMessage ||
      '未加载到触发消息'
    )
  }

  /**
   * 在执行记录接近底部时请求下一页游标数据.
   * @param event Agent 面板滚动事件
   * @return void
   */
  const handleRunListScroll = (event: UIEvent<HTMLElement>): void => {
    const viewport = event.target as HTMLElement
    const bottomDistance =
      viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight
    if (
      bottomDistance <= 64 &&
      controller.hasNextRunList &&
      !controller.isLoadingNextRunList
    ) {
      void controller.loadNextRunList()
    }
  }

  return (
    <ScrollArea
      className="min-h-0 w-full min-w-0 flex-1 [&_[data-radix-scroll-area-viewport]>div]:block! [&_[data-radix-scroll-area-viewport]>div]:w-full!"
      role="tabpanel"
      onScrollCapture={handleRunListScroll}
    >
      <div className="p-4">
        <div
          data-testid="conversation-details-card"
          className="border-border space-y-6 rounded-xl border p-4"
        >
          <section aria-labelledby="agent-basic-settings-title">
            <p
              id="agent-basic-settings-title"
              className="text-muted-foreground mb-3 text-xs font-medium"
            >
              基础配置
            </p>
            {canManageDefinition || canReplaceCredential ? (
              <FieldGroup
                className={cn(
                  'gap-3',
                  controller.isContextLoading &&
                    'pointer-events-none opacity-60',
                )}
              >
                <Field
                  orientation="horizontal"
                  className="min-h-10 items-center justify-between"
                >
                  <FieldLabel
                    htmlFor="agent-enabled"
                    className="text-xs font-medium"
                  >
                    启用 AI
                  </FieldLabel>
                  <Switch
                    id="agent-enabled"
                    checked={draft.enabled}
                    disabled={
                      controller.isContextLoading ||
                      controller.isDefinitionSaving
                    }
                    aria-label="启用会话 AI"
                    onCheckedChange={(enabled) =>
                      void controller.setAgentEnabled(enabled)
                    }
                  />
                </Field>

                <Field
                  orientation="horizontal"
                  className="min-h-10 items-center justify-between"
                >
                  <div>
                    <FieldLabel
                      htmlFor="agent-thinking-enabled"
                      className="text-xs font-medium"
                    >
                      深度思考
                    </FieldLabel>
                    <p className="text-muted-foreground mt-0.5 text-xs">
                      流式展示并保存模型 reasoning
                    </p>
                  </div>
                  <Switch
                    id="agent-thinking-enabled"
                    checked={draft.thinkingEnabled}
                    aria-label="启用深度思考"
                    onCheckedChange={(thinkingEnabled) =>
                      controller.setDefinitionDraft((current) => ({
                        ...current,
                        thinkingEnabled,
                      }))
                    }
                  />
                </Field>

                {draft.thinkingEnabled ? (
                  <AgentConfigSelect
                    id="agent-thinking-effort"
                    label="思考强度"
                    value={draft.thinkingEffort}
                    optionList={[
                      { value: 'low', label: '轻量' },
                      { value: 'medium', label: '标准' },
                      { value: 'high', label: '深度' },
                    ]}
                    onValueChange={(thinkingEffort) =>
                      controller.setDefinitionDraft((current) => ({
                        ...current,
                        thinkingEffort: thinkingEffort as
                          'low' | 'medium' | 'high',
                      }))
                    }
                  />
                ) : null}

                <Field className="gap-1.5">
                  <FieldLabel
                    id="agent-provider-label"
                    className="text-xs font-medium"
                  >
                    提供商
                  </FieldLabel>
                  <InputGroup
                    data-agent-control
                    className="h-10 w-full min-w-0 rounded-[10px]"
                  >
                    <InputGroupInput
                      id="agent-provider"
                      value="DeepSeek"
                      aria-labelledby="agent-provider-label"
                      className="h-10 text-sm font-normal"
                      readOnly
                    />
                    <InputGroupAddon align="inline-end">
                      <InputGroupText className="text-xs font-normal">
                        当前仅支持
                      </InputGroupText>
                    </InputGroupAddon>
                  </InputGroup>
                </Field>

                <AgentConfigSelect
                  id="agent-model"
                  label="模型"
                  value={draft.modelId}
                  optionList={DEEPSEEK_MODEL_OPTION_LIST}
                  onValueChange={(modelId) =>
                    controller.setDefinitionDraft((current) => ({
                      ...current,
                      modelId,
                    }))
                  }
                />

                <Field className="gap-1.5">
                  <FieldLabel
                    htmlFor="agent-api-base"
                    className="text-xs font-medium"
                  >
                    Base URL
                  </FieldLabel>
                  <InputGroup
                    data-agent-control
                    className="h-10 w-full min-w-0 rounded-[10px]"
                  >
                    <InputGroupInput
                      id="agent-api-base"
                      value={credentialDraft.apiBase}
                      aria-label="DeepSeek Base URL"
                      autoComplete="url"
                      className="h-10"
                      onChange={(event) =>
                        controller.setCredentialDraft((current) => ({
                          ...current,
                          apiBase: event.target.value,
                        }))
                      }
                    />
                    <InputGroupAddon align="inline-start">
                      <InputGroupText>
                        <Link2 aria-hidden />
                      </InputGroupText>
                    </InputGroupAddon>
                  </InputGroup>
                </Field>

                {controller.context?.credential ? (
                  <div className="bg-muted/45 flex min-w-0 items-center justify-between gap-3 rounded-lg px-3 py-2 text-xs">
                    <span className="text-muted-foreground shrink-0">
                      当前运行凭证
                    </span>
                    <span className="min-w-0 truncate font-medium">
                      @{credentialOwnerName} ·{' '}
                      {controller.context.credential.keyFingerprint}
                    </span>
                  </div>
                ) : null}

                <Field className="gap-1.5">
                  <FieldLabel
                    htmlFor="agent-api-key"
                    className="text-xs font-medium"
                  >
                    API Key
                  </FieldLabel>
                  <InputGroup
                    data-agent-control
                    className="h-10 w-full min-w-0 rounded-[10px]"
                  >
                    <InputGroupInput
                      id="agent-api-key"
                      type={isApiKeyVisible ? 'text' : 'password'}
                      value={credentialDraft.apiKey}
                      aria-label="DeepSeek API Key"
                      autoComplete="new-password"
                      placeholder={
                        controller.context?.credential
                          ? `已配置 ${controller.context.credential.keyFingerprint}`
                          : 'sk-••••••••••••••••'
                      }
                      className="h-10"
                      onChange={(event) =>
                        controller.setCredentialDraft((current) => ({
                          ...current,
                          apiKey: event.target.value,
                        }))
                      }
                    />
                    <InputGroupAddon align="inline-start">
                      <InputGroupText>
                        <KeyRound aria-hidden />
                      </InputGroupText>
                    </InputGroupAddon>
                    <InputGroupAddon align="inline-end">
                      <InputGroupButton
                        size="icon-xs"
                        aria-label={
                          isApiKeyVisible ? '隐藏 API Key' : '显示 API Key'
                        }
                        aria-pressed={isApiKeyVisible}
                        onClick={() =>
                          setIsApiKeyVisible((current) => !current)
                        }
                      >
                        {isApiKeyVisible ? (
                          <EyeOff aria-hidden />
                        ) : (
                          <Eye aria-hidden />
                        )}
                      </InputGroupButton>
                    </InputGroupAddon>
                  </InputGroup>
                </Field>

                <div className="flex items-center justify-between gap-3 pt-1">
                  <span
                    className={cn(
                      'flex min-w-0 items-center gap-1.5 text-xs',
                      controller.connectionError
                        ? 'text-red-600'
                        : controller.connectionLatency !== null
                          ? 'text-emerald-600'
                          : 'text-muted-foreground',
                    )}
                  >
                    <ConnectionStatusIcon
                      aria-hidden
                      className={cn(
                        'size-3.5 shrink-0',
                        controller.isConnectionTesting &&
                          'animate-spin motion-reduce:animate-none',
                      )}
                    />
                    {connectionStatusLabel}
                  </span>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-9 rounded-lg px-3"
                    disabled={
                      controller.isConnectionTesting ||
                      !controller.context?.canManageCurrentCredential
                    }
                    onClick={() => void controller.testCurrentCredential()}
                  >
                    {controller.isConnectionTesting ? (
                      <LoaderCircle aria-hidden className="animate-spin" />
                    ) : null}
                    测试当前凭证
                  </Button>
                </div>

                {canManageDefinition || canReplaceCredential ? (
                  <Button
                    type="button"
                    className="h-10 w-full rounded-[10px]"
                    disabled={
                      controller.isDefinitionSaving ||
                      controller.isCredentialSaving ||
                      controller.isConnectionTesting ||
                      (!canManageDefinition &&
                        (!credentialDraft.apiKey.trim() ||
                          !controller.context?.agent))
                    }
                    onClick={() => void controller.saveConfiguration()}
                  >
                    {controller.isDefinitionSaving ||
                    controller.isCredentialSaving ? (
                      <LoaderCircle aria-hidden className="animate-spin" />
                    ) : null}
                    保存配置
                  </Button>
                ) : null}
              </FieldGroup>
            ) : (
              <p className="text-muted-foreground text-xs leading-5">
                Agent 定义和凭证由具有管理权限的会话成员维护。
              </p>
            )}
          </section>

          <section aria-labelledby="agent-run-list-title">
            <div className="mb-3 flex items-center justify-between">
              <h3
                id="agent-run-list-title"
                className="text-muted-foreground text-xs font-medium"
              >
                Agent 执行记录
                <span className="text-muted-foreground ml-2 font-normal">
                  {controller.runList.length}
                </span>
              </h3>
              <button
                type="button"
                aria-label="刷新 Agent 执行记录"
                disabled={controller.isRunListLoading}
                className="text-muted-foreground hover:bg-muted hover:text-foreground rounded-md p-1 disabled:pointer-events-none disabled:opacity-50"
                onClick={() => void controller.refreshRunList()}
              >
                <RefreshCw
                  aria-hidden
                  className={cn(
                    'size-3.5',
                    controller.isRunListLoading &&
                      'animate-spin motion-reduce:animate-none',
                  )}
                />
              </button>
            </div>

            {controller.runList.length > 0 ? (
              <>
                <ul className="space-y-1" aria-label="Agent 执行记录">
                  {controller.runList.map((run) => {
                    const snapshot = controller.snapshotByRunId[run.runId]
                    const status = snapshot?.status ?? run.status ?? 'QUEUED'
                    return (
                      <li key={run.runId}>
                        <button
                          type="button"
                          className="hover:bg-muted/70 focus-visible:ring-ring -mx-2 w-[calc(100%+1rem)] rounded-lg px-2 py-2.5 text-left transition-colors outline-none focus-visible:ring-2"
                          onClick={() => controller.focusRun(run)}
                        >
                          <span className="flex items-center gap-2">
                            <AgentRunStatusBadge status={status} />
                            <span className="min-w-0 flex-1 truncate text-xs font-medium">
                              回复 @{getRequesterName(run)}
                            </span>
                            <time className="text-muted-foreground shrink-0 text-[11px]">
                              {formatRunTime(run.queuedAt)}
                            </time>
                          </span>
                          <span className="mt-1 flex items-center gap-2 pl-1">
                            <span className="text-muted-foreground min-w-0 flex-1 truncate text-xs">
                              {getQuestionSummary(run)}
                            </span>
                            <span className="text-muted-foreground shrink-0 text-[11px]">
                              {getAgentRunDurationLabel(run)}
                            </span>
                            <ChevronRight
                              aria-hidden
                              className="text-muted-foreground size-3.5"
                            />
                          </span>
                        </button>
                      </li>
                    )
                  })}
                </ul>
                {controller.hasNextRunList ||
                controller.isLoadingNextRunList ? (
                  <div
                    role="status"
                    aria-live="polite"
                    className="flex justify-center pt-3"
                  >
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="text-muted-foreground h-8 px-3 text-xs"
                      disabled={controller.isLoadingNextRunList}
                      onClick={() => void controller.loadNextRunList()}
                    >
                      {controller.isLoadingNextRunList ? (
                        <LoaderCircle
                          aria-hidden
                          className="animate-spin motion-reduce:animate-none"
                        />
                      ) : null}
                      {controller.isLoadingNextRunList
                        ? '正在加载更多记录'
                        : '加载更多记录'}
                    </Button>
                  </div>
                ) : (
                  <p className="text-muted-foreground pt-3 text-center text-[11px]">
                    已加载全部执行记录
                  </p>
                )}
              </>
            ) : (
              <div className="text-muted-foreground rounded-lg border border-dashed px-3 py-8 text-center text-xs">
                {controller.isRunListLoading
                  ? '正在加载执行记录…'
                  : '暂无 Agent 执行记录'}
              </div>
            )}
          </section>
        </div>
      </div>
    </ScrollArea>
  )
}
