# Sequence Diagrams

1. **SG Mapping Flow**
   1. Client calls `/api/agents/sg/mapping`.
   2. Agent orchestrator fetches SailPoint + CMDB context and prompts Azure OpenAI.
   3. Response stored/audited and returned to client.

2. **KC27 Verification Flow**
   1. Client posts SG payload.
   2. Vector store surfaces evidence, ChatClient summarizes.
   3. KC27 service labels decision and logs.
