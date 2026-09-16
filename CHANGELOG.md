# Changelog — Prime CRM

Entregas do projeto, organizadas por fase (roadmap completo no [README.md](README.md)).

## [Fase 7] — Qualidade e hardening (observabilidade)

Terceira entrega da Fase 7. Falta apenas performance/CI-CD (quality gates) para fechar a fase.

- **Actuator + Micrometer**: `spring-boot-starter-actuator` e `micrometer-registry-prometheus`
  adicionados ao modulo `api`. `/actuator/health`, `/actuator/health/liveness`,
  `/actuator/health/readiness` e `/actuator/info` continuam publicos (necessarios para health check
  de orquestracao); `/actuator/metrics` e `/actuator/prometheus` ficam atras do JWT como qualquer
  outro endpoint da API (nunca estiveram na lista de rotas publicas). Health groups de liveness/
  readiness habilitados (`management.endpoint.health.probes.enabled`), prontos para uso futuro em
  Kubernetes.
- **Logging estruturado com correlacao de requisicao**: novo `RequestCorrelationFilter` (primeiro
  filtro da cadeia, antes do rate limiting) gera ou reaproveita um `X-Request-Id` por requisicao,
  populando `requestId`/`tenantId` no MDC do SLF4J e devolvendo o mesmo header na resposta — permite
  rastrear uma requisicao especifica atraves de toda a stack de logs. `JwtAuthenticationFilter` some
  o `userId` ao MDC assim que a autenticacao e resolvida. Novo `logback-spring.xml` inclui os tres
  campos (`requestId`, `tenantId`, `userId`) no padrao de log do console; os niveis por pacote
  continuam 100% controlados pelas variaveis `APP_LOG_LEVEL`/`SQL_LOG_LEVEL` ja existentes (nenhuma
  mudanca de comportamento ali).
- **Log de eventos de seguranca antes silenciosos**: `GlobalExceptionHandler` agora grava `WARN` para
  `BadCredentialsException` (401) e `AccessDeniedException` (403) — antes esses casos geravam a
  resposta HTTP correta mas nao deixavam rastro nenhum no log.
- Corrigido de passagem: `.env.example` nao tinha as variaveis `RATE_LIMIT_*` introduzidas na entrega
  anterior de hardening — adicionadas agora.

## [Fase 7] — Qualidade e hardening (cobertura de testes)

Segunda entrega da Fase 7: cobertura de testes, priorizada pelo dono do produto apos o hardening de
seguranca. Continuam pendentes observabilidade e performance/CI-CD (quality gates).

- **Backend**: os dois unicos services sem teste unitario (`GlobalSearchService`,
  `NotificationService`) ganharam suites completas (`GlobalSearchServiceTest`,
  `NotificationServiceTest`), fechando a cobertura de `core/service/*` em 100% (agora 37 classes de
  service, todas testadas). `./mvnw verify` passa de 227+37 para 235+37 testes.
- **Frontend — regressao do bug de "undefined"**: `format.util.ts` (usado por praticamente toda
  formatacao de valor/data da UI) nunca tinha teste proprio, apesar de ser a peca central do fix do
  bug anterior — ganhou `format.util.spec.ts` (25 casos, cada funcao testada com `null` **e**
  `undefined` separadamente, refletindo o comportamento real da API descrito acima).
  `opportunity-board.component.ts` e `opportunity-detail-drawer.component.ts` (os dois componentes
  que tiveram o bug do "undefined%" na probabilidade/dias-na-etapa) nunca tinham spec proprio —
  ganharam specs novos com casos explicitos para probabilidade omitida pela API (nao so `null`), a
  forma exata como o bug se manifestava em produção. `dashboard.component.spec.ts` e
  `finance-dashboard.component.spec.ts` ganharam um caso de regressao dedicado, simulando a API
  omitindo os campos de tendencia (e nao apenas retornando `null`), para travar o fix anterior contra
  reintroducao futura.

## [Fase 7] — Qualidade e hardening (hardening de seguranca)

Primeira entrega da Fase 7, com o escopo de hardening de seguranca priorizado pelo dono do produto.
As demais frentes da fase (cobertura de testes, observabilidade, performance e quality gates de
CI/CD) ficam para entregas seguintes.

### Bloqueio por tentativas de login (brute-force lockout)

- `users` ganhou as colunas `failed_login_attempts` e `locked_until` (migration `V43`). Apos 5
  tentativas de senha invalida seguidas para a mesma conta, ela fica bloqueada por 15 minutos
  (`LoginLockoutService`), independente do IP de origem — protege contra ataques distribuidos por
  varios IPs contra uma unica conta.
- O contador de tentativas e o bloqueio sao persistidos numa transacao propria
  (`REQUIRES_NEW`), pelo mesmo motivo que o `audit_log` ja funciona assim: o `login()` sempre lanca
  excecao e faz rollback no caminho de falha, entao a contagem precisa sobreviver a esse rollback.
- Login com conta bloqueada retorna `401` com o codigo `ACCOUNT_LOCKED` sem sequer verificar a senha
  informada. Um login bem-sucedido zera o contador e remove o bloqueio. O evento de bloqueio e
  auditado com uma nova acao `LOGIN_LOCKED` (`AuditAction`, migration `V44` para o `CHECK` do
  `audit_log`).

### Rate limiting

- Novo filtro `RateLimitFilter` (antes do `JwtAuthenticationFilter` na cadeia do Spring Security)
  aplica limite de requisicoes por IP com janela fixa em memoria (`InMemoryRateLimiter`, sem
  dependencia nova): `POST /api/v1/auth/login` a 10 requisicoes/minuto (mitiga credential stuffing) e
  as demais rotas da API a 300 requisicoes/minuto. `/actuator/**` fica isento para nao quebrar health
  checks de orquestracao. Limites configuraveis via `app.rate-limit.*` /
  `RATE_LIMIT_*` (env vars), com a opcao de desligar inteiramente (`RATE_LIMIT_ENABLED=false`).
  Excesso responde `429` no mesmo formato de erro (`ApiErrorResponse`) usado pelo resto da API.

### Varredura de dependencias vulneraveis no CI

- `.github/dependabot.yml` novo, cobrindo os tres ecossistemas do repositorio (Maven em `backend/`,
  npm em `frontend/` e as proprias GitHub Actions), com checagem semanal e abertura automatica de PR
  quando ha atualizacao de seguranca disponivel.
- CI do frontend ganhou um passo `npm audit --audit-level=high` (nao bloqueia o pipeline —
  `continue-on-error`, tratado como sinal de alerta e nao gate — mas fica visivel no log de toda PR).
- Avaliado e deliberadamente deixado de fora desta entrega: OWASP Dependency-Check para o Maven do
  backend. Sem uma chave de API do NVD (que este ambiente nao possui), o plugin fica sujeito a
  rate-limit da base de vulnerabilidades e tende a falhar de forma intermitente e sem relacao com o
  codigo, gerando ruido em vez de sinal. Fica registrado como pendencia caso o dono do produto queira
  prover uma chave `NVD_API_KEY` futuramente.

## [Fix] — "undefined" em campos de tendencia/probabilidade sem dado historico

Causa raiz: `application.yml` define `jackson.default-property-inclusion: non_null`, entao a API
**omite** campos nulos da resposta JSON em vez de envia-los como `"campo": null`. Varios pontos do
frontend, porem, tratavam esses campos com verificacao estrita `=== null` / `!== null`, que nao cobre
`undefined` (o valor que uma propriedade ausente assume em JavaScript/TypeScript). Resultado: quando
nao havia dado historico para calcular uma tendencia (ex.: primeiro periodo de uso, sem mes anterior
para comparar) ou quando um campo numerico opcional nao estava preenchido, a tela renderizava a string
literal `undefined` em vez do rotulo/traço de "sem dado" esperado.

Reproduzido e corrigido com testes via Playwright (mock da API na camada HTTP, simulando exatamente o
comportamento do Jackson de omitir campos nulos) nos seguintes pontos:

- **Dashboard principal** (`dashboard.component.ts`): badges de tendencia de receita ganha, novos
  leads e novos clientes exibiam `undefined%` quando o backend nao retornava `wonAmountTrend`,
  `newLeadsTrend` ou `newCustomersTrend` por falta de periodo anterior para comparacao.
- **Dashboard financeiro** (`finance-dashboard.component.ts`): mesmo problema nos badges de tendencia
  de recebimentos e pagamentos (`movementTrend`).
- **Oportunidades** (lista, kanban e painel de detalhe): coluna/campo de probabilidade exibia
  `undefined%` quando a oportunidade nao tinha probabilidade definida; o historico de movimentacao de
  etapa tinha o mesmo problema em "dias na etapa anterior".
- **`opportunity-board.util.ts`**: o mesmo padrao de comparacao estrita fazia a deteccao automatica de
  motivo de ganho (`defaultProbability`) falhar silenciosamente quando a etapa nao tinha probabilidade
  padrao configurada — corrigido junto por ser a mesma causa raiz.

Todos os pontos corrigidos passaram a usar comparacao frouxa (`== null` / `!= null`) ou normalizacao
explicita (`?? null`) no limite onde o dado chega da API, tratando `undefined` e `null` de forma
identica — que e a intencao original de todo o codigo ja revisado (todos os helpers de formatacao
existentes, como `formatCurrencyBRL`/`formatIsoDate`, ja faziam essa checagem dupla corretamente).

Investigacao adicional (dashboards vazios, linhas de listagem com campos nulos, formularios de edicao
abertos com dados nulos) nao encontrou outras ocorrencias do mesmo bug — os demais componentes usam
`??`, `?.` ou os helpers de `format.util.ts`, que ja cobrem `null` e `undefined`.

## [Fix] — Varredura de seguranca, correcoes e limpeza pos-Fase 6

