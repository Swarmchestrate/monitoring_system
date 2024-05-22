{{/* _helpers.tpl */}}
{{- define "simple-app-prometheus.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name }}
{{- end -}}

{{- define "simple-app-prometheus.labels" -}}
app.kubernetes.io/name: {{ include "simple-app-prometheus.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "simple-app-prometheus.selectorLabels" -}}
app.kubernetes.io/name: {{ include "simple-app-prometheus.name" . }}
{{- end -}}

{{- define "simple-app-prometheus.name" -}}
{{- default "simple-app-prometheus" .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}
