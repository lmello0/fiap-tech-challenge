# ADR 0015 — Segredos e escala horizontal

**Status:** aceita (Fase 2)

## Segredos
- Nenhum segredo real é versionado. O Terraform gera as senhas (`random_password`) e cria os Secrets do Kubernetes: banco da aplicação, banco do Keycloak, administrador do Keycloak, segredo do client `workshop-admin`, segredo do webhook e o realm.
- O realm do Keycloak usa o placeholder `${KEYCLOAK_ADMIN_CLIENT_SECRET}` no lugar do segredo do client; o Keycloak o resolve na importação. Como o realm também contém os usuários de demonstração, ele é montado a partir de um Secret.
- ConfigMaps só têm valores não sensíveis. O ConfigMap é gerado pelo Kustomize com sufixo de hash, de modo que qualquer mudança de configuração reinicia os pods.

## Escala
- HPA com 2 a 6 réplicas, CPU alvo de 70%, sem métrica de memória (o heap da JVM não é devolvido ao sistema e mantinha as réplicas altas depois da carga). Os *requests* de CPU são baixos (250m) porque a utilização é medida sobre o request.
- Medido no minikube com 4 CPUs: de 2 para 6 réplicas em cerca de 1,5 minuto sob carga (103 mil requisições, nenhuma falha, p95 de 142 ms), e de volta para 2 após a carga.
- **Conexões do banco são o limite real:** o Postgres aceita 100. Com 6 réplicas e o pool padrão do Hikari (10), mais as do Keycloak, o total chegava a 98. Os pools foram limitados (aplicação em 8, Keycloak em 20), totalizando no pior caso 68.
- Várias réplicas são seguras porque os consumidores de eventos são idempotentes ([ADR 0011](0011-idempotencia-dos-consumidores.md)) e as atualizações disparadas por eventos usam lock pessimista ([ADR 0010](0010-estrategia-de-locks.md)). Ao reiniciar, cada réplica reenvia publicações pendentes, o que pode repetir uma entrega, tolerada pela idempotência.