Varredura completa do projeto (backend e frontend) em busca de falhas de seguranca, bugs, problemas
futuros e violacoes das convencoes do projeto (comentarios em codigo), apos a conclusao da Fase 6.

### Seguranca

- **Critico**: `JwtTokenProvider` agora falha na inicializacao (`IllegalStateException`) se o perfil
  Spring `prod` estiver ativo e `JWT_SECRET` ainda for o valor padrao de desenvolvimento (o mesmo
  hardcoded em `application.yml`, `.env.example` e `docker-compose.yml`). Sem essa checagem, um deploy
  em producao que esquecesse de definir `JWT_SECRET` assinaria tokens com um segredo publico neste
  repositorio, permitindo forjar tokens de qualquer usuario. Perfis `dev`/`test` continuam funcionando
  sem exigir um secret customizado.
- Tamanho maximo de pagina (`spring.data.web.pageable.max-page-size: 200`) para todas as listagens
  paginadas da API, evitando que um cliente solicite `?size=999999999` e force uma consulta sem limite
  pratico ao banco. 200 foi escolhido por ser exatamente o maior tamanho de pagina ja usado pelo
  frontend (listas de dominio/usuarios para popular `<p-select>`), entao nenhuma tela existente e
  afetada.
- `npm audit`: dependencias do frontend atualizadas (Angular 20.3.27 → 20.3.31 e patches transitivos de
  build) para eliminar as 13 vulnerabilidades reportadas (2 altas, 11 moderadas — incluindo um bypass de
  sanitizacao no `@angular/compiler` e falhas de SSRF/DoS em dependencias transitivas de build). `npm
  audit` e `npm audit --production` agora reportam zero vulnerabilidades.

### Correcoes

- `GlobalExceptionHandler`: excecoes inesperadas (handler generico `Exception.class`, HTTP 500) agora
  sao registradas via `Logger.error(...)` antes de responder ao cliente. Antes, um erro 500 nao
  mapeado era engolido silenciosamente, sem nenhum rastro no log do servidor, dificultando diagnostico
  em producao.
- Efeito colateral da atualizacao do Angular: a checagem de tipos de template ficou mais estrita e
  passou a rejeitar as funcoes `statusSeverity`/`actionSeverity` de 8 telas (Contratos, Pedidos,
  Propostas, Contas a Pagar, Contas a Receber, Auditoria, Tarefas, Agenda) que declaravam retorno
  `string` generico em vez da uniao literal exata que o `p-tag` do PrimeNG espera. Corrigido o tipo de
  retorno de cada funcao para bater com o `Record<Status, ...>` que ja alimentava a logica — sem
  mudanca de comportamento, apenas tipagem mais precisa. Corrigido tambem `CustomersPageComponent`,
  cujo `onTabChange` nao aceitava mais o `undefined` que o evento `valueChange` do `p-tabs` passou a
  emitir na nova versao.

### Limpeza (zero comentarios em codigo)

Removido um comentario de duas linhas em `loading.store.spec.ts` (unico comentario explicativo
remanescente no projeto, alem de falsos positivos como URLs e mascaras de CPF/CNPJ) que violava a
convencao do projeto de nao ter comentarios em codigo — a decisao que ele documentava (nao ler o
signal reativamente dentro de `start()`/`stop()` para evitar um loop de efeito) ja fica evidente pelo
nome do proprio teste de regressao.

### O que foi avaliado e mantido como esta (fora do escopo deste Fix)

- **Ausencia de bloqueio por tentativas de login (brute-force lockout)**: o login ja audita tentativas
  falhas (`AuditAction.LOGIN_FAILED`) mas nao bloqueia temporariamente a conta apos N tentativas. Um
  bloqueio de conta e uma feature nova (migration, campos na entidade `User`, logica de janela de
  tempo), nao uma correcao pontual — decidido deixar de fora, dado que o usuario optou por nao entrar
  na Fase 7 (Qualidade e hardening) neste momento.
- **Token JWT do WebSocket na query string** (`?access_token=...`): tradeoff ja documentado desde a
  Fase 3 (a API `WebSocket` do navegador nao permite enviar headers customizados no handshake). O
  `JwtHandshakeInterceptor` valida a assinatura do token e rejeita handshakes invalidos com 401 — nao e
  uma falha de autenticacao, apenas um vetor de exposicao (logs de acesso) inerente a essa abordagem.
- **`apiBaseUrl` fixo em `http://localhost:8080/api/v1`** no build de producao do frontend: bate com a
  topologia de deploy documentada (`docker-compose.yml` publica back e front na mesma maquina/host),
  entao nao e um bug neste escopo — so se tornaria um problema se o projeto passasse a ser implantado
  atras de dominios/servidores separados, o que nao esta em escopo agora.

### Qualidade

- Backend: novo `JwtTokenProviderTest` (falha com secret padrao em `prod`, sucesso com secret proprio
  em `prod`, sucesso com secret padrao em `dev`, emissao/leitura de token). Suite completa (`shared` +
  `infra` + `core` + `api`) segue verde: 220 testes em `core` e 37 em `api` (29 pulados por exigirem
  Postgres local, comportamento ja existente).
- Frontend: `npm run build` e suite completa (305 testes) verdes apos a atualizacao de dependencias e
  as correcoes de tipagem.

## [Fase 6] — Dashboards Comercial e Produtividade

Terceira e ultima entrega da Fase 6 (Dashboards por modulo e metas comerciais): dashboards de
Comercial (propostas, pedidos, contratos) e Produtividade (tarefas, agenda), fechando a fase.

### Banco de dados

Nenhuma migration nova. Foram adicionadas consultas de agregacao aos repositorios ja existentes:
`ProposalRepository` e `OrderRepository` ganharam `summarize*Between` (total e por status, dentro de um
periodo) e `summarize*ByMonth`; `ContractRepository` ganhou `summarizeByStatus` e
`summarizeExpiringBetween`; `TaskRepository` ganhou `rankAssigneesByCompleted` (ranking de conclusao
por responsavel); `CalendarEventRepository` ganhou contagens por status/periodo e `countOverdue`.
Nova projecao `LabeledCountAggregate` (label + contagem, sem valor monetario), companheira das
projecoes `AmountAggregate`/`LabeledAmountAggregate` ja existentes, usada onde o valor agregado e uma
contagem simples (conclusao de tarefas) em vez de uma soma financeira.

### Backend

- `GET /api/v1/dashboard/comercial`: propostas e pedidos emitidos no periodo com taxa de conversao
  (aceita/entregue sobre o total), contratos ativos (valor recorrente total, MRR) e contratos vencendo
  nos proximos 30 dias, e serie mensal dos ultimos 12 meses de propostas x pedidos. Permissao
  `hasAnyAuthority('PROPOSTAS_VIEW', 'PEDIDOS_VIEW', 'CONTRATOS_VIEW')`.
- `GET /api/v1/dashboard/produtividade`: resumo de tarefas (pendentes, em andamento, em atraso,
  vencendo hoje, concluidas nos ultimos 7 dias — o mesmo calculo ja usado no Dashboard geral), ranking
  dos 5 responsaveis que mais concluiram tarefas no periodo e resumo da agenda (eventos hoje, na
  semana e em atraso). Permissao `hasAnyAuthority('TAREFAS_VIEW', 'AGENDA_VIEW')`.

**Decisao de modelagem — sem card de metrica com tendencia no Comercial**: diferente do Dashboard
geral e do Financeiro, o dashboard Comercial nao compara o periodo atual contra o anterior. Conversao
proposta→pedido→contrato so faz sentido como uma taxa absoluta do periodo (aceitas sobre emitidas),
comparar essa taxa contra o periodo anterior adicionaria uma metrica de interpretacao duvidosa sem
pedido explicito — mantido fora do escopo.

### Frontend

Duas telas novas: `/comercial/dashboard` (cartoes de propostas/pedidos/contratos e grafico de area de
propostas x pedidos, novo primeiro item do submenu "Comercial" na sidebar) e `/produtividade/dashboard`
(resumo de tarefas, ranking de conclusao e resumo da agenda, novo item de primeiro nivel na sidebar,
ao lado de Tarefas e Agenda). Ambas reaproveitam os mesmos componentes/padroes visuais ja usados nos
dashboards geral e Financeiro (cartoes de metrica, `AreaChartComponent`, ranking com barra de progresso).

### Qualidade

- Backend: `CommercialDashboardServiceTest` (serie mensal com meses zerados, taxa de conversao de
  propostas e pedidos, taxas zeradas sem dados, contratos ativos/vencendo) e
  `ProductivityDashboardServiceTest` (contagem de atraso sobre status abertos, ranking com percentuais
  somando o total, ranking vazio sem dados, resumo de agenda hoje/semana/atraso). Suite completa do
  `core` seguiu verde (217 testes).
- Frontend: `commercial-dashboard.component.spec.ts` e `productivity-dashboard.component.spec.ts`
  cobrindo carregamento por periodo, cartoes de indicadores, serie mensal/ranking e estado de erro.
  Suite completa (305 testes) e `npm run build` verdes.

## [Fase 6] — Dashboard Financeiro

Segunda entrega da Fase 6 (Dashboards por modulo e metas comerciais): primeiro dashboard especifico
de modulo, consolidando indicadores de Contas a Receber e Contas a Pagar.

### Banco de dados

Nenhuma migration nova. Foram adicionadas consultas de agregacao (`summarizeOpen`, `summarizeOverdue`,
`summarizePaidBetween`, `summarizePaidByMonth`) aos repositorios `ReceivableRepository` e
`PayableRepository` ja existentes, no mesmo padrao das agregacoes ja usadas pelo Dashboard geral em
`OpportunityRepository`.

