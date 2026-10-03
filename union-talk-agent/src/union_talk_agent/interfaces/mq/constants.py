"""Message Service RabbitMQ 契约常量。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:07
"""

MESSAGE_EXCHANGE = "union.talk.message.exchange"
FILE_EXCHANGE = "union.talk.file.exchange"
AGENT_MENTIONED_QUEUE = "union.talk.message.agent.mentioned.queue"
AGENT_MENTIONED_ROUTING_KEY = "union.talk.message.agent.mentioned"
AGENT_MENTIONED_EVENT_TYPE = "AGENT_MENTIONED"
AGENT_MENTIONED_SCHEMA_VERSION = 1
MAX_EVENT_BYTES = 256 * 1024
LEGACY_EVENT_TIMEZONE_NAME = "Asia/Shanghai"
MESSAGE_CREATED_INDEX_QUEUE = "union.talk.message.created.agent.index.queue"
MESSAGE_CREATED_ROUTING_KEY = "union.talk.message.created"
MESSAGE_RECALLED_INDEX_QUEUE = "union.talk.message.recalled.agent.index.queue"
MESSAGE_RECALLED_ROUTING_KEY = "union.talk.message.recalled"
MESSAGE_DELETED_INDEX_QUEUE = "union.talk.message.deleted.agent.index.queue"
MESSAGE_DELETED_ROUTING_KEY = "union.talk.message.deleted"
ASSET_CONTENT_CHANGED_INDEX_QUEUE = "union.talk.file.asset.content.changed.agent.index.queue"
ASSET_CONTENT_CHANGED_ROUTING_KEY = "union.talk.file.asset.content.changed"
ASSET_DELETED_INDEX_QUEUE = "union.talk.file.asset.deleted.agent.index.queue"
ASSET_DELETED_ROUTING_KEY = "union.talk.file.asset.deleted"
