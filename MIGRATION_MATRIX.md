# GCP to AWS Migration Matrix

Inventory scope: service directories under `src/`, root `kubernetes-manifests/`, shared deployment components, IaC, CI/CD, and optional deployment/config directories under `extras/` and `.github/`.

## userservice

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| userservice | Python 3.14, Flask, SQLAlchemy | `google-api-core`, `google-auth`, `google-cloud-core`, `google-cloud-trace`, `googleapis-common-protos`, `opentelemetry-exporter-gcp-trace`, `opentelemetry-propagator-gcp` in `src/accounts/userservice/pyproject.toml` | Application code changes | Replace Cloud Trace exporter/propagator with AWS Distro for OpenTelemetry or AWS X-Ray SDK/exporter; remove Google auth/core libraries unless still required indirectly |
| userservice | Python 3.14, Flask, SQLAlchemy | `CloudTraceSpanExporter` and `CloudTraceFormatPropagator` initialized when `ENABLE_TRACING=true` in `src/accounts/userservice/userservice.py` | Application code changes | Export OpenTelemetry spans to AWS X-Ray through ADOT Collector or use X-Ray propagation |
| userservice | Kubernetes/Kustomize | Staging/production overlays include `src/components/cloud-sql`, which injects Cloud SQL proxy sidecar and localhost database connection settings | Infra-only | Replace Cloud SQL proxy with RDS/Aurora PostgreSQL endpoint, Secrets Manager/External Secrets, and standard VPC networking/security groups |
| userservice | Kubernetes/Kustomize | Production FWI overlay includes `backend-fwi` and `cloud-sql-fwi`, adding `GOOGLE_APPLICATION_CREDENTIALS`, projected `gcp-ksa` token volume, and Google ADC config | Infra-only, plus app dependency cleanup for tracing | Replace Fleet Workload Identity/Google ADC with EKS IRSA or EKS Pod Identity and IAM roles for service accounts |
| userservice | Kubernetes manifests | Root `kubernetes-manifests/userservice.yaml` uses Artifact Registry image `us-central1-docker.pkg.dev/.../userservice` and `bank-of-anthos` service account annotated for GKE Workload Identity in `kubernetes-manifests/config.yaml` | Infra-only | Push images to Amazon ECR; replace GKE Workload Identity annotation with `eks.amazonaws.com/role-arn` or EKS Pod Identity association |
| userservice | Cloud Build/Skaffold | Built and released through `src/accounts/cloudbuild.yaml` using Cloud Build builders, GCS cache URI, Artifact Registry default repo, and Cloud Deploy release | Infra-only | Replace with AWS CodeBuild/CodePipeline or GitHub Actions; use S3 for build cache, ECR for images, and Argo CD/Flux or CodeDeploy where appropriate |

## contacts

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| contacts | Python 3.14, Flask, SQLAlchemy | `google-api-core`, `google-auth`, `google-cloud-core`, `google-cloud-trace`, `googleapis-common-protos`, `opentelemetry-exporter-gcp-trace`, `opentelemetry-propagator-gcp` in `src/accounts/contacts/pyproject.toml` | Application code changes | Replace Cloud Trace exporter/propagator with ADOT/X-Ray; remove Google auth/core packages if unused |
| contacts | Python 3.14, Flask, SQLAlchemy | `CloudTraceSpanExporter` and `CloudTraceFormatPropagator` initialized when `ENABLE_TRACING=true` in `src/accounts/contacts/contacts.py` | Application code changes | Export traces to AWS X-Ray through ADOT Collector or use X-Ray SDK propagation |
| contacts | Kubernetes/Kustomize | Staging/production overlays include `src/components/cloud-sql`; production FWI overlay includes `backend-fwi` and `cloud-sql-fwi` | Infra-only, plus app dependency cleanup for tracing | Replace Cloud SQL with RDS/Aurora PostgreSQL; replace FWI/ADC with EKS IRSA or EKS Pod Identity |
| contacts | Kubernetes manifests | Root `kubernetes-manifests/contacts.yaml` uses Artifact Registry image and GKE Workload Identity service account from `kubernetes-manifests/config.yaml` | Infra-only | Use ECR image references and EKS service account IAM role annotation/Pod Identity |
| contacts | Cloud Build/Skaffold | Built and released through `src/accounts/cloudbuild.yaml` using GCS cache, Cloud Build, Artifact Registry, and Cloud Deploy | Infra-only | AWS CodeBuild/CodePipeline or GitHub Actions; S3 cache; ECR; Argo CD/Flux for Kubernetes delivery |