**Decisao de modelagem — "recebido/pago no periodo" a partir de `paidAt`**: como o modelo atual nao
mantem um historico de pagamentos (cada conta guarda apenas `paid_amount` acumulado e `paid_at` da
ultima baixa), o indicador de movimentacao do periodo soma `paid_amount` das contas cuja ultima baixa
caiu dentro da janela consultada. E uma simplificacao deliberada, coerente com a que ja existe em
Contas a Receber/Pagar desde a Fase 5 (sem uma tabela de parcelas de pagamento), e funciona bem no caso
comum de uma conta ser paga uma unica vez.

### Backend

- `GET /api/v1/dashboard/financeiro`: indicadores do periodo (padrao ultimos 30 dias) de Contas a
  Receber e a Pagar — em aberto, em atraso e movimentado no periodo com variacao contra o periodo
  anterior — e serie mensal dos ultimos 12 meses de recebido x pago x saldo liquido. Endpoint separado
  do Dashboard geral (`/api/v1/dashboard`), com sua propria permissao (`FINANCEIRO_VIEW`) em vez de
  `isAuthenticated()`.

### Frontend

Tela `/financeiro/dashboard`, com cartoes de indicadores (a receber/pagar em aberto e em atraso,
recebido/pago no periodo com variacao) e grafico de area do recebido x pago dos ultimos 12 meses,
reaproveitando o `AreaChartComponent` ja usado no Dashboard geral. Novo primeiro item do submenu
"Financeiro" na sidebar.

### Qualidade

- Backend: `FinanceDashboardServiceTest` cobrindo serie mensal com meses zerados, calculo de saldo
  liquido por mes, indicadores de aberto/atraso de contas a receber e a pagar, variacao sem periodo
  anterior e variacao calculada contra o periodo anterior. Suite completa do `core` seguiu verde
  (208 testes).
- Frontend: `finance-dashboard.component.spec.ts` cobrindo carregamento por periodo, cartoes de
  indicadores, serie mensal e estado de erro. Suite completa (290 testes) e `npm run build` verdes.

## [Fase 6] — Metas comerciais

Primeira entrega da Fase 6 (Dashboards por modulo e metas comerciais): cadastro de metas de vendas
por vendedor e mes, com acompanhamento automatico do valor realizado e do percentual de atingimento.

### Banco de dados

Migrations `V41` e `V42`: tabela `sales_goals` (vendedor, mes de referencia normalizado para o
primeiro dia do mes, valor da meta, observacoes) e as 4 permissoes novas (`METAS_*`) concedidas ao
perfil Administrador.

**Decisoes de modelagem**: `reference_month` e sempre normalizado para o primeiro dia do mes
(`date_trunc('month', ...)`, com CHECK garantindo isso no banco), permitindo uma unique index simples
por (vendedor, mes). O valor realizado e o percentual de atingimento **nao sao armazenados** — sao
calculados a cada consulta somando o `amount` das oportunidades com `outcome = WON` do vendedor
fechadas dentro do mes da meta (reaproveita o metodo de agregacao ja usado no ranking do Dashboard,
apenas com um filtro adicional por vendedor), no mesmo espirito dos campos computados (`overdue`,
`expired`) ja usados em Tarefas, Agenda, Propostas, Contratos e Financeiro — evita que a meta fique
com um valor realizado desatualizado se uma oportunidade for reaberta ou reatribuida depois.

### Backend

- CRUD completo (`/api/v1/sales-goals`) com busca textual (observacoes), filtros (vendedor, mes de
  referencia), paginacao, RBAC e auditoria.
- Validacao de duplicidade: nao e possivel cadastrar duas metas para o mesmo vendedor no mesmo mes
  (`ConflictException`, mesmo padrao ja usado para documento de cliente, e-mail e login de usuario).

### Frontend

Tela `/metas-comerciais`, novo item de primeiro nivel na sidebar (com atalho no "+ Novo" do topbar),
com listagem, diálogo de CRUD (vendedor, mes via seletor `p-datepicker` em modo `month`, valor da
meta, observacoes) e barra de progresso do atingimento (vermelho abaixo de 50%, amarelo entre 50% e
100%, verde a partir de 100%).

### Qualidade

- Backend: `SalesGoalServiceTest` cobrindo normalizacao do mes de referencia, conflito de meta
  duplicada, calculo de atingimento a partir do valor realizado, exclusao e busca por id inexistente.
  Suite completa do `core` seguiu verde (202 testes).
- Frontend: `sales-goals-page.component.spec.ts` cobrindo validacao, formatacao do mes, classificacao
  de severidade do atingimento e carregamento de vendedor ao editar. Suite completa (282 testes) e
  `npm run build` verdes.

## [Fase 5] — Documentos (PDF)

Terceira e ultima entrega da Fase 5 (Financeiro e documentos): geracao de PDF para Propostas,
Pedidos e Contratos, fechando a fase.

### Backend

- Nova dependencia `com.github.librepdf:openpdf` (modulo `core`), unica biblioteca de geracao de PDF
  do projeto ate aqui.
- `DocumentPdfWriter` (`com.primecrm.core.document`): monta o PDF (cabecalho, tabela de itens, total,
  observacoes) a partir de um modelo generico (`DocumentPdfModel`) — o mesmo escritor atende Proposta,
  Pedido e Contrato, sem duplicar layout por tipo de documento.
- `DocumentPdfService`: busca os dados de cada documento (reaproveitando `ProposalService`,
  `OrderService`, `ContractService` e os respectivos *ItemService* ja existentes — nenhum acesso novo a
  repositorio), monta o modelo e registra auditoria (`AuditAction.EXPORT`), no mesmo padrao ja usado na
  exportacao CSV de relatorios.
- Novo endpoint `GET /{id}/pdf` em `ProposalController`, `OrderController` e `ContractController`,
  reaproveitando a permissao `*_VIEW` de cada modulo (sem RBAC novo).

**Decisao de modelagem — Contrato sem itens proprios**: como Contrato ja nao tem uma tabela de itens
(reaproveita o Pedido de origem desde a Fase 4), o PDF do contrato busca os itens do pedido vinculado
(`contract.order`) quando existir; contratos criados sem um pedido de origem geram PDF sem tabela de
itens.

### Frontend

Botao "Baixar PDF" nas listagens de Propostas, Pedidos e Contratos, ao lado das demais acoes de linha,
disponivel para qualquer usuario com permissao de visualizacao do modulo. Reaproveita o utilitario
`downloadBlob` ja usado na exportacao CSV de Relatorios.

### Qualidade

- Backend: `DocumentPdfWriterTest` (PDF valido gerado com e sem itens/observacoes) e
  `DocumentPdfServiceTest` (nome do arquivo a partir do codigo, itens do pedido vinculado no PDF do
  contrato, contrato sem pedido gera PDF sem itens, auditoria de exportacao). Suite completa do `core`
  seguiu verde (197 testes).
- Frontend: `npm run build` e suite completa (276 testes) verdes.

## [Fase 5] — Contas a Pagar

Segunda entrega da Fase 5 (Financeiro e documentos): despesas e obrigações financeiras da empresa
junto a fornecedores, com baixa de pagamento total ou parcial.

### Banco de dados

Migration `V40`: tabela `payables` e as 4 permissões `FINANCEIRO_*` reaproveitadas (as mesmas já
concedidas ao perfil Administrador na entrega de Contas a Receber — não há RBAC novo). Código legível
`PAG-######`.

**Decisões de modelagem**: fornecedor não ganhou uma entidade própria — reaproveita a tabela `customers`
existente, já que `CLIENT_TYPE` inclui `FORNECEDOR` desde a Fase 1. Categoria e forma de pagamento
reaproveitam os `domain_types` `CATEGORY` e `PAYMENT_METHOD` já existentes. `paid_amount` acumulado
separado de `amount` (mesmo padrão de Contas a Receber) permite pagamento parcial, e atraso é computado
(`status = PENDING` e `due_date` no passado) em vez de armazenado. Diferente de Contas a Receber, não há
endpoint de geração a partir de um documento de origem — uma despesa não nasce de um Pedido ou Proposta
neste sistema, então esse "generate from" não foi replicado.

### Backend

- CRUD completo (`/api/v1/payables`) com busca textual, filtros (status, fornecedor, categoria, período
  de vencimento, atraso), paginação, RBAC e auditoria.
- `PATCH /{id}/pay`: mesmo contrato de Contas a Receber — sem valor informado, baixa o saldo restante
  integralmente; com valor menor que o saldo, registra pagamento parcial e a conta permanece pendente.

### Frontend

Tela `/financeiro/contas-a-pagar`, com listagem, diálogo de CRUD e diálogo dedicado de baixa de
pagamento (mostrando o saldo restante). O item "Financeiro" da sidebar e o atalho "+ Novo" do topbar
viraram submenu com Contas a Receber e Contas a Pagar.

### Qualidade

- Backend: `PayableServiceTest` cobrindo status inicial, pagamento total e parcial, exclusão e busca
  por id inexistente. Suíte completa do `core` seguiu verde (191 testes).
- Frontend: `payables-page.component.spec.ts` cobrindo validação e diálogo de baixa. Suíte completa
  (276 testes) e `npm run build` verdes.

## [Fase 5] — Contas a Receber

Primeira entrega da Fase 5 (Financeiro e documentos): parcelas de pagamento (contas a receber)
originadas de Pedidos ou Contratos, com baixa de recebimento total ou parcial.

### Banco de dados

Migrations `V37` a `V39`: novo `domain_type` `PAYMENT_METHOD` (boleto/pix/cartão/transferência/dinheiro,
mesmo raciocínio de reaproveitar o engine genérico já usado em `UNIT_OF_MEASURE` e `BILLING_CYCLE`),
tabela `receivables` e as 4 permissões novas (`FINANCEIRO_*`) concedidas ao perfil Administrador. Código
legível `REC-######`.

