# Prime CRM

> Gestão de relacionamento inteligente, do primeiro contato ao fechamento.

CRM corporativo, configurável e escalável, construído com **Angular + Spring Boot + PostgreSQL + PrimeNG**. Monorepo com backend e frontend no mesmo repositório.

Estado atual: **Fase 0 (Fundação) + Fase 1 (Parametrização e RBAC)** concluídas. Veja [CHANGELOG.md](CHANGELOG.md) para o detalhe do que foi entregue e [Roadmap](#roadmap) para o que vem a seguir.

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot 3.5.16, Spring Security 6 + JWT, Spring Data JPA, Flyway, MapStruct, springdoc-openapi |
| Frontend | Angular 20 (standalone), PrimeNG 20 + PrimeFlex + PrimeIcons, `@ngrx/signals`, `@ngx-translate` |
| Banco | PostgreSQL 16+ |
| Infra | Docker Compose (opcional), GitHub Actions (CI) |

## Pré-requisitos

- **JDK 21**
- **Node.js 22+** (o projeto usa Angular 20 via `npx`, não é necessário instalar o Angular CLI globalmente)
- **PostgreSQL 16+** rodando em `localhost:5432`
- Maven **não** é necessário — o projeto usa Maven Wrapper (`mvnw` / `mvnw.cmd`)
- Docker é opcional (só para rodar tudo containerizado)

## 1. Banco de dados

Crie o banco (usuário `postgres`, senha `1234` — configurável, ver `.env.example`):

```bash
psql -U postgres -c "CREATE DATABASE primecrm;"
```

No Windows, se `psql` não estiver no PATH, ele fica em `C:\Program Files\PostgreSQL\<versão>\bin\psql.exe`.

As migrations Flyway rodam automaticamente quando o backend sobe — não é preciso rodar nada manualmente.

## 2. Rodar o backend

```bash
cd backend
./mvnw clean install -DskipTests
./mvnw -pl api -am spring-boot:run
```

No Windows (PowerShell/cmd, sem Git Bash), troque `./mvnw` por `mvnw.cmd`.

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Variáveis de ambiente (todas com default para dev local, ver `.env.example`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MINUTES`, `JWT_REFRESH_EXPIRATION_DAYS`, `CORS_ALLOWED_ORIGINS`, `SERVER_PORT`.

## 3. Rodar o frontend

Em outro terminal:

```bash
cd frontend
npm install
npm start
```

Aplicação em `http://localhost:4200`, já apontando para a API em `http://localhost:8080/api/v1` (configurável em `src/environments/`).

## 4. Entrar no sistema

| Campo | Valor |
|---|---|
| Login | `admin` (ou o e-mail `admin@primecrm.local`) |
| Senha | `Admin@123` |

Esse usuário é criado pela migration de seed com o perfil **Administrador**, que já tem todas as permissões. Troque a senha antes de usar em qualquer ambiente que não seja local.

## Testes

```bash
cd backend && ./mvnw verify     # 93 testes (services + controllers)
cd frontend && npm test         # testes de componentes, stores e guards
cd frontend && npm run build    # build de producao
```

Os testes do backend rodam sem Docker. Caso venham a existir testes com Testcontainers em fases futuras, esses exigirão Docker Desktop instalado.

## Rodar tudo via Docker Compose (opcional)

```bash
docker compose up --build
```

Sobe PostgreSQL + backend + frontend (Nginx na porta 4200, API na 8080). Para o dia a dia de desenvolvimento, prefira rodar backend e frontend localmente (hot reload).

## O que dá para testar hoje

Depois de logar com o usuário admin:

- **Layout**: sidebar recolhível, tema claro/escuro (menu do usuário), troca de idioma entre pt-BR, inglês e espanhol em runtime, responsivo em desktop, tablet e mobile.
- **Cadastros Gerais** (`Configurações → Cadastros Gerais`): 16 cadastros parametrizáveis (tipos de cliente/pessoa/empresa, segmentos, ramos de atividade, origens do lead, motivos de perda/ganho, status, prioridades, tipos de tarefa, categorias, tags, equipes, cargos, departamentos) — todos com CRUD, cor, ícone, ordenação por drag and drop e ativar/inativar.
- **Pipelines**: funis e suas etapas, com probabilidade padrão, SLA em dias, cor e reordenação.
- **Campos Personalizados**, **Templates**, **Configurações Gerais** e **Feriados**.
- **Usuários, Perfis e Permissões**: criar usuários, atribuir perfis, vincular permissões por módulo, ativar/inativar/bloquear, redefinir senha. Crie um usuário com um perfil restrito e faça login com ele para ver os menus e botões sumirem conforme as permissões.
- **Auditoria**: toda criação, edição e exclusão grava em `audit_log`. Para conferir: `psql -U postgres -d primecrm -c "SELECT entity_name, action, user_email, created_at FROM audit_log ORDER BY created_at DESC LIMIT 10;"`
- **API**: todos os endpoints documentados e testáveis pelo Swagger UI.

## Estrutura e convenções

Ver [CLAUDE.md](CLAUDE.md) — estrutura de pastas, convenções de código, comandos e o checklist de Definition of Done usado no projeto.

## Solução de problemas

**`Unable to establish loopback connection` ao subir o backend** — erro do JDK ao criar um `Selector` de rede (o Tomcat embarcado precisa disso). Acontece em ambientes com a pilha de rede restrita/virtualizada. Se ocorrer na sua máquina, verifique VPN, adaptadores virtuais (Hyper-V/WSL) ou antivírus interferindo no loopback. O Flyway e o JPA sobem normalmente antes desse ponto, então o erro é isolado ao servidor web.

**Porta 4200 ou 8080 em uso** — rode o frontend em outra porta com `npm start -- --port 4300`, ou o backend com `SERVER_PORT=8081`.

**CORS bloqueando o frontend** — ajuste `CORS_ALLOWED_ORIGINS` no backend para a origem em que o frontend está rodando.

## Roadmap

- [x] **Fase 0** — Fundação: monorepo, autenticação JWT, layout base, CI
- [x] **Fase 1** — Parametrização (engine genérico + módulos dedicados) e RBAC (Usuários/Perfis/Permissões)
- [ ] **Fase 2** — Núcleo Comercial: Clientes, Empresas, Contatos, Leads, Funil/Kanban de Oportunidades
- [ ] **Fase 3** — Produtividade: Tarefas, Agenda/Calendário, Notificações em tempo real
- [ ] **Fase 4** — Comercial Avançado: Produtos/Serviços, Propostas, Pedidos, Contratos
- [ ] **Fase 5** — Financeiro e Documentos
- [ ] **Fase 6** — Dashboards e Relatórios com dados reais
- [ ] **Fase 7** — Qualidade e Hardening: auditoria avançada, performance, segurança

Itens fora de escopo até segunda ordem (dependem de credenciais/integrações externas ou de módulos futuros): SMTP real, WhatsApp Business API, Google Calendar/Contacts, Zapier, Meta/Google Ads, Webhooks, Chaves de API, notificações multicanal, backup automatizado, metas comerciais, multi-moeda.

## Licença

Uso interno / privado.
