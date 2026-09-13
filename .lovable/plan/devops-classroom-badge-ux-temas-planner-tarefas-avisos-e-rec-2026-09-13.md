# DevOps Classroom — Badge UX, temas, planner, tarefas, avisos e recado individual

Vou continuar no sistema que você enviou (backend Java/Spring + site React + Docker Compose). O código do zip será trazido para dentro deste projeto para eu editar, e você continua rodando com o `docker-compose` de sempre. A pré-visualização aqui no Lovable não vai exibir o sistema, porque ele depende do servidor Java e do banco.

## 1. Badge flutuante "UX"

Um botão redondo fixo no canto inferior direito, presente em todas as telas. Ao clicar, abre um painel de personalização com:

- **Cor base** — seletor de cores (roda de cor + campo hex) e atalhos prontos. O padrão continua o verde/ciano fluorescente atual. A cor escolhida recalcula automaticamente brilhos, bordas acesas e destaques.
- **Cor de apoio** — segunda cor de destaque (padrão violeta).
- **Tamanho da fonte** — de compacto a grande, escala tudo proporcionalmente.
- **Arredondamento dos cartões**, **intensidade do brilho neon**, **densidade do espaçamento** e **reduzir animações**.
- **Tema**: Escuro (padrão atual) e Claro Holográfico. O tema "Cyber" existente continua disponível.
- Botão **Restaurar padrão**.

Tudo é salvo no navegador do usuário e reaplicado ao entrar. O painel só ajusta as variáveis de estilo já existentes, então nenhuma tela precisa ser reescrita.

O tema claro holográfico existente será revisado para ficar legível em todas as telas (chat, diário, painéis, formulários, calendário novo).

## 2. Planner de aulas (professor)

Nova página **Planner** com calendário mensal:

- Visão de mês com as aulas nos dias; navegação entre meses e atalho "hoje".
- Clicar num dia abre o formulário para agendar: título, descrição, curso, turma, horário e duração.
- Clicar numa aula existente permite editar, remarcar ou excluir.
- Cores por situação: agendada, em andamento, finalizada, cancelada.
- Tudo é gravado na tabela de aulas já existente — as aulas planejadas aparecem normalmente no restante do sistema e podem ser iniciadas na hora.
- Alunos não veem a edição; para eles o calendário é somente leitura das aulas da própria turma.

## 3. Tarefas em grupo do aluno

- Nova página **Minhas tarefas** para o aluno: lista as atividades em grupo em que ele é participante, com assunto, prazo, equipe e colegas.
- Cada tarefa da lista tem uma caixa de marcação; ao marcar, fica registrada como concluída no banco, com quem concluiu e quando.
- Barra de progresso da equipe e do trabalho.
- O professor vê o mesmo progresso na tela de atividades em grupo, por equipe e por aluno.

## 4. Quadro de avisos no dashboard

- Bloco de avisos no topo do painel principal, visível para todos.
- Cada aviso tem título, texto, prioridade (informativo / importante / urgente), opção de fixar no topo e data de validade.
- Criar, editar, fixar e excluir é exclusivo do professor/administrador — tanto na tela quanto na verificação do servidor.
- Avisos podem ser gerais ou direcionados a uma turma.

## 5. Recado individual por toast

- Na sala de aula, o professor vê a lista de alunos presentes; ao lado de cada nome, um botão de "enviar recado".
- Abre um campo curto de texto (e um tom: informativo, elogio, atenção) e envia só para aquele aluno.
- O aluno recebe imediatamente, em tempo real, um toast na tela — ninguém mais vê.
- Se o aluno estiver offline no momento, o recado se perde por natureza (é um aviso de momento). Se você quiser histórico, posso guardar depois.

## Detalhes técnicos

**Preparação:** copiar `backend/`, `frontend/`, `deploy/` e os arquivos de compose do zip para a raiz do projeto (sem nenhum `.git`). O template em branco do Lovable (`src/routes`, etc.) fica intacto ou é removido conforme necessário; o app real é o do zip.

**Frontend** (`frontend/src`):
- `contexts/UxContext.tsx` — estado de tema/cor/fonte, persistência em `localStorage`, aplicação via `document.documentElement.style.setProperty` das variáveis já existentes (`--neon`, `--neon-2`, `--radius-card`, `--glow-strength`, escala base de fonte, densidade).
- `components/UxBadge.tsx` — badge flutuante + painel (absorve o `ThemeSwitcher` atual).
- `index.css` — novas variáveis derivadas (`--ui-scale`, `--space-scale`), revisão do bloco `[data-theme='holo']`, `@media (prefers-reduced-motion)` e modo "reduzir animações".
- `components/LessonPlanner.tsx` — calendário construído com `date-fns` (já é dependência), sem novas libs.
- `components/MyTasks.tsx` — tarefas do aluno.
- `components/NoticeBoard.tsx` — quadro de avisos (leitura + edição condicionada a `user.tipo`).
- `components/DirectToast.tsx` / ajustes em `ChatRoom.tsx` — envio do recado pelo professor e recepção via `react-hot-toast`.
- `services/websocket.ts` — assinar `/user/queue/avisos` e método `sendDirectNotice`.
- `App.tsx` — novas rotas/páginas no menu lateral, com itens filtrados por perfil.

**Backend** (`com.devopsclassroom`):
- `entity/Aviso.java` + `repository/AvisoRepository.java` + `controller/AvisoController.java` (`GET /api/avisos`, `POST/PUT/DELETE` restritos a PROFESSOR/ADMIN).
- `entity/ConclusaoTarefa.java` (trabalho, equipe, índice da tarefa, aluno, data) + repositório + endpoints em `AtividadeGrupoController`: `GET /api/atividades-grupo/minhas`, `POST .../{trabalhoId}/tarefas/{indice}/concluir`, `DELETE` para desmarcar, com verificação de que o aluno pertence ao trabalho/equipe.
- `AulaController` / `AulaService`: `PUT /api/aulas/{id}`, `DELETE /api/aulas/{id}`, `GET /api/aulas/agenda?inicio=&fim=` (professor vê tudo; aluno vê só as turmas em que está matriculado).
- `MensagemController`/novo `AvisoDiretoController` com `@MessageMapping("/aviso-direto/{aulaId}")` usando `SimpMessagingTemplate.convertAndSendToUser(login, "/queue/avisos", payload)`; `/user` já está configurado em `WebSocketConfig`. Valida que o remetente é professor e que o destinatário está presente na aula.
- Tabelas novas são criadas automaticamente (`ddl-auto: update`).

**Verificação:** compilar o backend com Maven e o frontend com `tsc && vite build` dentro do sandbox para garantir que tudo compila antes de entregar.
