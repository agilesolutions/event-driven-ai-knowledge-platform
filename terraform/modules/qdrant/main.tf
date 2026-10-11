
resource "kubernetes_namespace_v1" "qdrant" {
  metadata {
    name = var.namespace
    labels = {
      app        = "qdrant"
      managed-by = "terraform"
    }
  }
}

resource "helm_release" "qdrant" {
  name       = var.release_name
  repository = "https://qdrant.github.io/qdrant-helm"
  chart      = "qdrant"
  namespace  = kubernetes_namespace_v1.qdrant.metadata[0].name

  # Pin this after checking the chart versions available in your environment.
  version = var.chart_version

  atomic          = true
  cleanup_on_fail = true
  wait            = true
  timeout         = 600

  values = [
    yamlencode({
      replicaCount = 1

      service = {
        type = "ClusterIP"
      }

      persistence = {
        size = var.storage_size
      }

      resources = {
        requests = {
          cpu    = var.cpu_request
          memory = var.memory_request
        }
        limits = {
          cpu    = var.cpu_limit
          memory = var.memory_limit
        }
      }
    })
  ]

  depends_on = [kubernetes_namespace_v1.qdrant]
}