## accounts-db

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| accounts-db | PostgreSQL container, Kubernetes StatefulSet/Job | Staging/production overlays include `src/components/cloud-sql`; config rewrites `ACCOUNTS_DB_URI` to `postgresql://admin:admin@127.0.0.1:5432/accounts-db` for Cloud SQL proxy | Infra-only | Replace Cloud SQL proxy/localhost connection with RDS/Aurora PostgreSQL endpoint and credentials from Secrets Manager |
| accounts-db | Kubernetes/Kustomize | Init DB overlays include Cloud SQL proxy and `replace-args-job.yaml` for proxy lifecycle | Infra-only | Run init Job against RDS/Aurora private endpoint, or use migration tooling such as Flyway/Liquibase in CodeBuild/Kubernetes Job |
| accounts-db | Kubernetes/Kustomize | Production FWI/init-production-FWI overlays include `cloud-sql-fwi` projected Google token/ADC config | Infra-only | EKS IRSA/EKS Pod Identity for any AWS API access; normal database auth or IAM database authentication for RDS |
| accounts-db | Kubernetes manifests | Root `kubernetes-manifests/accounts-db.yaml` uses Artifact Registry image and GKE Workload Identity service account reference | Infra-only | ECR image; EKS service account IAM role/Pod Identity if needed |
| accounts-db | Cloud Build/Skaffold | `src/accounts/accounts-db/skaffold.yaml` participates in root Skaffold; team Cloud Build file uses GCS cache, Artifact Registry, and Cloud Deploy | Infra-only | CodeBuild/GitHub Actions, S3 cache, ECR, and Argo CD/Flux |

## balancereader

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| balancereader | Java 17, Spring Boot, Spring Data JPA | `spring-cloud-gcp-dependencies`, `libraries-bom`, `spring-cloud-gcp-starter`, `spring-cloud-gcp-starter-trace`, and `micrometer-registry-stackdriver` in `src/ledger/balancereader/pom.xml` | Application code changes | Replace Spring Cloud GCP with Spring Cloud AWS only where AWS APIs are needed; replace Stackdriver registry with Micrometer CloudWatch registry or OTEL metrics |
| balancereader | Java 17, Spring Boot | `spring.cloud.gcp.trace.enabled=${ENABLE_TRACING}` in `application.properties` | Application code changes | Configure OpenTelemetry/ADOT or AWS X-Ray tracing properties |
| balancereader | Java 17, Spring Boot | `MetadataConfig.getProjectId()`, `getZone()`, `getClusterName()` and `StackdriverMeterRegistry` in `BalanceReaderApplication.java`; Stackdriver registry injected into controller/tests | Application code changes | Use Micrometer CloudWatch registry, OTEL resource attributes, or Kubernetes Downward API/env vars for cluster metadata |
| balancereader | Kubernetes/Kustomize | Staging/production overlays include Cloud SQL proxy; production FWI overlay includes backend FWI and Cloud SQL FWI | Infra-only, plus app dependency cleanup for metrics/tracing | RDS/Aurora PostgreSQL; EKS IRSA/Pod Identity; ADOT/X-Ray/CloudWatch |
| balancereader | Kubernetes manifests | Root manifest uses Artifact Registry image and GKE Workload Identity service account | Infra-only | ECR image and EKS IAM role association |
| balancereader | Cloud Build/Skaffold | Built/released through `src/ledger/cloudbuild.yaml` with GCS cache, Cloud Build, Artifact Registry, and Cloud Deploy | Infra-only | CodeBuild/GitHub Actions, S3, ECR, Argo CD/Flux |