**Decisões de modelagem**: cada linha de `receivables` já é uma parcela individual (uma conta = um
vencimento = um valor), não um título "pai" com parcelas filhas — mais simples de consultar, ordenar e
dar baixa sem precisar navegar uma relação pai/filho. `paid_amount` é acumulado separado de `amount`,
permitindo pagamento parcial: a conta só fecha (`status = PAID`) quando `paid_amount >= amount`. Em
atraso é computado (`status = PENDING` e `due_date` no passado), no mesmo padrão de `overdue`/`expired`
já usado em Tarefas, Agenda, Propostas e Contratos — sem depender de um job para marcar atraso sozinho.

### Backend

- CRUD completo (`/api/v1/receivables`) com busca textual, filtros (status, cliente, pedido, contrato,
  período de vencimento, atraso), paginação, RBAC e auditoria.
- `PATCH /{id}/pay`: registra um pagamento. Sem valor informado, baixa o saldo restante integralmente;
  com valor menor que o saldo, registra pagamento parcial e a conta permanece pendente.
- `POST /receivables/from-order/{orderId}`: divide o valor total do pedido em N parcelas mensais iguais
  (a última parcela absorve o resto da divisão, para a soma das parcelas nunca ficar diferente do valor
  total por causa de arredondamento).

### Frontend

O item "Financeiro" da sidebar, que desde a Fase 1 existia desabilitado como "em breve", foi finalmente
ativado. Tela `/financeiro/contas-a-receber` com listagem, diálogo de CRUD, diálogo dedicado de baixa de
pagamento (mostrando o saldo restante) e diálogo de geração de parcelas a partir de um pedido.

### Qualidade

- Backend: `ReceivableServiceTest` cobrindo status inicial, geração de parcelas (soma bate com o total,
  datas mensais, arredondamento), pagamento total e parcial, exclusão. Suíte completa do `core` seguiu
  verde (186 testes).
- Frontend: `receivables-page.component.spec.ts` cobrindo validação, diálogo de baixa e geração de
  parcelas. Suíte completa (270 testes) e `npm run build` verdes.

## [Fase 4] — Contratos

Fecha a Fase 4 (Comercial avançado): Contratos, a vigência formal de uma venda recorrente ou continuada,
tipicamente originada de um Pedido entregue.

### Banco de dados

Migrations `V34` a `V36`: novo `domain_type` `BILLING_CYCLE` (único/mensal/trimestral/semestral/anual,
mesmo raciocínio de `UNIT_OF_MEASURE` — reaproveita o engine em vez de um enum fixo), tabela `contracts`
e as 4 permissões novas (`CONTRATOS_*`) concedidas ao perfil Administrador. Código legível `CTR-######`.

**Decisão de modelagem — sem itens próprios**: diferente de Proposta e Pedido, Contrato não tem uma
tabela `contract_items`. O detalhamento (quais produtos, quantidades) já vive no Pedido de origem
(`contracts.order_id`); o contrato só precisa saber **quanto** cobrar e **com que frequência**
(`recurring_amount` + `billing_cycle_id`). Criar uma terceira cópia de CRUD de itens não agregaria nada —
seria a mesma tela pela terceira vez sem uma razão de negócio diferente, na contramão direta da regra do
projeto contra abstração/repetição sem necessidade real.

**Vencimento sem tabela nem status próprio**: assim como Proposta e Compromisso, "vencido" é computado
(`status = ACTIVE` e `end_date` no passado) em vez de um status `EXPIRED` gravado — sem depender de um job
para encerrar contratos sozinho. `end_date` nulo significa vigência indeterminada (contrato sem data de
fim, renovado ou não).

### Backend

- CRUD completo (`/api/v1/contracts`) com busca textual, filtros (status, cliente, oportunidade,
  responsável, vencidos), paginação, RBAC e auditoria; `PATCH /{id}/status` (encerrar preenche
  `terminated_at`, no mesmo padrão de `decidedAt`/`closedAt` já usado em Proposta/Pedido).
- `POST /api/v1/contracts/from-order/{orderId}`: cria um contrato copiando cliente, oportunidade e
  responsável do pedido, e usa o **valor total do pedido como valor recorrente inicial** (o usuário ajusta
  depois se o ciclo de faturamento não for igual ao valor cheio do pedido).

### Frontend

Tela `/contratos`, mesmo padrão visual dos demais módulos comerciais (sem diálogo de itens, já que
Contrato não tem), com ações rápidas por status (ativar, suspender, reativar, encerrar) e indicadores de
vencido/renovação automática. A tela de Pedidos ganhou o botão "Converter em Contrato" nas linhas com
status Entregue, fechando de ponta a ponta o fluxo **Proposta → Pedido → Contrato** que atravessa toda a
Fase 4.

### Qualidade

- Backend: `ContractServiceTest` (status padrão, criação a partir de pedido copiando cliente/valor,
  `terminatedAt`, exclusão). Suíte completa do `core` seguiu verde (180 testes).
- Frontend: `contracts-page.component.spec.ts` e o teste de conversão em `orders-page.component.spec.ts`.
  Suíte completa (263 testes) e `npm run build` verdes.

## [Fase 4] — Pedidos

Terceira entrega da Fase 4: Pedidos, a confirmação formal de uma venda — estrutura praticamente irmã de
Propostas (cabeçalho + itens, mesmo padrão de snapshot de preço), com um caminho de conversão direto a
partir de uma proposta aceita.

### Banco de dados

Migrations `V31` a `V33`: tabelas `orders` e `order_items` (idênticas em estrutura a `proposals`/
`proposal_items`, com `proposal_id` como referência opcional de origem) e as 4 permissões novas
(`PEDIDOS_*`) concedidas ao perfil Administrador. Código legível `PED-######`.

### Backend

- CRUD completo do cabeçalho (`/api/v1/orders`) e dos itens (`/api/v1/orders/{orderId}/items`), no mesmo
  padrão de Propostas — total recalculado no servidor, preço como snapshot, sem regra de transição de
  status forçada (`CONFIRMED`/`PENDING` livres; `DELIVERED`/`CANCELED` preenchem `closedAt`).
- `POST /api/v1/orders/from-proposal/{proposalId}`: cria um pedido copiando cliente, oportunidade,
  responsável e **todos os itens** da proposta (mesma quantidade, preço e desconto praticados) — sem essa
  rota, transformar uma proposta aceita em pedido significaria redigitar cada item na mão.

### Frontend

Tela `/pedidos`, mesmo padrão visual de Propostas (listagem + diálogo de cabeçalho + diálogo de itens),
com ações rápidas por status (confirmar, marcar como entregue, cancelar) e um chip mostrando o código da
proposta de origem quando o pedido nasceu de uma. A tela de Propostas ganhou o botão "Converter em Pedido"
nas linhas com status Aceita, fechando visualmente o fluxo Proposta → Pedido.

### Qualidade

- Backend: `OrderServiceTest` (status padrão, criação a partir de proposta copiando itens, `closedAt`,
  recálculo de total, exclusão) e `OrderItemServiceTest` (snapshot de preço, exclusão recalculando o
  total). Suíte completa do `core` seguiu verde (174 testes).
- Frontend: `orders-page.component.spec.ts` e o teste de conversão em `proposals-page.component.spec.ts`.
  Suíte completa (257 testes) e `npm run build` verdes.

## [Fase 4] — Propostas

Segunda entrega da Fase 4: Propostas, o documento comercial enviado ao cliente com itens de
produto/serviço, construído sobre o catálogo de Produtos da entrega anterior.

### Banco de dados

Migrations `V28` a `V30`: tabelas `proposals` (cabeçalho) e `proposal_items` (itens), mais as 4
permissões novas (`PROPOSTAS_*`) concedidas ao perfil Administrador. Código legível `PRO-######` gerado
pelo banco, no mesmo padrão de Tarefas/Clientes/Leads/Oportunidades/Produtos.

**Decisão de modelagem — item como recurso aninhado, não array dentro do request**: seguindo o mesmo
padrão já usado em Pipeline + PipelineStage, o item da proposta tem endpoints próprios
(`/proposals/{id}/items`) em vez de viajar como um array dentro do `POST`/`PUT` da proposta. Descartei um
"salvar tudo de uma vez" porque criaria dois jeitos diferentes de fazer a mesma coisa no projeto — cada
edição de item vira sua própria chamada, auditada individualmente, exatamente como já acontece com etapas
de funil.

**Preço é um snapshot, não uma referência viva**: ao adicionar um item, o preço unitário do produto é
copiado para a linha da proposta no momento da inclusão (a menos que o usuário informe um preço
diferente). Se o preço do produto mudar depois, propostas já montadas não mudam de valor sozinhas — do
contrário, uma proposta enviada ontem por R$ 1.000 apareceria hoje por R$ 1.200 só porque o produto
reajustou.

**Total recalculado no servidor, nunca confiado ao cliente**: `proposals.total_amount` é a soma dos itens,
recalculada e persistida pelo backend (`ProposalService.recalculateTotal`) toda vez que um item é
criado/editado/removido — nunca um valor que o frontend envia. Isso também é o que permite listar e
ordenar propostas por valor total sem juntar (`join`) e somar itens a cada linha da listagem.

**Status sem regra de transição forçada**: diferente da movimentação de etapa de Oportunidade (que exige
motivo de perda), a proposta aceita qualquer transição de status livremente — `ACCEPTED`/`REJECTED`
preenchem `decidedAt` automaticamente (e o perdem se a proposta voltar a um status aberto), no mesmo
padrão já usado por `Task.completedAt`. Não há um status `EXPIRED` gravado no banco: expirado é computado
(`status = SENT` e `validUntil` no passado), igual ao `overdue` de Tarefas e Agenda — sem depender de um
job agendado para "expirar" propostas sozinho.

### Backend

CRUD completo do cabeçalho (`/api/v1/proposals`) com busca textual, filtros (status, cliente, oportunidade,
responsável, vencidas), paginação, RBAC e auditoria; `PATCH /{id}/status` para mudar o status; CRUD dos
itens (`/api/v1/proposals/{proposalId}/items`) sob a mesma permissão de edição da proposta
(`PROPOSTAS_EDIT`).

