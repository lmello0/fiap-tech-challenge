# Linguagem ubíqua

Termos usados no negócio, na documentação e no código (em inglês, entre parênteses).

## Cadastro (`customer`)

| Termo | Significado |
|---|---|
| **Cliente** (*Customer*) | Pessoa física ou jurídica identificada por CPF/CNPJ e e-mail únicos. Tem login próprio para acompanhar suas OS. |
| **Veículo** (*Vehicle*) | Pertence a um cliente e é identificado pela placa (modelo antigo ou Mercosul). Tem marca, modelo e ano. |
| **Desativação** | Exclusão lógica: o cadastro some das listagens e não entra em OS nova, mas o histórico permanece. Desativar o cliente desativa seus veículos e a conta de acesso. |

## Catálogo e estoque (`catalog`, `inventory`)

| Termo | Significado |
|---|---|
| **Serviço** (*RepairService*) | Trabalho vendido pela oficina (ex.: troca de óleo), com preço e tempo estimado. |
| **Peça** (*Part*) | Item físico em estoque, com SKU, preço, quantidade mínima e saldo. Insumos são tratados como peças. |
| **Movimentação** (*Stock Movement*) | Único registro que altera o saldo: **entrada** (compra), **ajuste** (contagem, quebra, perda; sempre com motivo) e **consumo** (OS finalizada). |
| **Reserva** (*Reservation*) | Quantidade de peças separada para uma OS entre a aprovação do orçamento e o fim do serviço. Reduz o disponível sem tirar do saldo. |
| **Disponível** (*Available*) | Saldo em estoque menos o reservado. |
| **Falta** (*Shortage*) | Reserva que não pôde ser atendida; a OS aguarda e a reserva é atendida sozinha quando o estoque aumenta. |
| **Estoque baixo** (*Low stock*) | Disponível menor ou igual à quantidade mínima da peça; gera alerta ao administrador. |

## Ordem de serviço (`workorder`)

| Termo | Significado |
|---|---|
| **Ordem de Serviço — OS** (*Work Order*) | Registro de um serviço em um veículo, da recepção à entrega. Identificada por um número legível (`OS-2026-000123`). Não confundir com pedido de compra. |
| **Diagnóstico** (*Diagnosis*) | Inspeção do mecânico, que define os itens do primeiro orçamento. |
| **Item** | Serviço ou peça, com quantidade e *snapshot* de nome e preço, ainda editável em Recebida e Em diagnóstico. |
| **Orçamento** (*Budget*) | Proposta de serviços e peças com total calculado, enviada ao cliente. Imutável depois de emitida. |
| **Orçamento adicional** (*Additional budget*) | Reparo extra encontrado durante a execução; contém só os itens novos. |
| **Aprovação / Recusa** | Decisão sobre um orçamento, pelo cliente (canal `APP`) ou pelo atendente em nome dele (`PHONE` ou `IN_PERSON`). |
| **Histórico de status** | Linha do tempo da OS: de qual para qual status, quando e quem fez (usuário, papel ou o próprio sistema). |
| **Tempo médio de execução** | Média do tempo líquido em *Em execução* das OS finalizadas no período, sem contar as pausas de aprovação adicional ou falta de peça. |

### Status da OS

| Português | Código | Quando |
|---|---|---|
| Recebida | `RECEIVED` | OS aberta no balcão |
| Em diagnóstico | `IN_DIAGNOSIS` | Mecânico avaliando o veículo |
| Aguardando aprovação | `WAITING_APPROVAL` | Orçamento enviado, aguardando decisão (ou aguardando o resultado da reserva de peças) |
| Aguardando peças | `WAITING_PARTS` | Orçamento aprovado, mas falta estoque |
| Em execução | `IN_EXECUTION` | Serviço aprovado em andamento |
| Finalizada | `COMPLETED` | Serviço pronto, veículo aguardando retirada |
| Entregue | `DELIVERED` | Veículo retirado pelo cliente |
| Cancelada | `CANCELLED` | Encerrada antes de finalizar |

## Perfis

| Perfil | Papel |
|---|---|
| **Administrador** (`ADMIN`) | Acesso total |
| **Atendente** (`ATTENDANT`) | Balcão: cadastros, abertura, entrega, cancelamento e decisão em nome do cliente |
| **Mecânico** (`MECHANIC`) | Diagnóstico, itens, reparo adicional e execução |
| **Cliente** (`CUSTOMER`) | Acompanha e decide sobre as próprias OS |