## ledgerwriter

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| ledgerwriter | Java 17, Spring Boot, Spring Data JPA | `spring-cloud-gcp-dependencies`, `libraries-bom`, `spring-cloud-gcp-starter`, `spring-cloud-gcp-starter-trace`, and `micrometer-registry-stackdriver` in `src/ledger/ledgerwriter/pom.xml` | Application code changes | Replace Spring Cloud GCP/Stackdriver with Spring Cloud AWS if needed, Micrometer CloudWatch, or OpenTelemetry/ADOT |
| ledgerwriter | Java 17, Spring Boot | `spring.cloud.gcp.trace.enabled=${ENABLE_TRACING}` in `application.properties` | Application code changes | Configure ADOT/X-Ray tracing |
| ledgerwriter | Java 17, Spring Boot | `MetadataConfig` and `StackdriverMeterRegistry` in `LedgerWriterApplication.java`; Stackdriver registry in controller/tests | Application code changes | CloudWatch/OTEL metrics and AWS/EKS-safe resource labels |
| ledgerwriter | Kubernetes/Kustomize | Cloud SQL proxy and FWI overlays in staging/production/production-fwi | Infra-only, plus app dependency cleanup for metrics/tracing | RDS/Aurora PostgreSQL; EKS IRSA/Pod Identity |
| ledgerwriter | Kubernetes manifests | Artifact Registry image and GKE Workload Identity service account | Infra-only | ECR image and EKS IAM role association |
| ledgerwriter | Cloud Build/Skaffold | `src/ledger/cloudbuild.yaml` Cloud Build/GCS/Cloud Deploy flow | Infra-only | CodeBuild/GitHub Actions, S3 cache, ECR, Argo CD/Flux |

## transactionhistory

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| transactionhistory | Java 17, Spring Boot, Spring Data JPA | `spring-cloud-gcp-dependencies`, `libraries-bom`, `spring-cloud-gcp-starter`, `spring-cloud-gcp-starter-trace`, and `micrometer-registry-stackdriver` in `src/ledger/transactionhistory/pom.xml` | Application code changes | Replace Spring Cloud GCP/Stackdriver with Spring Cloud AWS if needed, Micrometer CloudWatch, or OpenTelemetry/ADOT |
| transactionhistory | Java 17, Spring Boot | `spring.cloud.gcp.trace.enabled=${ENABLE_TRACING}` in `application.properties` | Application code changes | Configure ADOT/X-Ray tracing |
| transactionhistory | Java 17, Spring Boot | `MetadataConfig` and `StackdriverMeterRegistry` in `TransactionHistoryApplication.java`; Stackdriver registry in controller/tests | Application code changes | CloudWatch/OTEL metrics and AWS/EKS-safe resource labels |
| transactionhistory | Kubernetes/Kustomize | Cloud SQL proxy and FWI overlays in staging/production/production-fwi | Infra-only, plus app dependency cleanup for metrics/tracing | RDS/Aurora PostgreSQL; EKS IRSA/Pod Identity |
| transactionhistory | Kubernetes manifests | Artifact Registry image and GKE Workload Identity service account | Infra-only | ECR image and EKS IAM role association |
| transactionhistory | Cloud Build/Skaffold | `src/ledger/cloudbuild.yaml` Cloud Build/GCS/Cloud Deploy flow | Infra-only | CodeBuild/GitHub Actions, S3 cache, ECR, Argo CD/Flux |

