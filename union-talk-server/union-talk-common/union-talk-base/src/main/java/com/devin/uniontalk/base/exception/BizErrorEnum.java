package com.devin.uniontalk.base.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2026/5/16 20:16.
 *
 * <p>
 * 业务异常枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum BizErrorEnum implements BaseError {

    DUPLICATE_REQUEST(10001, "重复请求"),
    DUPLICATE_ENTITY(10002, "重复数据"),
    USER_NOT_FOUND(10003, "用户不存在"),
    PASSWORD_ERROR(10004, "密码错误"),
    USER_BLOCKED(10005, "用户已被封禁"),
    FRIEND_REQUEST_NOT_FOUND(10006, "好友申请不存在"),
    FRIEND_REQUEST_ALREADY_SENT(10007, "已发送过好友申请"),
    ALREADY_FRIENDS(10008, "已经是好友关系"),
    FRIEND_GROUP_NOT_FOUND(10009, "好友分组不存在"),
    GROUP_NOT_FOUND(10010, "群聊不存在"),
    GROUP_MEMBER_LIMIT(10011, "群成员已达上限"),
    NOT_GROUP_OWNER(10012, "非群主无权操作"),
    ALREADY_IN_GROUP(10013, "已在群聊中"),
    GROUP_MEMBER_MIN_LIMIT(10014, "群成员不能少于3人"),
    CURRENT_PASSWORD_ERROR(10015, "当前密码错误"),
    CONVERSATION_NOT_FOUND(20001, "会话不存在"),
    NOT_CONVERSATION_MEMBER(20002, "非会话成员"),
    MESSAGE_NOT_FOUND(20003, "消息不存在"),
    MESSAGE_CONTENT_EMPTY(20004, "消息内容不能为空"),
    MESSAGE_TYPE_UNSUPPORTED(20005, "不支持的消息类型"),
    MESSAGE_CONTENT_TOO_LONG(20006, "消息内容长度超限"),
    CONVERSATION_CREATE_FAILED(20007, "会话创建失败"),
    CONVERSATION_TARGET_QUERY_FAILED(20008, "会话目标信息查询失败"),
    CONVERSATION_DISSOLVE_FAILED(20009, "会话解散失败"),
    MESSAGE_ASSET_ID_INVALID(20010, "消息资产文件id非法"),
    MESSAGE_ASSET_QUERY_FAILED(20011, "消息资产文件查询失败"),
    MESSAGE_ASSET_TYPE_MISMATCH(20012, "消息类型与资产文件类型不匹配"),
    MESSAGE_MENTION_TYPE_INVALID(20013, "只有文本消息可以包含提及信息"),
    AGENT_REPLY_PARAM_INVALID(20014, "Agent回复参数非法"),
    AGENT_REPLY_TRIGGER_INVALID(20015, "Agent触发消息非法"),
    AGENT_REPLY_IDEMPOTENCY_KEY_INVALID(20016, "Agent回复幂等键非法"),
    MESSAGE_MENTION_PARAM_INVALID(20017, "消息提及参数非法"),
    MESSAGE_MENTION_TARGET_INVALID(20018, "消息提及目标不在当前会话"),
    ASSET_FOLDER_NOT_FOUND(30001, "资产目录不存在"),
    ASSET_FILE_NOT_FOUND(30002, "资产文件不存在"),
    ASSET_NAME_DUPLICATE(30003, "同名资产已存在"),
    ASSET_FOLDER_MOVE_INVALID(30004, "资产目录移动目标非法"),
    ASSET_FILE_RENAME_EXT_CHANGED(30005, "文件重命名不能修改后缀"),
    UPLOAD_SESSION_NOT_FOUND(30006, "上传任务不存在"),
    UPLOAD_SESSION_STATUS_INVALID(30007, "上传任务状态不允许当前操作"),
    UPLOAD_CHUNK_MISSING(30008, "上传分片不完整"),
    UPLOAD_CHUNK_HASH_MISMATCH(30009, "上传分片摘要不一致"),
    UPLOAD_PARAM_INVALID(30010, "上传参数非法"),
    STORAGE_OBJECT_FAILED(30011, "对象存储操作失败"),
    UPLOAD_ERROR(30012, "上传失败"),
    ASSET_PERMISSION_DENIED(30013, "无资产操作权限"),
    ASSET_SYSTEM_FOLDER_IMMUTABLE(30014, "系统资产目录不允许修改"),
    ASSET_TARGET_FOLDER_INVALID(30015, "资产目标目录非法"),
    ASSET_ACCESS_CONTEXT_QUERY_FAILED(30016, "资产访问上下文查询失败");

    /**
     * 错误码.
     */
    private final Integer errCode;

    /**
     * 错误信息.
     */
    private final String errMsg;

    @Override
    public Integer getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }
}
