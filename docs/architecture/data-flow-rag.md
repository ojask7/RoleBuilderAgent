# RAG Data Flow

1. Raw exports land in `data/raw` via scripts.
2. Transformation notebooks write parquet artifacts to `data/processed`.
3. Embedding jobs push vectors to pgvector/Cosmos per `collections.yml`.
4. Agents call `VectorStore` via Spring AI and apply `retrieval-policies.yml` parameters.