### Frontend

Tela `/propostas` (grupo "Módulos") com listagem, diálogo de cabeçalho e ações rápidas por linha (enviar,
aceitar, rejeitar, conforme o status atual). Os itens são geridos num diálogo próprio
(`ProposalItemsDialogComponent`), replicando o mesmo padrão visual do "Gerenciar Etapas" de Pipeline —
tabela com CRUD inline e mini-formulário, mostrando o total calculado em tempo real no rodapé.

### Qualidade

- Backend: `ProposalServiceTest` (status, `decidedAt`, recálculo de total, exclusão) e
  `ProposalItemServiceTest` (snapshot de preço do produto, override explícito, exclusão recalculando o
  total). Suíte completa do `core` seguiu verde (165 testes).
- Frontend: `proposals-page.component.spec.ts` cobrindo validação, troca de status e abertura do diálogo
  de itens. Suíte completa (250 testes) e `npm run build` verdes.

## [Fase 4] — Produtos

Primeira entrega da Fase 4 (Comercial avançado): catálogo de Produtos, base para Propostas, Pedidos e
Contratos, que ainda não foram construídos.

### Banco de dados

Migrations `V25` a `V27`: novo `domain_type` `UNIT_OF_MEASURE` (com 9 valores de exemplo — a unidade de
medida varia demais entre comércio/indústria/serviços para ser um enum fixo, então reaproveita o mesmo
engine `domain_types`/`domain_values` que já existe, em vez de uma tabela nova), tabela `products` e as
4 permissões novas (`PRODUTOS_*`), concedidas ao perfil Administrador.

**Decisões de modelagem**: categoria reaproveita o `domain_type` `CATEGORY` que já existia (compartilhado
com outros cadastros). A distinção produto físico × serviço é um booleano (`is_service`) e não um
`domain_value` nem um enum — é um eixo binário e fixo que não faz sentido o usuário configurar, diferente
de categoria/unidade que variam por tenant. `active` é um campo à parte do soft delete (`deleted_at`):
um produto pode ser descontinuado (inativo, mas com histórico preservado para quando Propostas/Pedidos
existirem) sem precisar ser excluído.

### Backend

CRUD completo (`/api/v1/products`) com busca textual (nome/código/SKU/descrição), filtros (categoria,
unidade, produto×serviço, ativo), paginação, RBAC e auditoria — no mesmo padrão dos demais módulos.
Código legível (`PRD-######`) gerado pelo banco via sequence, como em Tarefas/Clientes/Leads/Oportunidades.
SKU é único por tenant quando informado (índice parcial, mesmo padrão do documento de Cliente).

### Frontend

Tela `/produtos` (dentro do grupo "Módulos" da sidebar, junto de Leads/Contatos/Empresas), listagem com
`generic-table` e diálogo de CRUD com preço de venda/custo em `p-inputnumber` no formato moeda. Cadastro
de "Unidades de Medida" também passou a aparecer em Configurações → Cadastros Gerais, do mesmo jeito que
qualquer outro `domain_type`.

### Qualidade

- Backend: `ProductServiceTest` cobrindo criação, atualização, exclusão e busca por id inexistente.
  Suíte completa do `core` seguiu verde (155 testes).
- Frontend: `products-page.component.spec.ts` cobrindo validação do formulário, valores padrão, edição e
  filtro por categoria. Suíte completa (244 testes) e `npm run build` verdes.

## [Fase 3] — Notificações em tempo real

Fecha a Fase 3: o sino de notificações deixa de depender só do polling a cada 2 minutos e passa a
receber atualizações via WebSocket (STOMP), com o polling mantido como rede de segurança em caso de
queda de conexão.

### Decisão de arquitetura: push periódico no servidor, não push por evento de escrita

A alternativa óbvia seria publicar um evento a cada `save()`/`delete()` de Tarefa, Oportunidade, Lead e
Compromisso e recalcular a notificação do usuário afetado. Descartada porque parte das notificações
(`TASK_OVERDUE`, `OPPORTUNITY_CLOSE_DATE_PASSED`, `CALENDAR_EVENT_OVERDUE`) fica desatualizada só pela
passagem do tempo — uma tarefa vence à meia-noite sem que ninguém tenha salvo nada — então instrumentar
os `Service`s de escrita não seria suficiente sozinho, e ainda exigiria alterar cinco services diferentes
e memorizar quem é o "usuário afetado" de cada evento (nem sempre é só o `assignee`/`owner` atual: uma
reatribuição muda o afetado de dois usuários ao mesmo tempo).

Em vez disso, o backend mantém um registro dos usuários com sessão WebSocket ativa
(`NotificationSocketRegistry`, uma conexão pode ter múltiplas abas) e, a cada 20 segundos
(`NotificationPushScheduler`), reexecuta a mesma consulta que já existia (`NotificationService.list`) para
cada usuário conectado e envia o resultado via `SimpMessagingTemplate.convertAndSendToUser`. Mais simples,
cobre corretamente as notificações que dependem só do relógio, e nenhum service de negócio precisou ser
tocado — o preço é uma latência de até 20s em vez de instantânea, aceitável para um sino de notificações.

### Backend

- `spring-boot-starter-websocket` adicionado ao módulo `api`. Endpoint STOMP em `/ws` (`/ws/**` liberado
  no `SecurityConfig`, sem exigir o filtro de JWT via header — a autenticação acontece no próprio
  handshake).
- Autenticação do handshake via `access_token` na query string (WebSocket do navegador não permite header
  `Authorization` customizado): `JwtHandshakeInterceptor` valida o token com o `JwtTokenProvider` já
  existente e `NotificationHandshakeHandler` resolve o `Principal` da sessão STOMP como o id do usuário —
  é essa string que `convertAndSendToUser` usa para rotear a mensagem certa para a sessão certa.
- Cliente se conecta direto em `/ws/websocket` (o sufixo que o SockJS do Spring expõe para WebSocket puro),
  então não foi preciso trazer `sockjs-client` para o frontend — só `@stomp/stompjs`, que já fala
  WebSocket nativo.

### Frontend

- `NotificationSocketService` (`core/services/notification-socket.service.ts`): conecta ao abrir sessão
  autenticada (`effect()` observando `SessionStore.isAuthenticated`), reconecta sozinho em caso de queda
  (`reconnectDelay`) e desconecta no logout.
- `TopbarComponent` passou a atualizar `notifications`/`notificationTotal` tanto pela chamada REST
  original (primeira pintura da tela, e novamente a cada 2 minutos como rede de segurança) quanto pelas
  mensagens recebidas em `/user/queue/notifications` — o que chegar primeiro atualiza a tela.

### Qualidade

- Backend: `./mvnw compile` limpo nos três módulos e suíte completa de `core`/`api` verde (151 + 24 testes,
  os de integração via Testcontainers seguem pulados neste sandbox por falta de Docker, como já registrado
  neste arquivo). Não foi possível abrir uma conexão WebSocket real neste ambiente — a limitação de
  `Selector`/loopback já documentada no `CLAUDE.md` também impede subir o servidor Tomcat necessário para
  isso — então a verificação end-to-end do handshake fica pendente de execução local pelo usuário.
- Frontend: `npm run build` e `npm test` (238 testes) verdes.

## [Fase 3] — Agenda

Complementa a Fase 3: com Tarefas, Relatórios, Auditoria e Dashboard já entregues, faltava a Agenda.
Notificações em tempo real (WebSocket) continuam pendentes e ficam para uma próxima entrega — o sino
de notificações segue por polling, só que agora também enxerga compromissos atrasados.

### Banco de dados

Migrations `V23` e `V24`: tabela `calendar_events` e as 4 permissões novas (`AGENDA_*`), concedidas ao
perfil Administrador.

**Decisão de modelagem**: o compromisso reaproveita o mesmo `domain_type` `TASK_TYPE` que Tarefas já usa
para classificar tipo (reunião, ligação, visita...) em vez de criar um catálogo próprio — a distinção
entre "tarefa" e "compromisso" já está na tabela (uma tem prazo, a outra tem início/fim), não precisa
duplicar o tipo. Por não ser um registro referenciado externamente como cliente/lead/oportunidade, o
compromisso não tem código legível (`AGE-######`) — só id.

### Backend

- CRUD completo (`/api/v1/agenda`) com filtros (status, tipo, responsável, cliente, lead, oportunidade,
  período de início e atraso), busca textual, paginação, RBAC e auditoria — no mesmo padrão de Tarefas.
- `GET /api/v1/agenda/range?from=&to=` devolve, sem paginação, todos os compromissos que se sobrepõem ao
  período informado — é o endpoint que a visão de calendário (mês) usa para carregar a grade inteira em
  uma chamada só, em vez de uma requisição por dia visível.
- Compromisso "atrasado" considera o fim (`end_at`, ou o próprio início quando não há fim) contra o
  instante atual, não só o início — um compromisso de duas horas que já começou não é atraso enquanto
  ainda está dentro da janela.
- O sino de notificações passou a incluir compromissos atrasados do próprio usuário (`CALENDAR_EVENT_OVERDUE`),
  no mesmo mecanismo derivado (sem tabela de lida/não lida) que já valia para tarefas, oportunidades e leads.

### Frontend

Tela única (`/agenda`) com duas visões alternáveis por um `p-selectButton`:

- **Calendário** (padrão): grade mensal construída em CSS Grid puro — sem biblioteca de calendário nova,
  no mesmo espírito das visualizações do dashboard — com navegação mês anterior/próximo, atalho "Hoje",
  destaque do dia atual e um chip por compromisso (atrasado em vermelho, cancelado riscado). Clicar num
  dia vazio já abre o diálogo de criação com a data preenchida; clicar num chip abre a edição.
