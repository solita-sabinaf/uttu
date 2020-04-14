terraform {
  required_version = ">= 0.12"
}

provider "google" {
  version = "~> 2.19"
}

provider "kubernetes" {
  load_config_file = var.load_config_file
}

resource "google_service_account" "uttu_service_account" {
  account_id   = "${var.labels.team}-${var.labels.app}-sa"
  display_name = "${var.labels.team}-${var.labels.app} service account"
  project = var.gcp_project
}

resource "google_project_iam_member" "project" {
  project = var.gcp_project
  role    = var.service_account_cloudsql_role
  member = "serviceAccount:${google_service_account.uttu_service_account.email}"
}

resource "google_service_account_key" "uttu_service_account_key" {
  service_account_id = google_service_account.uttu_service_account.name
}

resource "kubernetes_secret" "uttu_service_account_credentials" {
  metadata {
    name      = "${var.labels.team}-${var.labels.app}-sa-key"
    namespace = var.kube_namespace
  }
  data = {
    "credentials.json" = "${base64decode(google_service_account_key.uttu_service_account_key.private_key)}"
  }
}

resource "kubernetes_secret" "ror-uttu-db-password" {
  metadata {
    name      = "${var.labels.team}-${var.labels.app}-db-password"
    namespace = var.kube_namespace
  }

  data = {
    "password" = var.ror-uttu-db-password
  }
}