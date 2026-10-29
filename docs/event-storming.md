# Event Storming

Resumo textual dos dois fluxos exigidos pelo desafio, no formato usado no quadro (Miro): **eventos de domínio** (laranja), **comandos** (azul),
**atores** (amarelo), **agregados** (amarelo-claro), **políticas** (lilás) e **sistemas externos** (rosa).
O quadro completo está no Miro: [Event Storming — Workshop](https://miro.com/app/board/uXjVH9wTsBg=/?share_link_id=625997029412).

## 1. Criação e acompanhamento da OS

```mermaid
flowchart LR
    A1([Atendente]) --> C1[Cadastrar cliente] --> E1{{Cliente cadastrado}}
    E1 --> P1[/Política: criar conta de acesso/] --> S1[(Keycloak)] --> E1b{{Conta criada e e-mail de senha enviado}}
    A1 --> C2[Cadastrar veículo] --> E2{{Veículo cadastrado}}
    A1 --> C3[Abrir OS<br/>CPF/CNPJ + placa] --> AG1((OS)) --> E3{{OS aberta<br/>status: Recebida}}
    M1([Mecânico]) --> C4[Iniciar diagnóstico] --> E4{{Diagnóstico iniciado}}
    M1 --> C5[Incluir serviços e peças] --> E5{{Item incluído}}
    M1 --> C6[Concluir diagnóstico] --> E6{{Orçamento emitido<br/>status: Aguardando aprovação}}
    E6 --> P2[/Política: avisar o cliente/] --> E6b{{E-mail do orçamento enviado}}
    K1([Cliente]) --> C7[Aprovar orçamento] --> E7{{Orçamento aprovado}}
    K1 --> C8[Recusar orçamento] --> E8{{Orçamento recusado<br/>volta a Em diagnóstico}}
    E7 --> P3[/Política: reservar peças/] --> E9{{Peças reservadas<br/>status: Em execução}}
    E7 --> P3 --> E10{{Falta de peças<br/>status: Aguardando peças}}
    M1 --> C9[Propor reparo adicional] --> E11{{Orçamento adicional emitido}}
    M1 --> C10[Finalizar execução] --> E12{{OS finalizada}}
    E12 --> P4[/Política: baixar estoque e avisar o cliente/] --> E12b{{Estoque consumido / cliente avisado}}
    A1 --> C11[Entregar veículo] --> E13{{OS entregue}}
    A1 --> C12[Cancelar OS] --> E14{{OS cancelada}}
    K1 --> Q1[Consultar OS pela API]
```

Pontos quentes discutidos:
- Quem decide o orçamento: o cliente pelo app, ou o atendente em nome dele (telefone/balcão), sempre registrando o canal e o autor.
- O que acontece quando o cliente recusa: a OS volta ao diagnóstico com os itens restaurados; um novo orçamento é emitido encadeado ao anterior.
- Reparo adicional: pausa a execução até a decisão, sem cancelar o que já foi aprovado.

## 2. Gestão de peças e insumos

```mermaid
flowchart LR
    A1([Atendente / Administrador]) --> C1[Cadastrar peça] --> AG1((Peça)) --> E1{{Peça cadastrada}}
    A1 --> C2[Registrar entrada de estoque] --> E2{{Estoque recebido}}
    E2 --> P1[/Política: atender reservas em espera, da mais antiga/] --> E3{{Peças reservadas}}
    A1 --> C3[Ajustar estoque<br/>com motivo] --> E4{{Estoque ajustado}}
    E4 --> P1
    E5{{Orçamento aprovado}} --> P2[/Política: reservar tudo ou nada/] --> E6{{Peças reservadas}}
    P2 --> E7{{Falta de peças}}
    E8{{OS finalizada}} --> P3[/Política: consumir reserva/] --> E9{{Estoque consumido}}
    E10{{OS cancelada}} --> P4[/Política: liberar reserva/] --> E11{{Reserva liberada}} --> P1
    E4 --> P5[/Política: disponível abaixo do mínimo/] --> E12{{Estoque baixo detectado}} --> E13{{E-mail ao administrador}}
    A1 --> C4[Desativar peça] --> E14{{Peça desativada}}
```
