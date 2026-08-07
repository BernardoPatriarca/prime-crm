# CLAUDE.md — Prime CRM

Contexto persistente do projeto para qualquer sessão de desenvolvimento assistido. Leia isto antes de tocar em qualquer módulo.

## Visão geral

Prime CRM é um CRM corporativo genérico e configurável (comércio, indústria, serviços, consultorias, representantes, agências, imobiliárias). Monorepo com backend Spring Boot e frontend Angular no mesmo repositório. Documento de referência completo do produto: histórico da conversa que originou o projeto (guardado como `PLAN.md` de cada fase, se existir, e no `CHANGELOG.md`).

Este projeto é desenvolvido em **fases incrementais** (ver seção "Roadmap" no `README.md` e no `CHANGELOG.md`). Nunca inicie uma fase nova sem concluir os critérios de aceite da anterior, salvo autorização explícita.

## Stack e versões exatas

**Backend**
- Java 21
- Spring Boot 3.5.16 (linha 3.x, conforme especificação do produto)
- Spring Security 6, Spring Data JPA, Hibernate
- Flyway (flyway-core + flyway-database-postgresql)
- PostgreSQL 16+ (ambiente local roda PostgreSQL 18 nativo no Windows)
- Lombok, MapStruct 1.6.3
- springdoc-openapi 2.9.0 (Swagger UI)
- JWT via io.jsonwebtoken (jjwt) 0.13.0
- Maven multi-módulo com Maven Wrapper (não depende de Maven instalado globalmente)

**Frontend**
- Angular 20 (standalone components, sem NgModules)
- PrimeNG 20 + @primeuix/themes 3 (preset customizado, ver `frontend/src/app/core/theme/prime-crm-preset.ts`) + PrimeFlex + PrimeIcons
- @ngrx/signals 20 (Signal Store) para estado global
- @ngx-translate/core 18 + @ngx-translate/http-loader para i18n em runtime (pt-BR/en/es)
- RxJS, Reactive Forms

## Estrutura de pastas

```
prime-crm/
  backend/
    pom.xml                 # parent Maven (packaging pom)
    shared/                 # excecoes (ApiException e subclasses), DTOs base (ApiErrorResponse, PageResponse), TenantContext
    infra/                  # entidades JPA (BaseEntity + subclasses), repositories, migrations Flyway (db/migration)
    core/                   # services de negocio, mappers MapStruct, autenticacao JWT (provider/validator)
    api/                    # controllers REST, SecurityConfig, GlobalExceptionHandler, PrimeCrmApplication (main), application*.yml
    mvnw / mvnw.cmd
  frontend/
    src/app/core/           # guards, interceptors, services, Signal Stores, theme
    src/app/layout/         # shell, topbar, sidebar
    src/app/features/       # auth, dashboard, settings/* (um subdiretorio por modulo de configuracao)
    src/app/shared/         # componentes reutilizaveis (generic-table etc), pipes, validators
    src/assets/i18n/        # pt-BR.json, en.json, es.json
  docker-compose.yml        # postgres + backend + frontend (uso futuro/producao/CI — dev local usa Postgres nativo)
  .env.example
  README.md                 # como rodar o projeto (fonte de verdade para setup)
  CHANGELOG.md               # o que foi entregue em cada fase
  .github/workflows/ci.yml
```

## Convenções