## ledger-db

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| ledger-db | PostgreSQL container, Kubernetes StatefulSet/Job | Staging/production overlays include `src/components/cloud-sql`; config rewrites `SPRING_DATASOURCE_URL` to `jdbc:postgresql://127.0.0.1:5432/ledger-db` for Cloud SQL proxy | Infra-only | Replace Cloud SQL proxy/localhost connection with RDS/Aurora PostgreSQL endpoint and Secrets Manager credentials |
| ledger-db | Kubernetes/Kustomize | Init DB overlays include Cloud SQL proxy and `replace-args-job.yaml` | Infra-only | Run DB init/migrations against RDS/Aurora private endpoint |
| ledger-db | Kubernetes/Kustomize | Production FWI/init-production-FWI overlays include `cloud-sql-fwi` projected Google token/ADC config | Infra-only | EKS IRSA/EKS Pod Identity, or standard database auth/IAM DB auth |
| ledger-db | Kubernetes manifests | Root `kubernetes-manifests/ledger-db.yaml` uses Artifact Registry image and GKE Workload Identity service account reference | Infra-only | ECR image; EKS service account IAM role/Pod Identity if needed |

## frontend

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| frontend | Python 3.14, Flask | `google-auth`, `opentelemetry-exporter-gcp-trace`, and `opentelemetry-propagator-gcp` in `src/frontend/pyproject.toml` | Application code changes | Replace with ADOT/X-Ray exporter/propagator or OTLP exporter to ADOT Collector |
| frontend | Python 3.14, Flask | `CloudTraceSpanExporter`, `CloudTraceFormatPropagator`, and trace context propagation in `frontend.py`/`traced_thread_pool_executor.py` | Application code changes | Export to AWS X-Ray via ADOT; preserve generic OTEL context propagation where possible |
| frontend | Python 3.14, Flask | Uses metadata endpoint default `metadata.google.internal` in `frontend.py` for environment/location display | Application code changes | Use EC2/EKS metadata where appropriate, or Kubernetes Downward API/environment variables |
| frontend | Kubernetes/GKE Ingress | Production service annotations `cloud.google.com/neg` and `cloud.google.com/backend-config` | Infra-only | AWS Load Balancer Controller target groups/target-type annotations and ALB target group attributes |
| frontend | Kubernetes/GKE Ingress | Production ingress annotations `kubernetes.io/ingress.global-static-ip-name`, `networking.gke.io/managed-certificates`, and `networking.gke.io/v1beta1.FrontendConfig` | Infra-only | AWS Load Balancer Controller ALB annotations, ACM certificate ARN, Route 53 alias, and optional Global Accelerator |
| frontend | Kubernetes/GKE CRDs | `ManagedCertificate`, `FrontendConfig`, and `BackendConfig` resources | Infra-only | AWS Certificate Manager, ALB listener/SSL policy annotations, WAFv2 WebACL association, and ALB redirect actions |
| frontend | Kubernetes/Kustomize | Production FWI ingress overlay includes `frontend-fwi` projected Google token/ADC config | Infra-only, plus app dependency cleanup for tracing | EKS IRSA/EKS Pod Identity for AWS APIs |
| frontend | Kubernetes manifests | Root manifest uses Artifact Registry image and GKE Workload Identity service account | Infra-only | ECR image and EKS IAM role association |
| frontend | Cloud Build/Skaffold | `src/frontend/cloudbuild.yaml` uses Cloud Build builders, GCS cache, Artifact Registry, Cloud Deploy, and Cloud Logging-only option | Infra-only | CodeBuild/GitHub Actions, S3 cache, ECR, Argo CD/Flux, CloudWatch Logs |

## loadgenerator

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| loadgenerator | Python 3.14, Locust | Root manifest uses Artifact Registry image `us-central1-docker.pkg.dev/.../loadgenerator` | Infra-only | ECR image |
| loadgenerator | Kubernetes/Skaffold | Root Skaffold module deploys loadgenerator; no GCP SDK or tracing dependency found in service source | Infra-only | Keep Kubernetes manifests for EKS with standard image/namespace adjustments |

