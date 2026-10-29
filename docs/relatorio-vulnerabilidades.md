# Relatório de análise de vulnerabilidades

**Data da análise:** 29/10/2026 · **Versão analisada:** `fase2` (imagem `workshop:latest`)

## 1. Escopo e método

| Alvo | Ferramenta | O que cobre |
|---|---|---|
| Dependências do projeto (Maven) | OWASP Dependency-Check 12.1.9 (base NVD e RetireJS) | 102 dependências, incluindo as embutidas em arquivos JavaScript |
| Imagem Docker da aplicação | Trivy (imagem `aquasec/trivy:latest`, scanner `vuln`) | Pacotes do sistema operacional, bibliotecas Java e binários da imagem final |

Os relatórios brutos estão em [`docs/relatorios`](relatorios): `dependency-check-report.html` e `trivy-image-report.json`.

Para reproduzir:

```bash
./mvnw org.owasp:dependency-check-maven:12.1.9:check      # relatório em target/dependency-check-report.html
docker build -t workshop:latest .
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:latest image --scanners vuln workshop:latest
```

## 2. Resumo

| Origem | Crítica | Alta | Média | Baixa | Situação |
|---|---|---|---|---|---|
| Dependency-Check | 1 | 1 | 0 | 1 | Todos no `swagger-ui` (biblioteca DOMPurify embutida); **sem correção publicada**, risco tratado abaixo |
| Dependency-Check (falso positivo) | 1 | 0 | 0 | 0 | `spring-boot-devtools`: suprimido com justificativa |
| Trivy — bibliotecas Java | 0 | 0 | 0 | 0 | Nenhuma vulnerabilidade |
| Trivy — pacotes do SO | 0 | 0 | 44 | 4 | Sem correção publicada pela distribuição |

Nenhuma vulnerabilidade de nível crítico ou alto afeta o código da aplicação ou as bibliotecas Java de execução.

## 3. Dependency-Check

### 3.1 DOMPurify embutido no Swagger UI — CVE-2026-116802 (baixa), CVE-2026-116803 (alta), CVE-2026-116804 (crítica)

