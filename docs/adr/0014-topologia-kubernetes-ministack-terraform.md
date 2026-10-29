# ADR 0014 — Topologia: minikube, MiniStack e Terraform

**Status:** aceita (Fase 2)

## Decisão
- **Cluster:** minikube (driver `docker`), criado pelo Terraform (provider `scott-the-programmer/minikube`) com `metrics-server`, necessário para o HPA.
- **Banco:** PostgreSQL 18 como **RDS no MiniStack** (emulador de AWS), criado pelo Terraform. O MiniStack sobe um container Postgres real; o endpoint público é habilitado (`MINISTACK_RDS_PUBLIC_ENDPOINT`) e o cluster o alcança por `host.minikube.internal:<porta>`.
- **Usuários e databases** (`workshop_app`/`workshop` e `keycloak`/`keycloak`) são criados dentro da instância pelo provider `postgresql`, mantendo um único usuário de banco para a aplicação.
- **Credenciais:** geradas pelo Terraform e guardadas no Secrets Manager do MiniStack; a raiz seguinte as lê para criar os Secrets do Kubernetes.
- **Três raízes aplicadas em ordem** (`01-foundation`, `02-database`, `03-platform`), porque os providers da raiz seguinte dependem de recursos da anterior. O estado é local, pois o ambiente é efêmero.
- **Manifestos:** Kustomize (`k8s/base` e `k8s/overlays/minikube`), YAML puro.
- **Acesso:** `kubectl port-forward` em `localhost:8080/8081/8025`, o que mantém o emissor do JWT igual ao do compose.

## Consequências
- Um erro de configuração real foi encontrado e corrigido na validação: publicar a faixa de portas do RDS no container do próprio MiniStack fazia a porta do host apontar para ele em vez de para o Postgres.
- O MiniStack guarda o estado em memória; destruir o container perde as instâncias (recriadas pelo Terraform).
- Em produção real, troca-se o endpoint do provider `aws` e o cluster; o restante do desenho permanece.
