
output "service_name" {
  value = var.release_name
}

output "namespace" {
  value = var.namespace
}

output "http_port" {
  value = 6333
}

output "grpc_port" {
  value = 6334
}

output "internal_http_url" {
  value = "http://${var.release_name}.${var.namespace}.svc.cluster.local:6333"
}

output "internal_grpc_host" {
  value = "${var.release_name}.${var.namespace}.svc.cluster.local"
}