- **Lista**: mesmo padrão de `generic-table` já usado em Tarefas/Clientes/etc., com filtros de status,
  responsável e atraso.
- Compromisso ganhou atalho no botão "+ Novo" da topbar e item próprio na sidebar, ambos condicionados à
  permissão (`AGENDA_CREATE`/`AGENDA_VIEW`).

### Qualidade

- Backend: `CalendarEventServiceTest` cobrindo status padrão, validação de intervalo (fim antes do início),
  troca de status, exclusão e o guard de `range` sem período informado. Suíte completa do módulo `core`
  seguiu verde (`./mvnw -pl core test`).
- Frontend: `agenda-page.component.spec.ts` cobrindo carregamento inicial do mês, troca para lista,
  validação do formulário, valor padrão de status, edição e navegação entre meses. Suíte completa
  (238 testes) e `npm run build` verdes.

## [Fase 3] — Tarefas, Relatórios e Auditoria consultável

### Banco de dados
Migrations `V19` a `V21`: tabela `tasks` (com sequence do código legível `TAR-001042`), expansão do `CHECK` de `audit_log.action` para incluir eventos de sessão e extração (`LOGIN`, `LOGIN_FAILED`, `LOGOUT`, `EXPORT`) mais índices de apoio à tela de auditoria, e 9 permissões novas (`TAREFAS_*`, `RELATORIOS_*`, `AUDITORIA_*`) concedidas ao perfil Administrador.

**Decisões de modelagem**: a tarefa é genérica e se liga *opcionalmente* a cliente, contato, lead e oportunidade — em vez de uma tabela de atividade por módulo. Tipo e prioridade reaproveitam o engine `domain_values` (`TASK_TYPE` / `PRIORITY`), então não há enum fixo no código para eles; só `status` é enum, por ter regra de negócio associada (concluir preenche `completed_at`, sair de concluída limpa).

### Backend
- **Tarefas**: CRUD completo com filtros (status, tipo, prioridade, responsável, cliente, lead, oportunidade, período de vencimento e atraso), busca textual, paginação, RBAC e auditoria. `PATCH /tasks/{id}/status` para concluir/reabrir sem enviar o registro inteiro.
- **Relatórios** (`/api/v1/reports/{customers|opportunities|tasks}`): agregação genérica por dimensão. Um único executor (`ReportAggregator`) monta `group by` + `count` + `sum` via Criteria API; cada relatório declara suas dimensões em um enum (`CustomerReportGroupBy`, `OpportunityReportGroupBy`, `TaskReportGroupBy`), então adicionar um agrupamento novo é uma linha de enum, não um endpoint novo. São 35 agrupamentos no total — inclusive agrupamentos por mês (`to_char`) e um `CASE` para "em atraso". Oportunidades também somam o valor (`amount`). Exportação em CSV por relatório.
- **Auditoria consultável**: a gravação já existia desde a Fase 1; agora existe API de leitura (`/api/v1/audit-logs`) com filtros por entidade, registro, ação, usuário e período, linha do tempo de um registro específico e exportação CSV. A cobertura foi ampliada para **eventos de sessão** (login, login recusado e logout) e **extrações de dados** — relatórios e exportações registram a si mesmos no log. Eventos de sessão são gravados em transação própria (`REQUIRES_NEW`), e não pelo listener de after-commit, senão um login recusado (que faz rollback) nunca seria auditado.

### Frontend
- **Sidebar reorganizada**: Clientes, Oportunidades e Tarefas passam a ser itens de primeiro nível, fora de qualquer submenu; Leads, Contatos, Empresas e Financeiro ficam em "Módulos"; e entram os grupos "Relatórios" e "Auditoria" (dentro de Configurações). Todos os itens continuam filtrados por permissão.
- **Tarefas**: listagem com filtros de status, responsável e atraso, destaque visual de tarefas vencidas, ação rápida de concluir e dialog de CRUD.
- **Relatórios**: uma única tela (`/relatorios/:report`) atende os três relatórios, com seletor de agrupamento, período, responsável, cartões de totais, barra de distribuição por linha e exportação CSV.
- **Auditoria**: listagem com filtros e dialog de detalhes que renderiza o diff campo a campo (valor anterior → novo valor) e os metadados do evento (IP, navegador, usuário).

### Dashboard com dados reais

Os cartões demonstrativos saíram; o dashboard agora consome `GET /api/v1/dashboard`, que devolve tudo em **uma chamada** — indicadores do período, funil, série mensal, ranking e tarefas — em vez de a tela orquestrar seis requisições.

- **Seis indicadores** com comparação contra o período imediatamente anterior de mesmo tamanho (receita ganha, pipeline aberto, taxa de ganho, ticket médio, novos leads e clientes ativos). Quando não há período anterior com movimento, a variação vem nula e a tela mostra "sem base" em vez de um falso 0%/100%.
- **Gráficos em SVG, sem dependência nova**: nenhuma biblioteca de chart foi adicionada. São dois componentes reutilizáveis em `shared/components/charts` — área/linha (12 meses, ganho preenchido + aberto tracejado, com tooltip nativo por ponto) e rosca (ganhas/perdidas/em aberto, com a taxa de ganho no centro). Ambos usam as variáveis de tema do preset, então respondem a tema claro/escuro sem código extra.
- **Funil por etapa** com barras na cor da própria etapa, proporcionais à maior etapa, e o percentual de cada uma sobre o funil; **ranking de responsáveis** por valor ganho, com barra de participação.
- **Período selecionável** (7/30/90 dias) e recarga manual, com skeleton no primeiro carregamento e estado de erro com botão de tentar novamente.
- Os blocos de funil/ranking e de tarefas só aparecem para quem tem `OPORTUNIDADES_VIEW` e `TAREFAS_VIEW`; o endpoint em si exige apenas usuário autenticado.

**Fuso horário**: as fronteiras de período são calculadas em `America/Sao_Paulo` e não em UTC — senão "hoje" e "este mês" mudariam de valor às 21h no horário de Brasília.

**Grid e responsividade**: o layout usa **container queries** (`@container`), não media queries de viewport. A diferença importa porque a sidebar recolhe: em 768px de viewport com a sidebar aberta sobram 414px de conteúdo, e uma media query de viewport acharia que "cabe bastante". Os cartões reagem à largura real da área de conteúdo — 6 colunas acima de 78rem, 3 acima de 48rem, 2 acima de 25rem e 1 abaixo disso. O valor de cada cartão também escala pela largura do próprio cartão (`clamp(1.1rem, 10cqi, 1.5rem)`), o que resolve o estouro de números longos como `R$ 34.146.000,00` sem depender de fonte fixa. Medido em 1920/1440/768/375: nenhum valor truncado e nenhum overflow horizontal.

**Tooltips**: cartões de indicador, etapas do funil, linhas do ranking e cartões de tarefa têm tooltip com o número por extenso e a leitura da variação ("Alta de 153,92% em relação ao período anterior"). Nos gráficos SVG o tooltip é `<title>` nativo — ponto do gráfico mensal e fatia da rosca —, sem JavaScript de posicionamento.

### Trava total do navegador ao abrir "Cadastros Gerais" (e outros dois diálogos de configuração)

Clicar em qualquer cadastro dentro de `Configurações → Cadastros Gerais` (ex.: Tipo de Cliente) travava a aba inteira — nenhuma interação, nem o DevTools, respondia mais.

**Causa raiz**: o `loadingInterceptor` global (que liga/desliga a barra de carregamento) lê e escreve o signal `LoadingStore.activeRequests` de forma **síncrona**, no meio de toda chamada HTTP — antes mesmo de qualquer resposta chegar. `domain-values-page.component.ts` disparava essa chamada (`domainTypeService.list()`) de dentro de um `effect()` do construtor sem envolver a chamada em `untracked()`. Como o Angular Signals rastreia **qualquer** leitura de signal que ocorra durante a execução síncrona de um `effect()` — mesmo dentro de serviços/interceptors injetados, várias camadas abaixo —, a leitura de `activeRequests()` feita pelo interceptor virou dependência implícita desse `effect`. E como o próprio interceptor escreve nesse signal logo em seguida, o `effect` era marcado sujo e reagendado — disparando `load()` de novo, uma nova requisição, uma nova leitura+escrita do interceptor, e assim indefinidamente. Cada iteração é agendada via microtask, então o navegador nunca chega a "devolver" o controle para pintar a tela ou responder ao DevTools: trava de verdade, não só fica lento.

Diagnosticado ao vivo, sem alterar comportamento: um contador temporário dentro do `effect` (removido depois) mostrou o padrão exato — cada nova requisição XHR (capturada via `XMLHttpRequest.prototype.open` interceptado) correspondia a exatamente mais uma execução do `effect`, comprovando o ciclo.

**Correção em duas camadas**:
- Nos três pontos que tinham o mesmo padrão perigoso (`effect()` chamando um `load()` que faz HTTP, sem `untracked()`) — `domain-values-page.component.ts`, `pipeline-stages-dialog.component.ts` ("Gerenciar Etapas" de um pipeline) e `role-permissions-dialog.component.ts` ("Gerenciar Permissões" de um perfil) —, o corpo que dispara a chamada HTTP passou a rodar dentro de `untracked()`, igual ao padrão já usado em `reports-page.component.ts` (que por isso nunca teve esse problema).
- Na raiz, `LoadingStore.start()`/`.stop()` pararam de ler `store.activeRequests()` via getter reativa e passaram a usar a forma de **updater function** do `patchState` (`(state) => ({...})`), que recebe um snapshot em vez de fazer uma leitura reativa. Isso elimina a classe inteira do problema: nenhum `effect()` futuro — mesmo esquecendo o `untracked()` — pode mais ser capturado como dependente do contador de requisições globais.

