{{- define "ai-agent-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "ai-agent-platform.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "ai-agent-platform.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "ai-agent-platform.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" -}}
{{- end -}}