- **Onde:** `swagger-ui-5.34.0.jar` (`swagger-ui-bundle.js` e `swagger-ui-es-bundle.js`), que chega pelo `springdoc-openapi-starter-webmvc-ui` 3.2.0. O bundle traz o DOMPurify **3.4.16**; as falhas foram corrigidas no **3.4.17** ([release](https://github.com/cure53/DOMPurify/releases/tag/3.4.17)).
- **Atualização possível?** Não no momento. `org.webjars:swagger-ui` 5.34.0 e `springdoc-openapi-starter-webmvc-ui` 3.2.0 são as versões mais recentes publicadas.
- **Exposição real:** o DOMPurify higieniza HTML no navegador. O Swagger UI só renderiza descrições vindas da especificação OpenAPI, que é gerada pela própria aplicação a partir do código; nenhum texto digitado por usuários entra nela. Não há caminho para um atacante injetar conteúdo nessa página. O risco prático é baixo, mas o achado é real e deve ser acompanhado.
- **Tratamento:**
  1. A interface pode ser desligada sem alterar o código: `SWAGGER_UI_ENABLED=false` (testado em `SwaggerUiToggleTest`; a especificação em `/v3/api-docs` continua disponível). **Recomendado em produção.**
  2. Atualizar o springdoc assim que sair uma versão com o DOMPurify ≥ 3.4.17 e repetir este scan.

### 3.2 `spring-boot-devtools` — CVE-2022-31691 (reportada como crítica) · falso positivo

A CVE trata das extensões *Spring Tools* para IDEs (Eclipse e VS Code) e da edição de YAML com SnakeYAML. O Dependency-Check a associou à biblioteca apenas pelo nome.
Além disso, o `spring-boot-devtools` **não faz parte do jar empacotado** (opcional e excluído pelo plugin do Spring Boot). A supressão está em
[`config/dependency-check-suppressions.xml`](../config/dependency-check-suppressions.xml), com a justificativa registrada.

## 4. Trivy — imagem final

Imagem base: `eclipse-temurin:25-jre` (Ubuntu 26.04). **Bibliotecas Java: 0 vulnerabilidades.** Pacotes do sistema operacional: 44 médias e 4 baixas, **todas sem versão corrigida** na distribuição.

| Pacote | Versão instalada | Achados |
|---|---|---|
| `rust-coreutils` | 0.10.0-1ubuntu2~26.04.1 | 20 |
| `libpcre2-8-0` | 10.46-1build1 | 8 |
| `dash` | 0.5.12-12ubuntu3 | 2 |
| `libc-bin`, `libc6`, `libc-gconv-modules-extra`, `locales` (glibc) | 2.43-2ubuntu2.4 | 2 em cada |
| `tar` | 1.35+dfsg-4ubuntu0.4 | 2 |
| `libexpat1`, `libp11-kit0`, `p11-kit`, `p11-kit-modules`, `libsystemd0`, `libudev1`, `login.defs`, `passwd` | — | 1 em cada |

**Análise:** são utilitários e bibliotecas do sistema (coreutils, expressões regulares, shell, glibc) que a aplicação não invoca e que não ficam expostos pela rede.
Explorá-los exigiria antes executar comandos dentro do container. Mesmo nesse caso, o processo roda sem privilégios.

**Ações:** reconstruir a imagem periodicamente (`docker build --pull`) para receber as correções assim que a distribuição as publicar e repetir o Trivy.

## 5. Controles de segurança já presentes no sistema

- **Autenticação e autorização:** JWT do Keycloak validado com emissor explícito; papéis `ADMIN`, `ATTENDANT`, `MECHANIC` e `CUSTOMER` aplicados por método; refresh token com rotação (o realm revoga o token a cada uso e não permite reutilização).
- **Isolamento de dados do cliente:** o cliente só acessa o que pertence ao `customer_id` do próprio token; uma OS de outro cliente responde `404`, sem revelar que ela existe. A visão do cliente não expõe os usuários internos que fizeram cada ação.
- **Validação de entrada:** CPF/CNPJ (dígitos verificadores) e placa (modelo antigo e Mercosul) no domínio; Bean Validation nos DTOs; consultas por JPA/parâmetros, sem concatenação de SQL.
- **Senhas:** nunca tratadas pela aplicação; ficam no Keycloak. A conta do cliente é criada pelo atendente e o cliente define a própria senha por e-mail.
- **Erros:** formato RFC 9457, sem *stack trace* nem detalhes internos; conflitos de concorrência viram `409`.
- **Container:** imagem multi-stage, execução com usuário sem privilégios, *healthcheck* e sem ferramentas de build na imagem final.
- **Auditoria:** histórico de status e movimentações de estoque são append-only, com o autor de cada ação.
- **Segredos:** nenhum segredo real versionado; os valores do `.env.example` e do realm de demonstração existem só para o ambiente local.

## 6. Limitações e riscos aceitos

- Os scans cobrem a aplicação e suas dependências. As imagens de terceiros do `compose.yaml` (PostgreSQL, Keycloak, Mailpit) **não** foram analisadas.
- Não foi feita análise estática do código-fonte (SAST) nem teste dinâmico (DAST/pentest).
- O ambiente de demonstração usa credenciais públicas (`admin/admin` etc.) e HTTP sem TLS. **Não deve ser exposto à internet.** Em produção: TLS, segredos fora do repositório, `start` em vez de `start-dev` no Keycloak e Swagger UI desligado.
- O Dependency-Check foi executado sem chave da API do NVD, o que apenas torna a atualização da base mais lenta.
- A base de vulnerabilidades muda diariamente; este relatório vale para a data da análise.
