# Changelog — Prime CRM

Entregas do projeto, organizadas por fase (roadmap completo no [README.md](README.md)).

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
