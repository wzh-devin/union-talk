import { useRef, useState, type RefObject } from 'react'
import { MoreHorizontal, UserPlus, UsersRound } from 'lucide-react'

import { Button } from '@/components/shadcn-ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/shadcn-ui/dropdown-menu'
import { AddFriendDialog } from '@/components/home/list/add-friend-dialog'
import { CreateGroupDialog } from '@/components/home/list/create-group-dialog'
import type {
  AddFriendFormInput,
  CreateGroupFormInput,
  FriendOption,
} from '@/pages/home/model/types'

interface ConversationListActionsProps {
  section: 'friends' | 'groups'
  friendConversationList: FriendOption[]
  mobileSuccessFocusRef: RefObject<HTMLButtonElement | null>
  onSendFriendRequest: (input: AddFriendFormInput) => void | Promise<void>
  onRefreshFriendList: () => void | Promise<void>
  onCreateGroup: (input: CreateGroupFormInput) => void | Promise<void>
}

type ActiveDialog = 'add-friend' | 'create-group' | null

/**
 * 根据会话分区展示列表新增菜单与对应表单.
 * @param props 会话列表操作属性
 * @return 会话列表操作入口
 */
export const ConversationListActions = ({
  section,
  friendConversationList,
  mobileSuccessFocusRef,
  onSendFriendRequest,
  onRefreshFriendList,
  onCreateGroup,
}: ConversationListActionsProps) => {
  const [activeDialog, setActiveDialog] = useState<ActiveDialog>(null)
  const actionButtonRef = useRef<HTMLButtonElement>(null)

  /**
   * 打开创建群聊弹窗并刷新服务端好友列表.
   * @return void
   */
  const handleCreateGroupSelect = (): void => {
    setActiveDialog('create-group')
    void onRefreshFriendList()
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            ref={actionButtonRef}
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label="列表更多操作"
            className="text-muted-foreground hover:bg-muted hover:text-foreground shrink-0"
          >
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {section === 'friends' ? (
            <DropdownMenuItem onSelect={() => setActiveDialog('add-friend')}>
              <UserPlus />
              添加好友
            </DropdownMenuItem>
          ) : (
            <DropdownMenuItem onSelect={handleCreateGroupSelect}>
              <UsersRound />
              创建群聊
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      <AddFriendDialog
        open={activeDialog === 'add-friend'}
        returnFocusRef={actionButtonRef}
        onOpenChange={(isOpen) => setActiveDialog(isOpen ? 'add-friend' : null)}
        onSubmit={onSendFriendRequest}
      />
      <CreateGroupDialog
        open={activeDialog === 'create-group'}
        returnFocusRef={actionButtonRef}
        successFocusRef={mobileSuccessFocusRef}
        friendConversationList={friendConversationList}
        onOpenChange={(isOpen) =>
          setActiveDialog(isOpen ? 'create-group' : null)
        }
        onSubmit={onCreateGroup}
      />
    </>
  )
}
