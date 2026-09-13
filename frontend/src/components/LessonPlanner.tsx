import React, { useEffect, useMemo, useState } from 'react';
import {
  addMonths,
  endOfMonth,
  endOfWeek,
  format,
  isSameDay,
  isSameMonth,
  startOfMonth,
  startOfWeek,
} from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { ChevronLeft, ChevronRight, Plus, X, Loader2, Trash2, CalendarDays } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../services/api';

interface AulaAgenda {
  id: number;
  titulo: string;
  descricao: string | null;
  dataAula: string | null;
  duracao: string | null;
  status: 'AGENDADA' | 'EM_ANDAMENTO' | 'FINALIZADA' | 'CANCELADA';
  cursoId: number | null;
  turmaId: number | null;
}

interface Curso { id: number; nome: string; codigo?: string }
interface Turma { id: number; nome: string | null; codigo: string }

interface Props {
  isProfessor: boolean;
  onAbrirAula?: (aulaId: number) => void;
}

const SEMANA = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];

const formVazio = {
  id: 0,
  titulo: '',
  descricao: '',
  cursoId: '' as string,
  turmaId: '' as string,
  data: '',
  hora: '19:00',
  duracao: '2h00',
  status: 'AGENDADA',
};

const LessonPlanner: React.FC<Props> = ({ isProfessor, onAbrirAula }) => {
  const [mes, setMes] = useState(() => startOfMonth(new Date()));
  const [aulas, setAulas] = useState<AulaAgenda[]>([]);
  const [cursos, setCursos] = useState<Curso[]>([]);
  const [turmas, setTurmas] = useState<Turma[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [form, setForm] = useState<typeof formVazio | null>(null);
  const [salvando, setSalvando] = useState(false);

  const inicio = useMemo(() => startOfWeek(startOfMonth(mes), { weekStartsOn: 0 }), [mes]);
  const fim = useMemo(() => endOfWeek(endOfMonth(mes), { weekStartsOn: 0 }), [mes]);

  const dias = useMemo(() => {
    const lista: Date[] = [];
    const cursor = new Date(inicio);
    while (cursor <= fim) {
      lista.push(new Date(cursor));
      cursor.setDate(cursor.getDate() + 1);
    }
    return lista;
  }, [inicio, fim]);

  const carregarAgenda = async () => {
    setCarregando(true);
    try {
      const resp = await api.get('/api/aulas/agenda', {
        params: { inicio: format(inicio, 'yyyy-MM-dd'), fim: format(fim, 'yyyy-MM-dd') },
      });
      setAulas(resp.data || []);
    } catch {
      toast.error('Não foi possível carregar a agenda');
      setAulas([]);
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    carregarAgenda();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inicio.getTime(), fim.getTime()]);

  useEffect(() => {
    if (!isProfessor) return;
    api.get('/api/cursos').then((r) => setCursos(r.data || [])).catch(() => setCursos([]));
  }, [isProfessor]);

  useEffect(() => {
    const cursoId = form?.cursoId;
    if (!cursoId) {
      setTurmas([]);
      return;
    }
    api
      .get(`/api/academico/cursos/${cursoId}/turmas`)
      .then((r) => setTurmas(r.data || []))
      .catch(() => setTurmas([]));
  }, [form?.cursoId]);

  const aulasDoDia = (dia: Date) =>
    aulas.filter((a) => a.dataAula && isSameDay(new Date(a.dataAula), dia));

  const abrirNova = (dia: Date) => {
    if (!isProfessor) return;
    setForm({
      ...formVazio,
      cursoId: cursos[0] ? String(cursos[0].id) : '',
      data: format(dia, 'yyyy-MM-dd'),
    });
  };

  const abrirEdicao = (aula: AulaAgenda) => {
    if (!isProfessor) {
      if (aula.status === 'EM_ANDAMENTO' && onAbrirAula) onAbrirAula(aula.id);
      return;
    }
    const data = aula.dataAula ? new Date(aula.dataAula) : new Date();
    setForm({
      id: aula.id,
      titulo: aula.titulo,
      descricao: aula.descricao || '',
      cursoId: aula.cursoId ? String(aula.cursoId) : '',
      turmaId: aula.turmaId ? String(aula.turmaId) : '',
      data: format(data, 'yyyy-MM-dd'),
      hora: format(data, 'HH:mm'),
      duracao: aula.duracao || '2h00',
      status: aula.status,
    });
  };

  const salvar = async () => {
    if (!form) return;
    if (!form.titulo.trim()) return toast.error('Informe o título da aula');
    if (!form.cursoId) return toast.error('Selecione o curso');
    if (!form.data) return toast.error('Informe a data');

    setSalvando(true);
    try {
      const payload = {
        titulo: form.titulo.trim(),
        descricao: form.descricao.trim(),
        cursoId: Number(form.cursoId),
        turmaId: form.turmaId ? Number(form.turmaId) : null,
        dataAula: `${form.data}T${form.hora || '00:00'}:00`,
        duracao: form.duracao,
        status: form.status,
      };
      if (form.id) await api.put(`/api/aulas/${form.id}`, payload);
      else await api.post('/api/aulas', payload);
      toast.success(form.id ? 'Aula atualizada' : 'Aula agendada');
      setForm(null);
      await carregarAgenda();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Não foi possível salvar a aula');
    } finally {
      setSalvando(false);
    }
  };

  const excluir = async () => {
    if (!form?.id) return;
    if (!window.confirm('Excluir esta aula do planejamento?')) return;
    try {
      await api.delete(`/api/aulas/${form.id}`);
      toast.success('Aula excluída');
      setForm(null);
      await carregarAgenda();
    } catch {
      toast.error('Não foi possível excluir a aula');
    }
  };

  const classeEvento = (status: AulaAgenda['status']) =>
    status === 'EM_ANDAMENTO'
      ? 'cal-event cal-event-andamento'
      : status === 'FINALIZADA'
        ? 'cal-event cal-event-finalizada'
        : status === 'CANCELADA'
          ? 'cal-event cal-event-cancelada'
          : 'cal-event';

  return (
    <section className="card">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div className="flex items-center gap-2">
          <CalendarDays className="w-5 h-5 neon" />
          <h2 className="font-semibold capitalize">{format(mes, "MMMM 'de' yyyy", { locale: ptBR })}</h2>
          {carregando && <Loader2 className="w-4 h-4 animate-spin txt-dim" />}
        </div>
        <div className="flex items-center gap-2">
          <button className="icon-btn" onClick={() => setMes(addMonths(mes, -1))} aria-label="Mês anterior">
            <ChevronLeft className="w-4 h-4" />
          </button>
          <button className="btn-secondary" onClick={() => setMes(startOfMonth(new Date()))}>Hoje</button>
          <button className="icon-btn" onClick={() => setMes(addMonths(mes, 1))} aria-label="Próximo mês">
            <ChevronRight className="w-4 h-4" />
          </button>
          {isProfessor && (
            <button className="btn-primary" onClick={() => abrirNova(new Date())}>
              <Plus className="w-4 h-4" />
              Nova aula
            </button>
          )}
        </div>
      </div>

      <div className="cal-grid mb-1">
        {SEMANA.map((d) => (
          <div key={d} className="cal-weekday">{d}</div>
        ))}
      </div>

      <div className="cal-grid">
        {dias.map((dia) => {
          const doMes = isSameMonth(dia, mes);
          const hoje = isSameDay(dia, new Date());
          const doDia = aulasDoDia(dia);
          return (
            <div
              key={dia.toISOString()}
              className={`cal-day ${doMes ? '' : 'cal-day-out'} ${hoje ? 'cal-day-today' : ''}`}
              onDoubleClick={() => abrirNova(dia)}
            >
              <div className="flex items-center justify-between">
                <span className="cal-day-num">{format(dia, 'd')}</span>
                {isProfessor && (
                  <button
                    className="icon-btn !p-1"
                    title="Agendar aula neste dia"
                    aria-label={`Agendar aula em ${format(dia, 'dd/MM/yyyy')}`}
                    onClick={() => abrirNova(dia)}
                  >
                    <Plus className="w-3 h-3" />
                  </button>
                )}
              </div>
              {doDia.map((a) => (
                <button
                  key={a.id}
                  type="button"
                  className={classeEvento(a.status)}
                  title={`${a.titulo}${a.dataAula ? ` · ${format(new Date(a.dataAula), 'HH:mm')}` : ''}`}
                  onClick={() => abrirEdicao(a)}
                >
                  {a.dataAula ? `${format(new Date(a.dataAula), 'HH:mm')} ` : ''}
                  {a.titulo}
                </button>
              ))}
            </div>
          );
        })}
      </div>

      {!isProfessor && (
        <p className="text-xs txt-faint mt-3">
          Calendário das aulas da sua turma. Aulas em andamento podem ser abertas com um clique.
        </p>
      )}

      {form && isProfessor && (
        <div className="modal-backdrop" onClick={() => setForm(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold">{form.id ? 'Editar aula' : 'Agendar aula'}</h3>
              <button className="icon-btn" onClick={() => setForm(null)} aria-label="Fechar">
                <X className="w-4 h-4" />
              </button>
            </div>

            <label className="ux-label" htmlFor="aula-titulo">Título</label>
            <input
              id="aula-titulo"
              className="input-field w-full mb-3"
              value={form.titulo}
              onChange={(e) => setForm({ ...form, titulo: e.target.value })}
            />

            <label className="ux-label" htmlFor="aula-desc">Planejamento / conteúdo previsto</label>
            <textarea
              id="aula-desc"
              rows={4}
              className="input-field w-full mb-3"
              value={form.descricao}
              onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
              <div>
                <label className="ux-label" htmlFor="aula-curso">Curso</label>
                <select
                  id="aula-curso"
                  className="input-field w-full"
                  value={form.cursoId}
                  onChange={(e) => setForm({ ...form, cursoId: e.target.value, turmaId: '' })}
                >
                  <option value="">Selecione…</option>
                  {cursos.map((c) => (
                    <option key={c.id} value={c.id}>{c.nome}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="ux-label" htmlFor="aula-turma">Turma (opcional)</label>
                <select
                  id="aula-turma"
                  className="input-field w-full"
                  value={form.turmaId}
                  onChange={(e) => setForm({ ...form, turmaId: e.target.value })}
                >
                  <option value="">Todas</option>
                  {turmas.map((t) => (
                    <option key={t.id} value={t.id}>{t.nome || t.codigo}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="ux-label" htmlFor="aula-data">Data</label>
                <input
                  id="aula-data"
                  type="date"
                  className="input-field w-full"
                  value={form.data}
                  onChange={(e) => setForm({ ...form, data: e.target.value })}
                />
              </div>
              <div>
                <label className="ux-label" htmlFor="aula-hora">Horário</label>
                <input
                  id="aula-hora"
                  type="time"
                  className="input-field w-full"
                  value={form.hora}
                  onChange={(e) => setForm({ ...form, hora: e.target.value })}
                />
              </div>
              <div>
                <label className="ux-label" htmlFor="aula-duracao">Duração</label>
                <input
                  id="aula-duracao"
                  className="input-field w-full"
                  placeholder="2h00"
                  value={form.duracao}
                  onChange={(e) => setForm({ ...form, duracao: e.target.value })}
                />
              </div>
              <div>
                <label className="ux-label" htmlFor="aula-status">Situação</label>
                <select
                  id="aula-status"
                  className="input-field w-full"
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                >
                  <option value="AGENDADA">Agendada</option>
                  <option value="EM_ANDAMENTO">Em andamento</option>
                  <option value="FINALIZADA">Finalizada</option>
                  <option value="CANCELADA">Cancelada</option>
                </select>
              </div>
            </div>

            <div className="flex justify-between gap-2">
              {form.id ? (
                <button className="btn-danger" onClick={excluir}>
                  <Trash2 className="w-4 h-4" />
                  Excluir
                </button>
              ) : <span />}
              <div className="flex gap-2">
                <button className="btn-secondary" onClick={() => setForm(null)}>Cancelar</button>
                <button className="btn-primary" onClick={salvar} disabled={salvando}>
                  {salvando && <Loader2 className="w-4 h-4 animate-spin" />}
                  Salvar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
};

export default LessonPlanner;
