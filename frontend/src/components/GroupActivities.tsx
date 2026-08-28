import React, { useCallback, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import api from '../services/api';
import {
  Users,
  Shuffle,
  Plus,
  Trash2,
  CalendarClock,
  ListChecks,
  Target,
  Loader2,
  X,
} from 'lucide-react';

type Aluno = { id: number; nome: string };
type Grupo = { id: number; nome: string; integrantes: Aluno[] };
type Atividade = {
  id: number;
  titulo: string;
  assunto?: string | null;
  descricao?: string | null;
  pontuacaoMaxima?: number;
  prazoEntrega?: string | null;
  turmaId?: number | null;
  turmaNome?: string | null;
  professorNome?: string | null;
  tarefas: string[];
  participantes: Aluno[];
  grupos: Grupo[];
};
type Turma = { id: number; nome?: string; codigo?: string };

interface Props {
  isProfessor: boolean;
}

const formatarPrazo = (valor?: string | null) => {
  if (!valor) return 'Sem prazo definido';
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) return valor;
  return data.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
};

const GroupActivities: React.FC<Props> = ({ isProfessor }) => {
  const [atividades, setAtividades] = useState<Atividade[]>([]);
  const [turmas, setTurmas] = useState<Turma[]>([]);
  const [alunos, setAlunos] = useState<Aluno[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [sorteando, setSorteando] = useState<number | null>(null);
  const [mostrarForm, setMostrarForm] = useState(false);
  const [tamanhoGrupo, setTamanhoGrupo] = useState(3);

  const [form, setForm] = useState({
    titulo: '',
    assunto: '',
    descricao: '',
    prazoEntrega: '',
    pontuacaoMaxima: 100,
    turmaId: '',
    tarefas: [''] as string[],
    participantes: [] as number[],
  });

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const resp = await api.get('/api/atividades-grupo');
      setAtividades(resp.data || []);
    } catch {
      toast.error('Não foi possível carregar as atividades em grupo.');
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregar();
    if (isProfessor) {
      api
        .get('/api/admin/turmas')
        .then((r) => setTurmas(r.data || []))
        .catch(() => setTurmas([]));
    }
  }, [carregar, isProfessor]);

  // Alunos matriculados na turma selecionada
  useEffect(() => {
    if (!form.turmaId) {
      setAlunos([]);
      return;
    }
    api
      .get(`/api/atividades-grupo/turmas/${form.turmaId}/alunos`)
      .then((r) => setAlunos(r.data || []))
      .catch(() => setAlunos([]));
    setForm((prev) => ({ ...prev, participantes: [] }));
  }, [form.turmaId]);

  const alternarParticipante = (id: number) => {
    setForm((prev) => ({
      ...prev,
      participantes: prev.participantes.includes(id)
        ? prev.participantes.filter((p) => p !== id)
        : [...prev.participantes, id],
    }));
  };

  const salvar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.turmaId) return toast.error('Selecione a turma.');
    try {
      await api.post('/api/atividades-grupo', {
        titulo: form.titulo,
        assunto: form.assunto,
        descricao: form.descricao,
        pontuacaoMaxima: Number(form.pontuacaoMaxima) || 100,
        turmaId: Number(form.turmaId),
        prazoEntrega: form.prazoEntrega ? `${form.prazoEntrega}:00` : null,
        tarefas: form.tarefas.filter((t) => t.trim()),
        participantes: form.participantes,
      });
      toast.success('Atividade em grupo criada!');
      setMostrarForm(false);
      setForm({
        titulo: '',
        assunto: '',
        descricao: '',
        prazoEntrega: '',
        pontuacaoMaxima: 100,
        turmaId: '',
        tarefas: [''],
        participantes: [],
      });
      carregar();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Erro ao criar atividade.');
    }
  };

  const sortear = async (id: number) => {
    setSorteando(id);
    try {
      const resp = await api.post(`/api/atividades-grupo/${id}/sorteio?tamanhoGrupo=${tamanhoGrupo}`);
      setAtividades((prev) => prev.map((a) => (a.id === id ? resp.data : a)));
      toast.success('Grupos sorteados!');
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Não foi possível sortear os grupos.');
    } finally {
      setSorteando(null);
    }
  };

  const excluir = async (id: number) => {
    if (!confirm('Excluir esta atividade em grupo?')) return;
    try {
      await api.delete(`/api/atividades-grupo/${id}`);
      setAtividades((prev) => prev.filter((a) => a.id !== id));
      toast.success('Atividade excluída.');
    } catch {
      toast.error('Erro ao excluir atividade.');
    }
  };

  return (
    <div className="space-y-6">
      {isProfessor && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs uppercase tracking-wider txt-dim">Trabalho colaborativo</p>
            <h2 className="text-lg font-semibold title-glow">Atividades em grupo</h2>
          </div>
          <button className="btn-primary" onClick={() => setMostrarForm((v) => !v)}>
            {mostrarForm ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
            {mostrarForm ? 'Cancelar' : 'Nova atividade'}
          </button>
        </div>
      )}

      {isProfessor && mostrarForm && (
        <form onSubmit={salvar} className="card card-glow space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="text-xs uppercase tracking-wider txt-dim">Título</label>
              <input
                className="input-field mt-1"
                value={form.titulo}
                onChange={(e) => setForm({ ...form, titulo: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-wider txt-dim">Assunto</label>
              <input
                className="input-field mt-1"
                value={form.assunto}
                onChange={(e) => setForm({ ...form, assunto: e.target.value })}
                placeholder="Ex.: Pipelines CI/CD"
              />
            </div>
          </div>

          <div>
            <label className="text-xs uppercase tracking-wider txt-dim">Descrição</label>
            <textarea
              className="input-field mt-1"
              rows={3}
              value={form.descricao}
              onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="text-xs uppercase tracking-wider txt-dim">Turma</label>
              <select
                className="input-field mt-1"
                value={form.turmaId}
                onChange={(e) => setForm({ ...form, turmaId: e.target.value })}
                required
              >
                <option value="">Selecione</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome || t.codigo}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs uppercase tracking-wider txt-dim">Data de entrega</label>
              <input
                type="datetime-local"
                className="input-field mt-1"
                value={form.prazoEntrega}
                onChange={(e) => setForm({ ...form, prazoEntrega: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-wider txt-dim">Pontuação</label>
              <input
                type="number"
                className="input-field mt-1"
                value={form.pontuacaoMaxima}
                onChange={(e) => setForm({ ...form, pontuacaoMaxima: Number(e.target.value) })}
              />
            </div>
          </div>

          <div>
            <label className="text-xs uppercase tracking-wider txt-dim">Tarefas</label>
            <div className="space-y-2 mt-1">
              {form.tarefas.map((tarefa, idx) => (
                <div key={idx} className="flex gap-2">
                  <input
                    className="input-field"
                    value={tarefa}
                    placeholder={`Tarefa ${idx + 1}`}
                    onChange={(e) => {
                      const tarefas = [...form.tarefas];
                      tarefas[idx] = e.target.value;
                      setForm({ ...form, tarefas });
                    }}
                  />
                  <button
                    type="button"
                    className="btn-ghost px-3"
                    onClick={() => setForm({ ...form, tarefas: form.tarefas.filter((_, i) => i !== idx) })}
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setForm({ ...form, tarefas: [...form.tarefas, ''] })}
              >
                <Plus className="h-4 w-4" /> Adicionar tarefa
              </button>
            </div>
          </div>

          <div>
            <label className="text-xs uppercase tracking-wider txt-dim">
              Participantes {form.participantes.length > 0 && `(${form.participantes.length} selecionados)`}
            </label>
            <div className="mt-2 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 max-h-56 overflow-y-auto">
              {alunos.map((aluno) => (
                <button
                  type="button"
                  key={aluno.id}
                  onClick={() => alternarParticipante(aluno.id)}
                  className={`glass rounded-xl px-3 py-2 text-left text-sm transition-colors ${
                    form.participantes.includes(aluno.id) ? 'border border-cyan-400/50 neon' : 'border border-white/5'
                  }`}
                >
                  {aluno.nome}
                </button>
              ))}
              {alunos.length === 0 && (
                <p className="text-xs txt-faint">Selecione uma turma para listar os alunos matriculados.</p>
              )}
            </div>
          </div>

          <button type="submit" className="btn-primary w-full justify-center">
            <Plus className="h-4 w-4" /> Criar atividade
          </button>
        </form>
      )}

      {isProfessor && atividades.length > 0 && (
        <div className="card flex flex-wrap items-center gap-3">
          <Shuffle className="h-4 w-4 neon-violet" />
          <span className="text-sm txt-dim">Alunos por grupo no sorteio:</span>
          <input
            type="number"
            min={2}
            className="input-field w-24"
            value={tamanhoGrupo}
            onChange={(e) => setTamanhoGrupo(Number(e.target.value))}
          />
        </div>
      )}

      {carregando ? (
        <div className="card flex items-center gap-2 txt-dim">
          <Loader2 className="h-4 w-4 animate-spin" /> Carregando atividades...
        </div>
      ) : (
        <section className="grid grid-cols-1 xl:grid-cols-2 gap-4">
          {atividades.map((atividade) => (
            <article key={atividade.id} className="card card-glow flex flex-col gap-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="rounded-xl glass p-3">
                    <Users className="h-5 w-5 neon-violet" />
                  </div>
                  <div>
                    <h3 className="font-semibold">{atividade.titulo}</h3>
                    <p className="text-xs txt-faint mt-1">
                      {atividade.assunto || 'Sem assunto'} · {atividade.turmaNome || 'Turma'}
                    </p>
                  </div>
                </div>
                <span className="badge-neon flex items-center gap-1">
                  <Target className="h-3 w-3" />
                  {atividade.pontuacaoMaxima ?? 100} pts
                </span>
              </div>

              {atividade.descricao && <p className="text-sm txt-dim">{atividade.descricao}</p>}

              <div className="flex flex-wrap gap-2">
                <span className="chip-info">
                  <CalendarClock className="h-3.5 w-3.5 neon-amber" />
                  {formatarPrazo(atividade.prazoEntrega)}
                </span>
                <span className="chip-info">
                  <Users className="h-3.5 w-3.5 neon" />
                  {atividade.participantes.length} participantes
                </span>
              </div>

              {atividade.tarefas.length > 0 && (
                <div>
                  <p className="text-xs uppercase tracking-wider txt-dim flex items-center gap-1.5 mb-2">
                    <ListChecks className="h-3.5 w-3.5 neon-lime" /> Tarefas
                  </p>
                  <ul className="space-y-1.5">
                    {atividade.tarefas.map((tarefa, idx) => (
                      <li key={idx} className="text-sm txt-dim flex gap-2">
                        <span className="neon">{String(idx + 1).padStart(2, '0')}</span>
                        {tarefa}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              <div>
                <p className="text-xs uppercase tracking-wider txt-dim mb-2">Grupos sorteados</p>
                {atividade.grupos.length === 0 ? (
                  <p className="text-xs txt-faint">Nenhum sorteio realizado ainda.</p>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {atividade.grupos.map((grupo) => (
                      <div key={grupo.id} className="glass rounded-xl p-3">
                        <p className="text-sm font-semibold neon">{grupo.nome}</p>
                        <ul className="mt-2 space-y-1">
                          {grupo.integrantes.map((i) => (
                            <li key={i.id} className="text-xs txt-dim">
                              {i.nome}
                            </li>
                          ))}
                        </ul>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {isProfessor && (
                <div className="flex gap-2 border-t divider pt-3">
                  <button
                    className="btn-primary flex-1 justify-center"
                    disabled={sorteando === atividade.id}
                    onClick={() => sortear(atividade.id)}
                  >
                    {sorteando === atividade.id ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Shuffle className="h-4 w-4" />
                    )}
                    Sortear grupos
                  </button>
                  <button className="btn-ghost px-3" onClick={() => excluir(atividade.id)}>
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              )}
            </article>
          ))}
          {atividades.length === 0 && <p className="card txt-dim">Nenhuma atividade em grupo cadastrada.</p>}
        </section>
      )}
    </div>
  );
};

export default GroupActivities;
