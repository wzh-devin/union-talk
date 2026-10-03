import { useState } from 'react'

import { ConversationFileTree } from '@/components/home/details/conversation-file-tree'
import { ConversationAgentPanel } from '@/components/home/details/conversation-agent-panel'
import { ScrollArea } from '@/components/shadcn-ui/scroll-area'
import { DetailGroup } from '@/components/home/details/detail-group'
import { FriendRelationSettings } from '@/components/home/details/friend-relation-settings'
import { GroupInformationSettings } from '@/components/home/details/group-information-settings'
import { GroupMemberListDialog } from '@/components/home/details/group-member-list-dialog'
import { NamedAvatar } from '@/components/home/shared/named-avatar'
import type {
  ProcessingFriendRelation,
  ProcessingGroupManagement,
} from '@/hooks/use-home-conversations'
import type { ConversationAgentController } from '@/hooks/use-conversation-agent'
import type { GroupInformationFormInput } from '@/pages/home/model/group-information'
import type { Conversation, FriendOption } from '@/pages/home/model/types'
import { getMemberAvatarAccent } from '@/utils/avatar'

interface ConversationDetailsPanelProps {
  conversation: Conversation
  fileRefreshVersion?: number
  processingFriendRelation?: ProcessingFriendRelation | null
  onBlockFriend?: (targetUserId: string) => Promise<boolean>
  onUnblockFriend?: (targetUserId: string) => Promise<boolean>
  onDeleteFriend?: (targetUserId: string) => Promise<boolean>
  onUpdateFriendRemark?: (
    targetUserId: string,
    remark: string,
  ) => Promise<boolean>
  canManageGroup?: boolean
  processingGroupId?: string
  currentUserId?: string
  friendOptionList?: FriendOption[]
  processingGroupManagement?: ProcessingGroupManagement | null
  onUpdateGroupInformation?: (
    groupId: string,
    input: GroupInformationFormInput,
  ) => Promise<boolean>
  onInviteGroupMembers?: (
    groupId: string,
    userIdList: string[],
  ) => Promise<boolean>
  onKickGroupMember?: (
    groupId: string,
    targetUserId: string,
  ) => Promise<boolean>
  onLeaveGroup?: (groupId: string) => Promise<boolean>
  onDissolveGroup?: (groupId: string) => Promise<boolean>
  agentController?: ConversationAgentController
}

type DetailsTab = 'info' | 'files' | 'agent'

const baseDetailTabList: Array<{ id: DetailsTab; label: string }> = [
  { id: 'info', label: 'Info' },
  { id: 'files', label: 'Files' },
]

/**
 * 展示会话基本信息与成员头像预览.
 * @param props 会话详情属性
 * @return ReactElement 会话详情面板
 */