Coberto por `loading.store.spec.ts`: um teste isolado recria o mecanismo exato (um `effect()` ambiente que chama `store.start()`/`.stop()` de forma síncrona) e comprova que ele executa uma única vez mesmo após múltiplos `TestBed.tick()`. Validado que esse teste realmente pega o bug — revertendo `LoadingStore` para a versão antiga, o próprio Chrome Headless do Karma travou e caiu por timeout, igual ao navegador real.

### Indent ausente no terceiro nível da sidebar

Itens dentro de um grupo aninhado (ex.: "Tipo de Cliente" dentro de "Cadastros Gerais" dentro de "Configurações") apareciam no mesmo x que "Cadastros Gerais" e que os irmãos de segundo nível, sem indicar a hierarquia. Causa: a regra global `.p-panelmenu-submenu { padding: 0; }` zerava o indent de **qualquer** submenu aninhado, mas o PrimeNG marca a lista raiz do menu com as classes `p-panelmenu-root-list` E `p-panelmenu-submenu` ao mesmo tempo — então a mesma regra também afetava (sem querer) uma lista que não devia ganhar indent. Corrigido separando as duas: `.p-panelmenu-root-list` continua sem padding extra (o indent do primeiro nível já vem de `.p-panelmenu-content`), e `.p-panelmenu-submenu:not(.p-panelmenu-root-list)` — ou seja, apenas listas *realmente* aninhadas — ganha `padding-left: 0.85rem` e a mesma linha guia vertical sutil já usada no primeiro nível. Medido no navegador: nível 1 permanece em 23px (inalterado), nível 2 passa a 37px.

### Topbar funcional e ergonomia dos diálogos

- **Rodapé fixo nos diálogos**: os botões Salvar/Cancelar param de rolar junto com o formulário. O rodapé gruda no fim da área rolável (`position: sticky` ancorado no `.p-dialog-content`) e os campos rolam por baixo dele. Foi feito no `.dialog-footer` global, então vale para os 17 diálogos do sistema de uma vez, sem tocar em nenhum template. As margens negativas cobrem o padding do diálogo — e para essa conta fechar o padding do `.p-dialog-content` passou a ser definido por `--pc-dialog-padding` em vez de depender do valor interno do tema.
- **Notificações** (`GET /api/v1/notifications`): o sino deixou de ser enfeite. Os alertas são derivados dos dados reais — tarefas do usuário atrasadas e vencendo hoje, oportunidades dele com previsão de fechamento vencida e leads sem responsável — com badge de contagem, ícone por severidade, atualização a cada 2 minutos e clique navegando para o módulo. Não há tabela de "lida/não lida": o alerta some quando o motivo dele deixa de existir, que é o comportamento honesto para algo derivado.
- **Busca global** (`GET /api/v1/search`): busca única sobre clientes, contatos, leads, oportunidades e tarefas, com no máximo 5 resultados por módulo, debounce de 300 ms e mínimo de 2 caracteres. Cada módulo só é consultado se o usuário tiver a permissão de visualização correspondente — a checagem é passada como predicado para o service, então a regra fica em um lugar só.
- **Botão "+ Novo"**: os atalhos passaram a navegar para o módulo com `?novo=1`, e a página abre o diálogo de criação sozinha. O parâmetro é limpo da URL logo depois, senão o atalho só funcionaria uma vez por página. Itens sem permissão de criação continuam desabilitados.
- **Alterar senha** (`PATCH /api/v1/auth/password`): o próprio usuário troca a senha informando a atual, sem depender de `USUARIOS_EDIT`. A troca revoga os refresh tokens ativos (os outros dispositivos precisam entrar de novo) e é auditada como `PASSWORD_CHANGED` (migration `V22`).
- Os itens "Editar Perfil", "Preferências" e "Central de Ajuda" foram **removidos** do menu do usuário em vez de continuarem desabilitados: os três dependem de telas que ainda não existem, e menu morto é pior que menu ausente.

### Indicador de carregamento

A barra de progresso no topo saiu: ela ficava no fluxo do documento, e seus 3px ligavam a barra de rolagem vertical, encolhendo a largura útil da página a cada requisição. No lugar dela entrou um **overlay centralizado** com o `loading.gif`, em `position: fixed` cobrindo a viewport — como não ocupa espaço no fluxo, a página não muda de tamanho durante o carregamento.

O overlay fica acima dos diálogos (z-index 2000 contra 1102 do dialog), traz `role="status"` com rótulo traduzido para leitores de tela e escurece levemente o fundo com desfoque, o que também impede cliques enquanto a requisição está em andamento. A `LoadingStore` e o interceptor continuam iguais — a contagem de requisições ativas é que liga e desliga o overlay.

### Selects: dependência, mensagem vazia e sobreposição

Três problemas somados no diálogo de lead — o campo Etapa devolvia "No results found" ao ser aberto:

- **Causa real**: Etapa depende de Funil. Enquanto nenhum funil estava escolhido a lista era legitimamente vazia, e o PrimeNG exibia a mensagem padrão dele, em inglês. O campo agora **fica desabilitado** com o texto "Selecione um funil primeiro" e só libera quando o funil escolhido tem etapas — a dependência ficou explícita em vez de aparecer como erro. Vale para o diálogo de criação e para o de conversão de lead.
- **Vazio × falha**: as mensagens de lista vazia do PrimeNG passaram a vir do i18n (`PrimeNG.setTranslation` sincronizado com o `ngx-translate`, reaplicado a cada troca de idioma), então todo select/multiselect do sistema diz "Nenhum resultado encontrado" no idioma ativo. Para distinguir de uma falha de carga, um helper (`createOptionsState`) guarda `items` + `failed` por lista e escolhe a mensagem: quando a requisição das opções quebra, o select mostra **"Não foi possível carregar"** em vez de sugerir que o banco está vazio.
- **Painel atrás do rodapé**: regressão do rodapé fixo introduzido nesta mesma fase — com `z-index: 1` ele passou a pintar por cima dos painéis, que o PrimeNG renderiza *dentro* do formulário. `overlayOptions.appendTo` no `providePrimeNG` não resolve (o `p-select` não consulta essa chave), então o `appendTo="body"` foi aplicado nos próprios componentes: os painéis saem do diálogo, deixam de ser recortados pela área rolável e passam a flutuar acima de tudo. Verificado no navegador: painel fora do diálogo, fundo opaco e a opção como elemento no topo do ponto de sobreposição.

### Identidade visual

A marca do produto passou a ser um SVG único (`P` branco sobre quadrado azul da paleta primária, com o ponto âmbar). O mesmo arquivo alimenta a aba do navegador, a topbar e as duas marcas da tela de login — antes cada lugar usava o ícone `pi-verified` do PrimeIcons dentro de um quadrado com gradiente CSS.

- `favicon.svg` (64px, para a aba) e `logo.svg` (mesmo desenho em 144px, para uso na interface).
- `favicon.ico` **multi-resolução** (16/32/48/64) para os casos que não aceitam SVG — abas de navegadores antigos, favoritos, atalho do Windows — e `apple-touch-icon.png` (180px) para iOS.
- Sem ImageMagick ou biblioteca de imagem no ambiente, os PNGs foram rasterizados pelo próprio Chrome (canvas a partir do SVG) e o container `.ico` foi montado em Node — cabeçalho ICONDIR + entradas apontando para PNGs embutidos, formato aceito de Windows Vista em diante. A estrutura do arquivo foi validada lendo o binário de volta: 4 imagens, dimensões conferindo entre a entrada do diretório e o cabeçalho de cada PNG.
- Os contêineres CSS das marcas perderam `background`/`gradiente` próprios (o SVG já traz o fundo), mantendo só tamanho, raio e sombra.

### Correções

**Não era possível criar nenhum registro pela API estando logado** (`409 CONFLICT — "Operacao viola uma restricao de unicidade ou integridade dos dados"`). A causa não era unicidade: o `AuditorAware` do JPA usa `authentication.getName()` para preencher `created_by`/`updated_by` (`VARCHAR(120)`), e o principal `AuthenticatedUser` era um record comum — o Spring Security então caía no `principal.toString()`, que traz id, e-mail, login, nome, perfis **e a lista inteira de permissões**, passando de 120 caracteres. O erro real no Postgres era `valor é muito longo para tipo character varying(120)`, mascarado pelo `DataIntegrityViolationException` genérico. `AuthenticatedUser` passou a implementar `AuthenticatedPrincipal`, expondo o login como nome de autenticação — que é o valor que deveria estar em `created_by` desde o início. Coberto por teste de regressão contra o banco real (`AuditorColumnRegressionTest`), que falha com a mensagem original se a correção for revertida.

**Toasts e diálogos apareciam atrás da topbar.** Duas causas somadas: a escala de z-index da aplicação (topbar 1200, sidebar 1100) estava **acima** da camada flutuante do PrimeNG (overlays 1000+, modais 1100+), e o deslocamento vertical do toast (`top: 5.25rem`) nunca chegou a valer porque o PrimeNG escreve `top: 20px` por style inline. Corrigido invertendo a escala — o "chrome" da aplicação foi para baixo da camada de overlays (sidebar 899/900, topbar 950) em vez de empurrar cada componente do PrimeNG para cima — e mantendo o deslocamento do toast com `!important`, único jeito de vencer o inline style. Isso corrige junto o mask de diálogo, que agora escurece a topbar, e o drawer da sidebar no mobile. A configuração `zIndex` do `providePrimeNG` não serve para isso: o `setConfig` do PrimeNG 20.1 ignora essa chave.

### Qualidade
- Backend: 182 testes (`./mvnw verify` verde), com testes novos de `TaskService`, `ReportService`, `AuditLogService`, `DashboardService`, do escritor de CSV e das duas correções acima. As agregações do dashboard também têm teste rodando contra o Postgres local, que quebra se alguma query JPQL parar de compilar no banco.
- Frontend: 231 testes (`npm test` verde) e `npm run build` verde.

---

## [Fase 2] — Núcleo Comercial

