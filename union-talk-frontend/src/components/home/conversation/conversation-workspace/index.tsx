import { useRef } from 'react'

import { useHomeRouteContext } from '@/hooks/use-home-route-context'
import { useConversationAgent } from '@/hooks/use-conversation-agent'
import { ChatPanel } from '@/components/home/conversation/chat-panel'
import { DetailsPanel } from '@/components/home/details/details-panel'
import { ConversationListActions } from '@/components/home/list/conversation-list-actions'
import { ListPanel } from '@/components/home/list/list-panel'
import { WorkspaceSurface } from '@/components/home/shared/workspace-surface'
import { hasComposerMentionType } from '@/pages/home/model/composer-document'
import { messageMentionType } from '@/services/message/message-contract'

interface ConversationWorkspaceProps {
  section: 'friends' | 'groups'
}

/**
 * 编排好友或群聊路由页面的工作台内容.
 * @param props 会话工作台属性
 * @return 会话工作台
 */
export const ConversationWorkspace = ({
  section,
}: ConversationWorkspaceProps) => {
  const { workspace, detailsPanel } = useHomeRouteContext()
  const mobileBackButtonRef = useRef<HTMLButtonElement>(null)
  const conversationList =
    section === 'groups'
      ? workspace.groupConversationList
      : workspace.friendConversationList
  const conversation = workspace.activeConversation
  const canManageGroup = Boolean(
    conversation?.kind === 'group' &&
    conversation.ownerId === workspace.profile.userId,
  )
  const agentController = useConversationAgent({
    conversation,
    onRefreshConversationMessages: workspace.refreshConversationMessages,
  })

  /**
   * 发送消息，并在 @AI 触发后立即刷新 Run 列表.
   * @return 消息发送流程
   */
  const sendMessage = async (): Promise<void> => {
    const shouldRefreshAgent = hasComposerMentionType(
      workspace.composerDocument,
      messageMentionType.AGENT,
    )
    await workspace.sendMessage()
    if (shouldRefreshAgent) {
      setTimeout(() => void agentController.refreshRunList(), 600)
    }
  }

  const detailsContent = conversation ? (
    <DetailsPanel
      kind="conversation"
      conversation={conversation}
      fileRefreshVersion={
        workspace.conversationFileRefreshVersionByConversationId[
          conversation.id
        ] ?? 0
      }
      processingFriendRelation={workspace.processingFriendRelation}
      onBlockFriend={workspace.blockFriend}
      onUnblockFriend={workspace.unblockFriend}
      onDeleteFriend={workspace.deleteFriend}
      onUpdateFriendRemark={workspace.updateFriendRemark}
      canManageGroup={canManageGroup}
      processingGroupId={workspace.processingGroupId}
      currentUserId={workspace.profile.userId}
      friendOptionList={workspace.friendOptionList}
      processingGroupManagement={workspace.processingGroupManagement}
      onUpdateGroupInformation={workspace.updateGroupInformation}
      onInviteGroupMembers={workspace.inviteGroupMembers}
      onKickGroupMember={workspace.kickGroupMember}
      onLeaveGroup={workspace.leaveGroup}
      onDissolveGroup={workspace.dissolveGroup}
      agentController={agentController}
    />
  ) : null

  return (
    <WorkspaceSurface
      section={section}
      listContent={
        <ListPanel
          kind="conversation"
          activeSection={section}
          conversationList={conversationList}
          selectedId={workspace.selectedIdBySection[section]}
          toolbarAction={
            <ConversationListActions
              section={section}
              friendConversationList={workspace.friendOptionList}
              mobileSuccessFocusRef={mobileBackButtonRef}
              onSendFriendRequest={workspace.sendLocalFriendRequest}
              onRefreshFriendList={workspace.refreshFriendOptionList}
              onCreateGroup={workspace.createLocalGroup}
            />
          }
          onSelectConversation={(conversationId) =>
            workspace.selectConversation(section, conversationId)
          }
        />
      }
      mainContent={
        conversation ? (
          <ChatPanel
            conversation={conversation}
            currentUserName={workspace.profile.displayName}
            isDetailsOpen={workspace.isDetailsOpen}
            composerDocument={workspace.composerDocument}
            backButtonRef={mobileBackButtonRef}
            onBackToList={workspace.showMobileList}
            onToggleDetails={workspace.toggleDetailsPanel}
            onOpenDetailsSheet={() => workspace.setDetailsSheetOpen(true)}
            onComposerChange={workspace.updateComposerDocument}
            onSendMessage={sendMessage}
            onSendEmoji={workspace.sendEmoji}
            onSelectAttachmentList={workspace.selectAttachmentList}
            uploadState={workspace.messageUploadState || undefined}
            hasNextMessages={workspace.hasNextMessages}
            isLoadingNextMessages={workspace.isLoadingNextMessages}
            onLoadNextMessages={workspace.loadNextMessages}
            onLatestReceivedMessageViewed={workspace.markConversationRead}
            agentController={agentController}
          />
        ) : null
      }
      detailsContent={detailsContent}
      isMobileListOpen={workspace.isMobileListOpen}
      isDetailsOpen={workspace.isDetailsOpen}
      isDetailsSheetOpen={workspace.isDetailsSheetOpen}
      detailsPanelWidth={detailsPanel.detailsPanelWidth}
      detailsPanelMaxWidth={detailsPanel.detailsPanelMaxWidth}
      isResizingDetailsPanel={detailsPanel.isResizingDetailsPanel}
      onDetailsSheetOpenChange={workspace.setDetailsSheetOpen}
      onBeginDetailsPanelResize={detailsPanel.beginDetailsPanelResize}
      onDetailsPanelResizeKeyDown={detailsPanel.handleDetailsPanelResizeKeyDown}
      onResetDetailsPanelWidth={detailsPanel.resetDetailsPanelWidth}
    />
  )
}
