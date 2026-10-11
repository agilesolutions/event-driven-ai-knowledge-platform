
variable "namespace" {
  type    = string
  default = "ai"
}

variable "release_name" {
  type    = string
  default = "qdrant"
}

variable "chart_version" {
  description = "Pin to a version available in the official Qdrant Helm repository."
  type        = string
}

variable "storage_size" {
  type    = string
  default = "5Gi"
}

variable "cpu_request" {
  type    = string
  default = "250m"
}

variable "memory_request" {
  type    = string
  default = "512Mi"
}

variable "cpu_limit" {
  type    = string
  default = "1"
}

variable "memory_limit" {
  type    = string
  default = "1Gi"
}