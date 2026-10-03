"""BGE 本地向量模型输出验证脚本。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/06 22:16
"""

from sentence_transformers import SentenceTransformer

model = SentenceTransformer("BAAI/bge-m3")

texts = [
    "The weather is lovely today.",
    "It is sunny outside.",
    "He drove to the stadium.",
]

embeddings = model.encode(texts, normalize_embeddings=True)

print("向量形状:", embeddings.shape)  # (3, 384)
print("相似度矩阵:")
print(embeddings @ embeddings.T)
