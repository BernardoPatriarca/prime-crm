# Changelog — Prime CRM

Entregas do projeto, organizadas por fase (roadmap completo no [README.md](README.md)).

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