## ledgermonolith

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| ledgermonolith | Java 17, Spring Boot, Spring Data JPA | `spring-cloud-gcp-dependencies`, `libraries-bom`, `spring-cloud-gcp-starter`, and `spring-cloud-gcp-starter-trace` in `src/ledgermonolith/pom.xml` | Application code changes | Replace with Spring Cloud AWS only if AWS APIs are needed; use ADOT/X-Ray for tracing |
| ledgermonolith | Java 17, Spring Boot | `spring.sleuth.enabled=${ENABLE_TRACING}` in application properties; GCP trace starter present but no Stackdriver registry code found | Application code changes | Update tracing configuration to ADOT/X-Ray or neutral OpenTelemetry |
| ledgermonolith | VM deployment scripts | Scripts use `gcloud compute instances`, GCE firewall rules, Debian image project, Cloud Storage artifact buckets, and Google Cloud SDK installation | Infra-only for scripts, app config if metadata DNS remains | Replace GCE VM scripts with EC2/SSM/Launch Template/Auto Scaling or ECS/EKS; replace GCS artifacts with S3 |
| ledgermonolith | Runtime config | `src/ledgermonolith/config.yaml` references GCE internal DNS naming | Application/config change | Use AWS Cloud Map, Route 53 private hosted zone, ECS/EKS service DNS, or VPC DNS |

## Shared Kubernetes Components

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| src/components/cloud-sql | Kustomize component | Injects `gcr.io/cloud-sql-connectors/cloud-sql-proxy` sidecar, `CSQL_PROXY_INSTANCE_CONNECTION_NAME`, `cloud-sql-admin.connectionName`, localhost DB URLs, and Cloud SQL proxy job behavior | Infra-only | Remove proxy; connect directly to RDS/Aurora private endpoint, or use RDS Proxy; store credentials in Secrets Manager/External Secrets |
| src/components/cloud-sql-fwi | Kustomize component | Adds Google ADC file path, `gcp-ksa` projected service account token, `FWI_WORKLOAD_IDENTITY_POOL`, and `backend-adc` config map | Infra-only | EKS IRSA/EKS Pod Identity and native AWS SDK credential chain |
| src/components/backend-fwi | Kustomize component | Adds Google ADC file path and projected `gcp-ksa` token/ADC config to backend pods | Infra-only, app cleanup if Google auth libraries become unused | EKS IRSA/EKS Pod Identity |
| src/components/frontend-fwi | Kustomize component | Adds Google ADC file path and projected `gcp-ksa` token/ADC config to frontend pods | Infra-only, app cleanup if Google auth libraries become unused | EKS IRSA/EKS Pod Identity |

## Root Kubernetes Manifests

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| kubernetes-manifests | Plain Kubernetes YAML | `kubernetes-manifests/config.yaml` annotates service account with `iam.gke.io/gcp-service-account` | Infra-only | EKS IRSA annotation `eks.amazonaws.com/role-arn` or EKS Pod Identity association |
| kubernetes-manifests | Plain Kubernetes YAML | All released workload manifests use Artifact Registry image references under `us-central1-docker.pkg.dev/bank-of-anthos-ci/bank-of-anthos/...` | Infra-only | Amazon ECR repositories and image references |
| kubernetes-manifests | Plain Kubernetes YAML | Java/Python services enable Cloud Trace through `ENABLE_TRACING` env flags, depending on app-level GCP exporters | Application code changes | ADOT/X-Ray or disable until tracing migration is complete |

