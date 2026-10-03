import { ConversationDetailsPanel } from '@/components/home/details/conversation-details-panel'
import { NotificationDetailsPanel } from '@/components/home/details/notification-details-panel'
import type {
  ProcessingFriendRelation,
  ProcessingGroupManagement,
} from '@/hooks/use-home-conversations'
import type { ConversationAgentController } from '@/hooks/use-conversation-agent'
import type { GroupInformationFormInput } from '@/pages/home/model/group-information'
import type {
  Conversation,
  FriendOption,
  NotificationItem,
  NotificationStatus,
} from '@/pages/home/model/types'

type DetailsPanelProps =
  | {
      kind: 'conversation'
      conversation: Conversation
      fileRefreshVersion: number
      processingFriendRelation: ProcessingFriendRelation | null
      onBlockFriend: (targetUserId: string) => Promise<boolean>
      onUnblockFriend: (targetUserId: string) => Promise<boolean>
      onDeleteFriend: (targetUserId: string) => Promise<boolean>
      onUpdateFriendRemark: (
        targetUserId: string,
        remark: string,
      ) => Promise<boolean>
      canManageGroup: boolean
      processingGroupId: string
      currentUserId: string
      friendOptionList: FriendOption[]
      processingGroupManagement: ProcessingGroupManagement | null
      onUpdateGroupInformation: (
        groupId: string,
        input: GroupInformationFormInput,
      ) => Promise<boolean>
      onInviteGroupMembers: (
        groupId: string,
        userIdList: string[],
      ) => Promise<boolean>
      onKickGroupMember: (
        groupId: string,
        targetUserId: string,
      ) => Promise<boolean>
      onLeaveGroup: (groupId: string) => Promise<boolean>
      onDissolveGroup: (groupId: string) => Promise<boolean>
      agentController: ConversationAgentController
    }
  | {
      kind: 'notification'
      notification: NotificationItem
      onSetNotificationStatus: (
        notificationId: string,
        status: NotificationStatus,
      ) => void
    }

/**
 * 根据详情领域类型委派对应面板.
 * @param props 详情面板属性
 * @return ReactElement 对应领域详情面板
 */
export const DetailsPanel = (props: DetailsPanelProps) => {
  if (props.kind === 'conversation') {
    return (
      <ConversationDetailsPanel
        conversation={props.conversation}
        fileRefreshVersion={props.fileRefreshVersion}
        processingFriendRelation={props.processingFriendRelation}
        onBlockFriend={props.onBlockFriend}
        onUnblockFriend={props.onUnblockFriend}
        onDeleteFriend={props.onDeleteFriend}
        onUpdateFriendRemark={props.onUpdateFriendRemark}
        canManageGroup={props.canManageGroup}
        processingGroupId={props.processingGroupId}
        currentUserId={props.currentUserId}
        friendOptionList={props.friendOptionList}
        processingGroupManagement={props.processingGroupManagement}
        onUpdateGroupInformation={props.onUpdateGroupInformation}
        onInviteGroupMembers={props.onInviteGroupMembers}
        onKickGroupMember={props.onKickGroupMember}
        onLeaveGroup={props.onLeaveGroup}
        onDissolveGroup={props.onDissolveGroup}
        agentController={props.agentController}
      />
    )
  }

  return (
    <NotificationDetailsPanel
      notification={props.notification}
      onSetNotificationStatus={props.onSetNotificationStatus}
    />
  )
}
