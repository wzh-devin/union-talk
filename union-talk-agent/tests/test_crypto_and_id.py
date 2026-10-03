"""密钥加密和 Snowflake ID 测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:22
"""

import base64
import os

from union_talk_agent.infrastructure.crypto.api_key_cipher import ApiKeyCipher
from union_talk_agent.infrastructure.id_generation.snowflake_id_generator import (
    SnowflakeIdGenerator,
)


def test_api_key_cipher_never_stores_plaintext() -> None:
    """
    AES-GCM 密文可解密且不包含明文

    :return: 无返回值
    """

    master_key = base64.urlsafe_b64encode(os.urandom(32)).decode()
    cipher = ApiKeyCipher(master_key)
    encrypted = cipher.encrypt("deepseek-secret-key")

    assert b"deepseek-secret-key" not in encrypted.ciphertext
    assert cipher.decrypt(encrypted.ciphertext, encrypted.nonce) == "deepseek-secret-key"
    assert len(encrypted.fingerprint) == 16


def test_snowflake_ids_are_positive_and_monotonic() -> None:
    """
    单节点连续生成的 ID 严格递增

    :return: 无返回值
    """

    generator = SnowflakeIdGenerator(worker_id=1, datacenter_id=1)
    id_list = [generator.next_id() for _ in range(1000)]

    assert id_list == sorted(id_list)
    assert len(set(id_list)) == len(id_list)
    assert id_list[0] > 0