## CI/CD and Skaffold

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| src/accounts/cloudbuild.yaml | Cloud Build YAML | Cloud Build builders `gcr.io/cloud-builders/gcloud`, `gcr.io/k8s-skaffold/skaffold`, GCS cache via `gcloud storage cp`, Artifact Registry `--default-repo`, Cloud Deploy release creation, Cloud Logging-only option | Infra-only | CodeBuild/CodePipeline or GitHub Actions; S3 cache; ECR; Argo CD/Flux sync; CloudWatch Logs |
| src/ledger/cloudbuild.yaml | Cloud Build YAML | Same Cloud Build, GCS, Artifact Registry, Cloud Deploy, and Cloud Logging dependencies | Infra-only | CodeBuild/CodePipeline or GitHub Actions; S3; ECR; Argo CD/Flux; CloudWatch Logs |
| src/frontend/cloudbuild.yaml | Cloud Build YAML | Same Cloud Build, GCS, Artifact Registry, Cloud Deploy, and Cloud Logging dependencies | Infra-only | CodeBuild/CodePipeline or GitHub Actions; S3; ECR; Argo CD/Flux; CloudWatch Logs |
| skaffold.yaml / service skaffold files | Skaffold config | Neutral Skaffold config, but wired into Cloud Build/Cloud Deploy release flow | Infra-only | Keep Skaffold if useful, or replace deployment step with Helm/Kustomize applied by Argo CD/Flux |
| .github/cloudbuild/ci-pr.yaml | Cloud Build YAML | GitHub-triggered Cloud Build config | Infra-only | GitHub Actions or CodeBuild PR validation |
| .github/workflows/terraform-validate-ci.yaml | GitHub Actions | Validates GCP Terraform directories | Infra-only | Point validation at AWS Terraform modules and providers |

## Terraform: iac/tf-multienv-cicd-anthos-autopilot

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | Google providers/modules: `hashicorp/google`, `google-beta`, project services, VPC/network/NAT, IAM modules | Infra-only | AWS provider, VPC, private/public subnets, NAT Gateway, IAM roles/policies |
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | GKE Autopilot private clusters for development/staging/production | Infra-only | Amazon EKS managed node groups or Fargate profiles |
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | Cloud SQL PostgreSQL modules for staging/production | Infra-only | Amazon RDS or Aurora PostgreSQL, optional RDS Proxy |
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | GKE Hub/Fleet memberships and Fleet Workload Identity issuers | Infra-only | EKS cluster identity, OIDC provider, IRSA/EKS Pod Identity |
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | Anthos Service Mesh and ACM/Config Management fleet features | Infra-only | AWS App Mesh or Istio on EKS for service mesh; Argo CD or Flux for GitOps |
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | Artifact Registry repository and IAM | Infra-only | Amazon ECR repositories and repository policies |
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | Cloud Build triggers/service accounts and Cloud Deploy delivery pipelines/targets | Infra-only | CodeBuild/CodePipeline or GitHub Actions; Argo CD/Flux for Kubernetes deployments |
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | GCS buckets for build cache and Cloud Deploy source staging | Infra-only | S3 buckets for build cache/artifacts |
| iac/tf-multienv-cicd-anthos-autopilot | Terraform | IAM roles including Cloud Trace agent, Monitoring viewer, Cloud Build builder, Cloud Deploy roles, Cloud SQL client/instanceUser, GKE Hub roles | Infra-only | IAM roles/policies for CloudWatch/X-Ray, CodeBuild/CodePipeline, RDS IAM auth if used, EKS access entries |

## Terraform: iac/tf-anthos-gke

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| iac/tf-anthos-gke | Terraform | Terraform Google providers and Google project APIs: Compute, Anthos, Container/GKE, GKE Hub/Connect, Anthos Config Management, Mesh APIs | Infra-only | AWS provider, VPC/EKS/IAM/CloudWatch/X-Ray APIs as needed |
| iac/tf-anthos-gke | Terraform | `terraform-google-modules/kubernetes-engine/google` GKE cluster with Workload Identity namespace | Infra-only | EKS module, OIDC provider, IRSA/Pod Identity |
| iac/tf-anthos-gke | Terraform | Anthos Service Mesh module and `mesh.cloud.google.com/proxy` annotation | Infra-only | Istio on EKS or AWS App Mesh |
| iac/tf-anthos-gke | Terraform | Anthos Config Management module syncing this repo | Infra-only | Argo CD or Flux GitOps |
| iac/tf-anthos-gke | Terraform | GCloud kubectl wrapper modules tied to GKE cluster credentials and Google project | Infra-only | Kubernetes provider/Helm provider against EKS, or Argo CD/Flux bootstrap |

