# SRE Playbook

- **Alert Sources**: Prometheus rules + Azure Monitor.
- **First Response**: Validate Azure OpenAI quota, database connectivity, and error logs.
- **Escalation**: Notify IAM platform team if outage > 30 min or KC27 agent unavailable during audit windows.