- **Nomenclatura**: entidades, tabelas e código em inglês (`snake_case` no banco, `camelCase`/`PascalCase` em Java/TS); textos de tela e labels em português (pt-BR) como idioma padrão, com infraestrutura de i18n pronta para en/es.
- **Camadas backend**: Controller → Service → Repository → Entity. Nunca expor `@Entity` diretamente em uma resposta HTTP — sempre DTO de saída (Response) + MapStruct. Requisições usam DTO de entrada (Request) com Bean Validation.
- **Toda entidade de negócio** estende `com.primecrm.infra.entity.BaseEntity` (id UUID, tenant_id, auditoria created/updated at/by, soft delete via `deleted_at`). Catálogos técnicos fixos (ex.: `permissions`) podem não estender BaseEntity quando não fizer sentido ter tenant/soft-delete.
- **Multi-tenant**: single-tenant por enquanto, mas toda tabela de negócio já nasce com `tenant_id` (default `00000000-0000-0000-0000-000000000001`, ver `TenantContext`). Não remova essa coluna em migrations futuras.
- **Migrations Flyway são aditivas**: nunca edite uma migration já aplicada em um ambiente. Se precisar corrigir algo, crie uma nova migration.
- **Parametrização genérica**: preferir o engine `domain_types`/`domain_values` a criar uma tabela nova para cada cadastro simples de domínio (tipos, origens, motivos, tags, etc.). Só crie tabela dedicada quando a estrutura de dados realmente for diferente (ex.: `pipeline_stages` tem FK, ordem, probabilidade — não cabe no engine genérico).
- **Frontend**: componentes standalone, lazy loading por rota de feature, Signal Store (`@ngrx/signals`) para estado compartilhado entre componentes (sessão, tema, layout). Usar classes e props nativas do PrimeNG (`severity`, `size`, `variant` etc.) em vez de CSS avulso — SCSS customizado deve se limitar a tokens de tema e ao esqueleto de layout.
- **Sem comentários no código** (nem Javadoc, nem `//`, nem `/* */`), em backend e frontend — instrução explícita do dono do produto. Nomes de classe/método/variável devem se explicar sozinhos; se uma decisão não for óbvia, explique no relatório final da tarefa ou no `CHANGELOG.md`, nunca dentro do código.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`). Branches: `main`, `develop`, `feature/*`, `fix/*`.

## Comandos

**Backend** (a partir de `backend/`):
```bash
./mvnw clean install          # build completo de todos os modulos
./mvnw verify                 # build + todos os testes (93 atualmente)
./mvnw -pl api -am spring-boot:run   # roda a API (perfil dev por padrao, Postgres local)
```

O `spring-boot-maven-plugin` esta declarado com `<skip>true</skip>` no pom pai e `<skip>false</skip>` no modulo `api` — sem isso, `-pl api -am` falha com "Unable to find a suitable main class" porque o Maven tenta executar o goal tambem no pom agregador raiz. Nao remova essa configuracao.
No Windows sem Git Bash, use `mvnw.cmd` no lugar de `./mvnw`.

**Frontend** (a partir de `frontend/`):
```bash
npm install
npm start          # ng serve, http://localhost:4200
npm run build      # build de producao
npm test           # testes unitarios (Karma/Jasmine)
```

**Docker Compose** (a partir da raiz, requer Docker Desktop instalado — opcional para dev local):
```bash
docker compose up --build
```

## Banco de dados local (ambiente de desenvolvimento desta máquina)

PostgreSQL já roda nativamente como serviço do Windows (`postgresql-x64-18`) em `localhost:5432`. Banco `primecrm` já existe. Credenciais: usuário `postgres`, senha `1234` (ver `.env.example` e `backend/api/src/main/resources/application-dev.yml`). Não é necessário Docker para rodar o backend localmente nesta máquina.

## Limitação conhecida do ambiente de execução (sandbox desta sessão)

Neste ambiente de sandbox especificamente, `java.nio.channels.Selector.open()` falha com `IOException: Unable to establish loopback connection` (causa raiz: `UnixDomainSockets.connect0` retorna `Invalid argument`) — isso quebra qualquer tentativa de subir um servidor Tomcat embarcado (`spring-boot:run`, `@SpringBootTest` com `webEnvironment=RANDOM_PORT/DEFINED_PORT`) ou qualquer coisa que abra um NIO Selector. Testado e confirmado com múltiplos `SelectorProvider` (`WEPollSelectorProvider` e `WindowsSelectorProvider`) — não é um problema do código do projeto, é uma restrição do ambiente/host onde os comandos são executados nesta sessão (Node.js/loopback TCP simples funciona normalmente; é especificamente `Pipe`/`Selector` do Java que falha). Fora deste sandbox (no terminal normal do usuário), isso não deve ocorrer.

Por causa disso, dentro desta sessão:
- Testes de service com Mockito puro (sem contexto Spring) funcionam normalmente.
- `@SpringBootTest(webEnvironment = WebEnvironment.NONE)` e `@DataJpaTest` funcionam (não sobem servidor web).
- `@WebMvcTest` com `MockMvc` funciona (não abre socket real).
- `spring-boot:run`, `@SpringBootTest(webEnvironment = RANDOM_PORT)` e testes de integração via HTTP real **não funcionam nesta sessão** — a verificação desses pontos fica documentada como "não verificável neste ambiente" e deve ser refeita pelo usuário rodando localmente fora do sandbox.

## Definition of Done (por funcionalidade)

- [ ] Migration Flyway criada e aplicada com sucesso
- [ ] Endpoints documentados no Swagger (`/swagger-ui.html`)
- [ ] Regras de negocio cobertas por testes unitarios (services)
- [ ] Tela responsiva (desktop, tablet, mobile)
- [ ] Tema claro e escuro validados
- [ ] Permissoes (RBAC) aplicadas na tela (esconder/desabilitar) e na API (`@PreAuthorize`)
- [ ] Paginacao, filtros e ordenacao funcionando (quando a tela for uma listagem)
- [ ] Auditoria registrando criacao/edicao/exclusao (`audit_log`)
- [ ] Sem erros no console do navegador
- [ ] Build e testes passando (backend `./mvnw verify` e frontend `npm run build` + `npm test`)

## Escopo fora das fases já entregues

Ver `CHANGELOG.md` para o que foi entregue em cada fase e o que fica para depois (integrações externas reais, financeiro, propostas/contratos, dashboards com dados reais, etc.).