## Anthos Config Management: iac/acm-multienv-cicd-anthos-autopilot

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| iac/acm-multienv-cicd-anthos-autopilot | Kustomize/ACM source | ACM/Config Sync deployment source referenced by Terraform `configmanagement.git.policy_dir` | Infra-only | Argo CD Application/ApplicationSet or Flux Kustomization source |
| iac/acm-multienv-cicd-anthos-autopilot | Kustomize overlays | Workload Identity patches add `iam.gke.io/gcp-service-account` for development/staging/production | Infra-only | EKS IRSA annotations or EKS Pod Identity associations |
| iac/acm-multienv-cicd-anthos-autopilot | Kustomize overlays | `cloud-sql-admin` config maps store Cloud SQL instance connection names | Infra-only | RDS/Aurora endpoint/port/database config and Secrets Manager credentials |
| iac/acm-multienv-cicd-anthos-autopilot | Kubernetes namespace YAML | Namespace annotations enable ASM sidecar injection | Infra-only | Istio injection label/annotation on EKS or AWS App Mesh sidecar configuration |

## Optional extras/cloudsql

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/cloudsql | Shell/YAML/docs | `create_cloudsql_instance.sh`, Cloud SQL setup, Cloud SQL Admin API assumptions, `cloud-sql-admin` secrets, Cloud SQL proxy manifests, and populate jobs | Infra-only | RDS/Aurora provisioning via Terraform/CloudFormation, Secrets Manager, RDS Proxy optional, DB init jobs/migrations |
| extras/cloudsql | Shell/YAML/docs | `setup_workload_identity.sh` configures GKE Workload Identity for Cloud SQL clients | Infra-only | EKS IRSA/EKS Pod Identity with RDS IAM auth only if IAM DB auth is selected |

## Optional extras/cloudsql-multicluster

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/cloudsql-multicluster | Shell/YAML/docs | Multi Cluster Ingress/MultiClusterService CRDs under `networking.gke.io`, GKE clusters, Anthos Hub memberships, Cloud SQL shared instance | Infra-only | Multi-cluster EKS behind AWS Global Accelerator/Route 53 latency routing and ALB/NLB; RDS/Aurora Global Database if multi-region database is required |
| extras/cloudsql-multicluster | Shell scripts/docs | `gcloud container`, `gcloud container hub`, `gcloud compute addresses`, GKE URIs, Anthos/multiclusteringress APIs | Infra-only | AWS CLI/Terraform for EKS, Global Accelerator, Route 53, ACM, and ALB |

## Optional extras/postgres-hpa

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/postgres-hpa | Kubernetes/Helm/Python operator | GKE Autopilot-specific guidance and resource sizing assumptions | Infra-only | EKS managed node groups or Fargate profile sizing; Kubernetes HPA/KEDA |
| extras/postgres-hpa | Kubernetes/Helm | `gcr.io/google-containers/prometheus-to-sd` and `--stackdriver-prefix=custom.googleapis.com` | Infra-only | CloudWatch Agent/ADOT Collector/Prometheus remote write to Amazon Managed Service for Prometheus |
| extras/postgres-hpa | HPA YAML | External/custom metric names under `custom.googleapis.com` and `loadbalancing.googleapis.com` | Infra-only | CloudWatch Container Insights metrics, Prometheus Adapter metrics, or KEDA scalers |
| extras/postgres-hpa | Kubernetes YAML | Legacy app images from `gcr.io/bank-of-anthos-ci/...` | Infra-only | ECR images |

## Optional extras/prometheus

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/prometheus/gmp | Kubernetes manifests | Google Managed Service for Prometheus resources/configuration | Infra-only | Amazon Managed Service for Prometheus with ADOT Collector or Prometheus Operator |
| extras/prometheus/oss | Helm values/YAML | Mostly OSS Prometheus/Alertmanager; no required GCP API found beyond Bank of Anthos context | Infra-only | Prometheus on EKS, Amazon Managed Service for Prometheus, and Amazon Managed Grafana |