export const ConversationDetailsPanel = ({
  conversation,
  fileRefreshVersion = 0,
  processingFriendRelation = null,
  onBlockFriend,
  onUnblockFriend,
  onDeleteFriend,
  onUpdateFriendRemark,
  canManageGroup = false,
  processingGroupId = '',
  currentUserId = '',
  friendOptionList = [],
  processingGroupManagement = null,
  onUpdateGroupInformation,
  onInviteGroupMembers,
  onKickGroupMember,
  onLeaveGroup,
  onDissolveGroup,
  agentController,
}: ConversationDetailsPanelProps) => {
  const [selectedTab, setSelectedTab] = useState<{
    conversationId: string
    tab: DetailsTab
  }>({ conversationId: conversation.id, tab: 'info' })
  const activeTab =
    selectedTab.conversationId === conversation.id ? selectedTab.tab : 'info'
  const detailTabList = agentController
    ? [...baseDetailTabList, { id: 'agent' as const, label: 'Agent' }]
    : baseDetailTabList
  const orderedMemberList = [...conversation.memberList].sort(
    (firstMember, secondMember) =>
      Number(secondMember.role === 'owner') -
      Number(firstMember.role === 'owner'),
  )
  const previewMemberList = orderedMemberList.slice(0, 4)
  const remainingMemberCount = Math.max(
    orderedMemberList.length - previewMemberList.length,
    0,
  )
  const detailItemList: Array<[string, string]> =
    conversation.kind === 'group'
      ? [
          ['说明', conversation.description],
          ['状态', conversation.status],
          ['最后活跃', conversation.lastActive],
          ['消息数', String(conversation.messages.length)],
        ]
      : [
          ['说明', conversation.description],
          [
            '状态',
            conversation.friendRelationStatus === 'blocked'
              ? '已拉黑'
              : conversation.status,
          ],
          ['编号', conversation.code],
          ['最后活跃', conversation.lastActive],
          ['消息数', String(conversation.messages.length)],
        ]
  const memberPreviewContent = (
    <>
      <span className="text-muted-foreground mb-2 block text-xs font-medium">
        {conversation.kind === 'group' ? '成员预览' : '对话成员'}
      </span>
      <div
        aria-label={`${conversation.name}成员头像`}
        data-testid="conversation-member-avatar-circles"
      >
        <div className="z-10 flex -space-x-4 rtl:space-x-reverse">
          {previewMemberList.map((member, index) => (
            <NamedAvatar
              key={member.id}
              name={member.name}
              avatarUrl={member.avatarUrl}
              className="size-10 border-2 border-white dark:border-gray-800"
              fallbackClassName={getMemberAvatarAccent(index)}
            />
          ))}
          {remainingMemberCount > 0 ? (
            <span
              aria-label={`另外 ${remainingMemberCount} 位成员`}
              className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-white bg-black text-center text-xs font-medium text-white dark:border-gray-800 dark:bg-white dark:text-black"
            >
              +{remainingMemberCount}
            </span>
          ) : null}
        </div>
      </div>
    </>
  )

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div
        className="border-border flex h-14 shrink-0 items-center gap-1 border-b px-4"
        role="tablist"
        aria-label="会话详情"
      >
        {detailTabList.map((tab) => {
          const isActive = tab.id === activeTab

          return (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={isActive}
              className={
                isActive
                  ? 'bg-muted text-foreground rounded-lg px-3 py-1.5 text-sm font-medium'
                  : 'text-muted-foreground hover:bg-muted/60 hover:text-foreground rounded-lg px-3 py-1.5 text-sm font-medium transition-colors'
              }
              onClick={() =>
                setSelectedTab({ conversationId: conversation.id, tab: tab.id })
              }
            >
              {tab.label}
            </button>
          )
        })}
      </div>
      {activeTab === 'info' ? (
        <ScrollArea className="min-h-0 flex-1" role="tabpanel">
          <div className="p-4">
            <div
              data-testid="conversation-details-card"
              className="border-border space-y-6 rounded-xl border p-4"
            >
              <DetailGroup title="基本信息" detailItemList={detailItemList} />
              {conversation.kind === 'group' ? (
                <GroupMemberListDialog
                  groupId={conversation.groupId || ''}
                  groupName={conversation.name}
                  memberList={orderedMemberList}
                  currentUserId={currentUserId}
                  canKickMembers={canManageGroup}
                  processingGroupManagement={processingGroupManagement}
                  onKickGroupMember={onKickGroupMember}
                  trigger={
                    <button
                      type="button"
                      aria-label={`查看${conversation.name}全部成员`}
                      className="hover:bg-muted/70 focus-visible:ring-ring -mx-2 w-[calc(100%+1rem)] rounded-lg px-2 py-2 text-left transition-colors outline-none focus-visible:ring-2"
                    >
                      {memberPreviewContent}
                    </button>
                  }
                />
              ) : (
                <div>{memberPreviewContent}</div>
              )}
              {conversation.kind === 'group' &&
              conversation.groupId &&
              onUpdateGroupInformation ? (
                <GroupInformationSettings
                  groupId={conversation.groupId}
                  groupName={conversation.name}
                  description={conversation.description}
                  memberLimit={conversation.groupMemberLimit ?? 100}
                  currentMemberCount={conversation.memberList.length}
                  isSaving={processingGroupId === conversation.groupId}
                  canEdit={canManageGroup}
                  memberList={conversation.memberList}
                  friendOptionList={friendOptionList}
                  processingGroupManagement={processingGroupManagement}
                  onUpdateGroupInformation={onUpdateGroupInformation}
                  onInviteGroupMembers={onInviteGroupMembers}
                  onLeaveGroup={onLeaveGroup}
                  onDissolveGroup={onDissolveGroup}
                />
              ) : null}
              {conversation.kind === 'friend' &&
              conversation.targetUserId &&
              onBlockFriend &&
              onUnblockFriend &&
              onDeleteFriend &&
              onUpdateFriendRemark ? (
                <FriendRelationSettings
                  friendName={conversation.name}
                  friendRemark={conversation.friendRemark || ''}
                  targetUserId={conversation.targetUserId}
                  isBlocked={conversation.friendRelationStatus === 'blocked'}
                  processingAction={
                    processingFriendRelation?.targetUserId ===
                    conversation.targetUserId
                      ? processingFriendRelation.action
                      : undefined
                  }
                  onBlockFriend={onBlockFriend}
                  onUnblockFriend={onUnblockFriend}
                  onDeleteFriend={onDeleteFriend}
                  onUpdateFriendRemark={onUpdateFriendRemark}
                />
              ) : null}
            </div>
          </div>
        </ScrollArea>
      ) : activeTab === 'files' ? (
        <div className="min-h-0 flex-1 p-4" role="tabpanel">
          <div
            data-testid="conversation-details-card"
            className="border-border h-full rounded-xl border p-4"
          >
            <ConversationFileTree
              conversationId={conversation.id}
              fileRefreshVersion={fileRefreshVersion}
            />
          </div>
        </div>
      ) : agentController ? (
        <ConversationAgentPanel
          conversation={conversation}
          controller={agentController}
        />
      ) : null}
    </div>
  )
}