### Banco de dados
Migrations `V13` a `V18`: `customers`, `contacts`, `leads`, `opportunities`, `opportunity_stage_history`, `customer_tags`, `lead_tags`, mais 18 permissões novas (`CLIENTES_*`, `CONTATOS_*`, `LEADS_*`, `OPORTUNIDADES_*`) e sequences para os códigos legíveis (`CLI-001042`, `LEAD-...`, `OPO-...`).

**Decisões de modelagem**: clientes e empresas vivem na **mesma tabela** (`customers`, diferenciados por `person_type`) — a tela "Empresas" é o mesmo cadastro filtrado por pessoa jurídica, evitando duplicar endereço, contato e histórico. Os códigos legíveis são gerados por `DEFAULT` no banco, não no Java, para que inserções por SQL puro (carga de dados) também recebam código e não haja corrida de concorrência.

### Backend
CRUD completo dos quatro recursos com filtros dinâmicos, paginação, RBAC e auditoria. Regras de negócio:
- **Conversão de lead em cliente** (`POST /leads/{id}/convert`), opcionalmente já criando a oportunidade no funil escolhido; um lead só pode ser convertido uma vez.
- **Movimentação de etapa** (`PATCH /opportunities/{id}/stage`) gravando histórico com dias na etapa anterior, recalculando a probabilidade pela etapa destino e exigindo motivo quando a etapa é de perda ou de ganho.
- **Endpoint de board** (`GET /opportunities/board`) devolvendo o Kanban pronto — colunas na ordem, com totalizadores e limite por coluna.
- Contato principal único por cliente; documento (CPF/CNPJ) único por tenant.

Todas as listagens usam carregamento em lote, com teste de regressão garantindo que a contagem de queries não cresce com o número de linhas.

### Frontend
- **Clientes** com formulário em abas (dados gerais, contato, endereço, comercial) e validação real de CPF/CNPJ (dígito verificador, rejeitando sequências repetidas). Ao salvar com erro em aba oculta, a aba correspondente é aberta.
- **Empresas** reaproveitando o mesmo componente via `data` de rota, não uma cópia.
- **Contatos** com busca de cliente server-side.
- **Leads** com ação de conversão.
- **Kanban de Oportunidades** com arrastar e soltar entre etapas: movimento otimista, dialog exigindo motivo quando a etapa pede, e **reversão exata do card (posição e totalizadores) se a API falhar**. Visão de lista alternável, dialog de CRUD e gaveta de histórico em linha do tempo.

### Massa de dados
`scripts/demo-data.sql` (configurações, usuários, perfis, funis) e `scripts/demo-data-commercial.sql` (320 clientes, 512 contatos, 260 leads, 420 oportunidades e 1.316 movimentações). Ambos idempotentes. Os documentos são gerados com dígito verificador válido — verificado passando os 320 pelo validador real da aplicação.

---

## [Fase 0 + Fase 1] — Fundação, Parametrização e RBAC

### Fundação (Fase 0)

- **Monorepo**: backend Spring Boot e frontend Angular no mesmo repositório, com Docker Compose, Dockerfiles (backend e frontend) e CI no GitHub Actions (`.github/workflows/ci.yml`).
- **Backend**: Java 21, Spring Boot 3.5.16, Maven multi-módulo (`shared` → `infra` → `core` → `api`) com Maven Wrapper (não exige Maven instalado globalmente). `BaseEntity` com id UUID, `tenant_id`, auditoria de criação/alteração e soft delete. `GlobalExceptionHandler` com códigos de erro padronizados. Swagger/OpenAPI em `/swagger-ui.html`.
- **Autenticação JWT**: access token + refresh token rotativo (armazenado apenas como hash SHA-256 no banco), endpoints `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/me`. Spring Security 6 stateless, BCrypt, CORS configurável.
- **Frontend**: Angular 20 standalone (sem NgModules), lazy loading por rota, PrimeNG 20 com preset de tema customizado na paleta do produto, PrimeFlex, PrimeIcons. Estado global com `@ngrx/signals` (Signal Store) para sessão, tema e layout. i18n em runtime com `@ngx-translate` (pt-BR, en, es — 3 idiomas com paridade total de chaves).
- **Layout**: TopBar (busca global, workspace, notificações, botão "+ Novo", menu do usuário com tema/idioma/logout) e Sidebar recolhível com accordion, filtrada por permissão. Tema claro/escuro com persistência.
- **Guards e interceptors**: `authGuard`, `permissionGuard`, `unsavedChangesGuard`; interceptors de JWT, tratamento de erro (toast padronizado a partir do `ApiErrorResponse` do backend), loading global e refresh automático de token em 401.

### Parametrização e RBAC (Fase 1)

- **Engine genérico de domínio**: tabelas `domain_types` (catálogo de 16 tipos: tipo de cliente, tipo de pessoa, tipo de empresa, segmento, ramo de atividade, origem do lead, motivo de perda, motivo de ganho, status, prioridade, tipo de tarefa, categoria, tag, equipe, cargo, departamento) e `domain_values`. Um único CRUD (backend e frontend) atende todos os cadastros simples — nada é enum fixo no código, tudo é configurável em banco.
- **Módulos de configuração dedicados**: Pipelines + Etapas (com probabilidade padrão, SLA, cor e obrigatoriedade de motivo de perda), Campos Personalizados, Templates, Configurações Gerais (chave/valor) e Feriados.
- **RBAC**: CRUD de Usuários (com status ativo/inativo/bloqueado, atribuição de perfis e reset de senha), Perfis de Acesso (com vínculo de permissões agrupadas por módulo) e catálogo de Permissões (somente leitura). Todos os endpoints protegidos com `@PreAuthorize` por código de permissão, e a UI esconde/desabilita ações conforme as permissões do usuário logado.
- **Auditoria**: gravação em `audit_log` de criação, edição e exclusão nos 9 services de escrita, com diff de campos (`{"campo": {"old": ..., "new": ...}}`), usuário, tenant, IP e user-agent. A gravação ocorre após o commit da transação de negócio e nunca derruba a operação principal em caso de falha. Campos sensíveis (senha, hash, token, secret) são removidos do diff.
- **Padrão de listagem**: componente `generic-table` compartilhado sobre o `p-table` do PrimeNG, com busca com debounce, paginação e ordenação server-side, reordenação por drag and drop (onde há ordem de exibição), skeleton loader e empty state.

### Banco de dados

12 migrations Flyway (`V1` a `V12`), aditivas e versionadas, cobrindo: extensão pgcrypto, engine de domínio, usuários, perfis/permissões/vínculos, refresh tokens, audit log, pipelines e etapas, campos personalizados, templates, configurações gerais, feriados e seed inicial (catálogo de permissões, perfil Administrador, usuário admin, tipos e valores de domínio de exemplo, configurações padrão).

### Qualidade

- Backend: 93 testes (JUnit 5 + Mockito nos services, `@WebMvcTest` nos controllers). `./mvnw verify` verde.
- Frontend: 47+ testes (Karma/Jasmine). `npm run build` e `npm test` verdes.
- Código sem comentários em backend e frontend (convenção do projeto).
- Revisão de segurança: senha nunca exposta em DTO de resposta ou log, todos os endpoints não públicos com `@PreAuthorize`, segredo JWT de desenvolvimento claramente marcado como placeholder.

### Correções de performance e robustez

- **Loop infinito de requisições (crítico)**: o `generic-table` mantinha o `p-table` dentro de um `@if/@else` ligado ao estado de carregamento, então a tabela era destruída e recriada a cada carga — e cada recriação disparava um novo `onLazyLoad`. Em telas cuja listagem voltava vazia o ciclo nunca estabilizava (medido: ~90 requisições por segundo, travando a interface e inundando o log de SQL). A tabela agora fica sempre montada, com o esqueleto de carregamento no template `loadingbody` do próprio PrimeNG.
- **Carga dupla em toda tela**: a página carregava no construtor e a tabela disparava a carga inicial ao nascer. Com `lazyLoadOnInit=false`, a carga inicial é só a da página. Como rede de segurança, o componente também ignora consultas idênticas consecutivas.
- **N+1 em listagens**: `PipelineService`, `RoleService` e `UserService` consultavam o banco uma vez por linha (10 registros = 11 consultas). Passaram a carregar etapas/permissões/perfis em lote (`IN (...)`), com agrupamento em memória. Há teste de regressão garantindo que a contagem de consultas não cresce com o número de linhas.
- **Log de SQL**: `org.hibernate.SQL` estava em `DEBUG` com `format_sql` ligado no perfil dev, imprimindo cada consulta em várias linhas. Agora vem desligado por padrão e é opt-in via `SQL_LOG_LEVEL` / `SQL_LOG_FORMAT` (nível da aplicação via `APP_LOG_LEVEL`).
- **Ajustes de JPA**: `open-in-view=false` (não segura conexão do pool durante a serialização) e `default_batch_fetch_size=25` como defesa contra N+1 residual.
- **Cache dos catálogos estáticos**: `domain_types` e `permissions` são catálogos fixos e somente leitura, agora servidos de cache em memória.

### Decisões de escopo desta fase

Ficaram deliberadamente fora, por dependerem de integrações externas reais ou de módulos ainda não construídos: configuração de e-mail/SMTP real, WhatsApp Business API, Google Calendar/Contacts, Zapier, Meta/Google Ads, Webhooks, Chaves de API, configuração de notificações (depende de WebSocket, Fase 3), configuração de backup, metas comerciais (depende do dashboard, Fase 6) e moedas/câmbio (depende do financeiro, Fase 5). Permissão em nível de campo e de registro ("somente meus registros") tem o modelo de dados preparado, mas a aplicação prática fica para quando existirem telas de negócio (Fase 2+). O Dashboard é um placeholder — os widgets reais são da Fase 6.

---

_Próximas fases no roadmap do [README.md](README.md)._