## Optional extras/tls-domain-managedcerts and extras/tls-ip-selfsigned

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/tls-domain-managedcerts | Kubernetes YAML | GKE `ManagedCertificate` CRD | Infra-only | AWS Certificate Manager certificate referenced by AWS Load Balancer Controller ingress annotation |
| extras/tls-ip-selfsigned | Kubernetes YAML/scripts | GKE `FrontendConfig`, GKE ingress annotations, and GCP load balancer/static IP assumptions | Infra-only | AWS Load Balancer Controller ALB annotations, ACM/self-managed cert secret as appropriate, Route 53/Global Accelerator |

## Optional extras/backup

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/backup | Kubernetes/YAML/docs | Backup/restore manifests and transformation rules for GKE/GCP sample environment; ingress/database manifests mirror GKE-oriented deployment | Infra-only | AWS Backup for EKS/RDS where applicable, Velero with AWS plugin/S3, and RDS snapshots |

## Optional extras/asm-multicluster and extras/istio

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/asm-multicluster | Shell/YAML/docs | Anthos Service Mesh multicluster setup, Anthos enablement, GKE clusters, ASM topology | Infra-only | Istio multicluster on EKS or AWS App Mesh |
| extras/istio | Kubernetes YAML/docs | Istio ingress used with ASM/GKE sample deployment | Infra-only | Istio ingress gateway on EKS, AWS Load Balancer Controller, or AWS App Mesh ingress pattern |

## Optional extras/apigee

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/apigee | YAML/docs | Apigee OAuth/API management config | Infra-only unless app auth flow is changed | Amazon API Gateway with Cognito/Lambda authorizers, or third-party API management |

## Optional extras/metrics-dashboard

| Service name | Language/framework | GCP-specific dependency | Fix type | Proposed AWS replacement |
|---|---|---|---|---|
| extras/metrics-dashboard | Cloud Monitoring dashboard JSON | Metric filters for `custom.googleapis.com`, `logging.googleapis.com`, `loadbalancing.googleapis.com`, and GKE resource types such as `k8s_container` | Infra-only | CloudWatch dashboards/Container Insights, Amazon Managed Grafana, AMP Prometheus metrics, ALB metrics |

## Service Dependency Map

| Service | Depends On (DB) | Calls (Services) | Called By (Services) |
|---|---|---|---|
| userservice | accounts-db | None found | frontend |
| contacts | accounts-db | None found | frontend |
| balancereader | ledger-db | None found | frontend, ledgerwriter |
| ledgerwriter | ledger-db | balancereader | frontend |
| transactionhistory | ledger-db | None found | frontend |
| frontend | None | userservice, contacts, balancereader, ledgerwriter, transactionhistory; ledgermonolith for ledger APIs when `src/ledgermonolith/config.yaml` is deployed | loadgenerator |
| loadgenerator | None | frontend | None found |
| ledgermonolith | ledger-db | None found | frontend when `src/ledgermonolith/config.yaml` reroutes `TRANSACTIONS_API_ADDR`, `BALANCES_API_ADDR`, and `HISTORY_API_ADDR` to ledgermonolith |

Safe conversion order: start with services that have no service-to-service dependencies and only require their backing database endpoint to be ready: userservice and contacts after accounts-db is available, and balancereader and transactionhistory after ledger-db is available. Ledgerwriter should wait until ledger-db and balancereader are converted because it writes to ledger-db and directly checks balances through balancereader. Frontend should wait until the services it calls are converted; in the standard microservice deployment that means userservice, contacts, balancereader, ledgerwriter, and transactionhistory, while in the monolith deployment its ledger API dependency can be satisfied by ledgermonolith instead. Loadgenerator should be last because it exercises frontend. Ledgermonolith can be migrated independently after ledger-db is available, but only becomes a frontend dependency if the monolith config is used.
