"""模型 API Key AEAD 加密。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:49
"""

import base64
import binascii
import hashlib
import os
from dataclasses import dataclass

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

from union_talk_agent.agent_config.domain.constants import API_KEY_FINGERPRINT_LENGTH
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode


@dataclass(frozen=True, slots=True)
class EncryptedApiKey:
    """API Key 加密结果。"""

    ciphertext: bytes
    nonce: bytes
    fingerprint: str


class ApiKeyCipher:
    """使用 AES-GCM 加密和解密模型 API Key。"""

    _ASSOCIATED_DATA = b"union-talk-agent-provider-key:v1"
    _NONCE_LENGTH = 12

    def __init__(self, encoded_master_key: str) -> None:
        """
        初始化 ApiKeyCipher

        :param encoded_master_key: Base64 编码的主密钥
        :return: 无返回值
        """

        self._encoded_master_key = encoded_master_key

    def encrypt(self, api_key: str) -> EncryptedApiKey:
        """
        加密 API Key

        :param api_key: Provider API Key 明文
        :return: 密文、随机 Nonce 和指纹
        """

        key = self._master_key()
        nonce = os.urandom(self._NONCE_LENGTH)
        ciphertext = AESGCM(key).encrypt(nonce, api_key.encode(), self._ASSOCIATED_DATA)
        fingerprint = hashlib.sha256(api_key.encode()).hexdigest()[:API_KEY_FINGERPRINT_LENGTH]
        return EncryptedApiKey(
            ciphertext=ciphertext,
            nonce=nonce,
            fingerprint=fingerprint,
        )

    def decrypt(self, ciphertext: bytes, nonce: bytes) -> str:
        """
        解密 API Key

        :param ciphertext: AES-GCM 密文
        :param nonce: 加密随机数
        :return: Provider API Key 明文
        """

        key = self._master_key()
        plaintext = AESGCM(key).decrypt(nonce, ciphertext, self._ASSOCIATED_DATA)
        return plaintext.decode()

    def _master_key(self) -> bytes:
        """
        解析并校验 API Key 主密钥

        :return: 32 字节 API Key 主密钥
        """

        if not self._encoded_master_key:
            raise AgentDomainError(
                AgentErrorCode.CONFIG_NOT_FOUND,
                "未配置 AGENT_API_KEY_MASTER_KEY",
            )
        try:
            key = base64.b64decode(
                self._encoded_master_key,
                altchars=b"-_",
                validate=True,
            )
        except (ValueError, binascii.Error) as error:
            raise AgentDomainError(
                AgentErrorCode.REQUEST_INVALID,
                "AGENT_API_KEY_MASTER_KEY 不是有效 Base64",
            ) from error
        if len(key) != 32:
            raise AgentDomainError(
                AgentErrorCode.REQUEST_INVALID,
                "AGENT_API_KEY_MASTER_KEY 解码后必须是 32 字节",
            )
        return key
