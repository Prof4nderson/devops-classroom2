import React, { useCallback, useEffect, useState } from 'react';
import api from '../services/api';
import toast from 'react-hot-toast';
import {
  BookOpen,
  CalendarCheck,
  ClipboardCheck,
  GraduationCap,
  Loader2,
  PlayCircle,
  Save,
  StopCircle,
  Trash2,
  TrendingUp,
} from 'lucide-react';

interface DiarioProps {
  isProfessor: boolean;
}

type AulaDiario = {
  id: number;
  titulo: string;
  dataAula?: string | null;
  status?: string | null;
  conteudoMinistrado?: string | null;
  observacoes?: string | null;
  iniciadaEm?: string | null;
  finalizadaEm?: string | null;
};

const STATUS_PRESENCA = ['PRESENTE', 'AUSENTE', 'ATRASADO', 'JUSTIFICADO'] as const;

const dataBR = (valor?: string | null) => {
  if (!valor) return '—';
  const d = new Date(valor);
  return isNaN(d.getTime()) ? valor : d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
};

const corDaMedia = (media: number) => (media >= 7 ? 'neon-lime' : media >= 6 ? 'neon-amber' : 'text-red-400');

const Diario: React.FC<DiarioProps> = ({ isProfessor }) => {
  const [loading, setLoading] = useState(true);
  const [turmas, setTurmas] = useState<any[]>([]);
  const [turmaId, setTurmaId] = useState<number | null>(null);
  const [diario, setDiario] = useState<any>(null);
  const [boletim, setBoletim] = useState<any>(null);
  const [aulaSelecionada, setAulaSelecionada] = useState<number | null>(null);
  const [conteudo, setConteudo] = useState({ conteudoMinistrado: '', observacoes: '' });
  const [novaNota, setNovaNota] = useState({ alunoId: '', titulo: '', tipo: 'PROVA', nota: '', peso: '1' });
  const [salvando, setSalvando] = useState(false);

  // ------------------------------------------------------------ carregamento
  const carregarBoletim = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/api/diario/meu');
      setBoletim(data);
    } catch {
      toast.error('Não foi possível carregar seu boletim');
    } finally {
      setLoading(false);
    }
  }, []);

  const carregarDiario = useCallback(async (id: number) => {
    setLoading(true);
    try {
      const { data } = await api.get(`/api/diario/turma/${id}`);
      setDiario(data);
      const emAndamento = (data.aulas || []).find((a: AulaDiario) => a.status === 'EM_ANDAMENTO');
      const alvo = emAndamento || (data.aulas || [])[data.aulas.length - 1];
      setAulaSelecionada(alvo?.id ?? null);
      setConteudo({
        conteudoMinistrado: alvo?.conteudoMinistrado || '',
        observacoes: alvo?.observacoes || '',
      });
    } catch {
      toast.error('Não foi possível carregar o diário da turma');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isProfessor) {
      carregarBoletim();
      return;
    }
    (async () => {
      try {
        const { data } = await api.get('/api/diario/turmas');
        setTurmas(data || []);
        if (data?.length) setTurmaId(data[0].id);
        else setLoading(false);
      } catch {
        toast.error('Não foi possível carregar as turmas');
        setLoading(false);
      }
    })();
  }, [isProfessor, carregarBoletim]);

  useEffect(() => {
    if (isProfessor && turmaId) carregarDiario(turmaId);
  }, [isProfessor, turmaId, carregarDiario]);

  const aulaAtual: AulaDiario | undefined = (diario?.aulas || []).find((a: AulaDiario) => a.id === aulaSelecionada);

  // ------------------------------------------------------------ ações
  const trocarAula = (id: number) => {
    setAulaSelecionada(id);
    const a = (diario?.aulas || []).find((x: AulaDiario) => x.id === id);
    setConteudo({ conteudoMinistrado: a?.conteudoMinistrado || '', observacoes: a?.observacoes || '' });
  };

  const iniciarAula = async () => {
    if (!aulaSelecionada) return;
    try {
      await api.post(`/api/diario/aula/${aulaSelecionada}/iniciar`);
      toast.success('Aula iniciada');
      if (turmaId) carregarDiario(turmaId);
    } catch {
      toast.error('Erro ao iniciar a aula');
    }
  };

  const finalizarAula = async () => {
    if (!aulaSelecionada) return;
    try {
      await api.post(`/api/diario/aula/${aulaSelecionada}/finalizar`, conteudo);
      toast.success('Aula finalizada — faltas consolidadas');
      if (turmaId) carregarDiario(turmaId);
    } catch {
      toast.error('Erro ao finalizar a aula');
    }
  };

  const salvarConteudo = async () => {
    if (!aulaSelecionada) return;
    setSalvando(true);
    try {
      await api.put(`/api/diario/aula/${aulaSelecionada}`, conteudo);
      toast.success('Conteúdo registrado no diário');
      if (turmaId) carregarDiario(turmaId);
    } catch {
      toast.error('Erro ao salvar o conteúdo');
    } finally {
      setSalvando(false);
    }
  };

  const marcarPresenca = async (alunoId: number, status: string) => {
    if (!aulaSelecionada) return;
    try {
      await api.post(`/api/diario/aula/${aulaSelecionada}/presenca`, { alunoId, status });
      setDiario((prev: any) => {
        if (!prev) return prev;
        const chamada = { ...(prev.chamada || {}) };
        chamada[aulaSelecionada] = { ...(chamada[aulaSelecionada] || {}), [alunoId]: status };
        return { ...prev, chamada };
      });
    } catch {
      toast.error('Erro ao registrar presença');
    }
  };

  const chamadaCompleta = async (status: string) => {
    if (!aulaSelecionada) return;
    const registros = (diario?.alunos || []).map((a: any) => ({ alunoId: a.id, status }));
    try {
      await api.post(`/api/diario/aula/${aulaSelecionada}/chamada`, { registros });
      toast.success('Chamada registrada');
      if (turmaId) carregarDiario(turmaId);
    } catch {
      toast.error('Erro na chamada em lote');
    }
  };

  const lancarNota = async () => {
    if (!turmaId || !novaNota.alunoId || !novaNota.nota) {
      toast.error('Selecione o aluno e informe a nota');
      return;
    }
    try {
      await api.post('/api/diario/avaliacoes', {
        alunoId: Number(novaNota.alunoId),
        turmaId,
        aulaId: aulaSelecionada,
        titulo: novaNota.titulo || 'Avaliação',
        tipo: novaNota.tipo,
        nota: Number(novaNota.nota.replace(',', '.')),
        peso: Number(novaNota.peso.replace(',', '.')) || 1,
      });
      toast.success('Nota lançada');
      setNovaNota({ alunoId: '', titulo: '', tipo: 'PROVA', nota: '', peso: '1' });
      carregarDiario(turmaId);
    } catch {
      toast.error('Erro ao lançar a nota');
    }
  };

  const excluirNota = async (id: number) => {
    try {
      await api.delete(`/api/diario/avaliacoes/${id}`);
      if (turmaId) carregarDiario(turmaId);
    } catch {
      toast.error('Erro ao excluir a nota');
    }
  };

  // ------------------------------------------------------------ render
  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-6 h-6 animate-spin neon" />
      </div>
    );
  }

  // ---------- visão do aluno ----------
  if (!isProfessor) {
    const turmasBoletim = boletim?.turmas || [];
    if (!turmasBoletim.length) {
      return <p className="txt-dim">Nenhuma turma com registros no diário ainda.</p>;
    }
    return (
      <div className="space-y-4">
        {turmasBoletim.map((item: any, idx: number) => (
          <article key={idx} className="card card-glow">
            <header className="flex flex-wrap items-center justify-between gap-3 mb-4">
              <div>
                <h3 className="font-semibold title-glow">{item.turma?.nome || item.turma?.codigo}</h3>
                <p className="text-xs txt-dim">
                  {item.turma?.curso} {item.turma?.periodo ? `• ${item.turma.periodo}` : ''}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                <span className="chip-info">
                  <CalendarCheck className="w-3.5 h-3.5 neon" />
                  Frequência {item.frequencia}%
                </span>
                <span className="chip-info">
                  <TrendingUp className={`w-3.5 h-3.5 ${corDaMedia(item.media)}`} />
                  Média {item.media}
                </span>
              </div>
            </header>

            <div className="grid grid-cols-3 gap-3 mb-4 text-center">
              <div className="glass rounded-xl p-3">
                <p className="text-lg font-bold neon">{item.aulasRealizadas}</p>
                <p className="text-xs txt-dim">Aulas</p>
              </div>
              <div className="glass rounded-xl p-3">
                <p className="text-lg font-bold neon-lime">{item.presencas}</p>
                <p className="text-xs txt-dim">Presenças</p>
              </div>
              <div className="glass rounded-xl p-3">
                <p className="text-lg font-bold text-red-400">{item.faltas}</p>
                <p className="text-xs txt-dim">Faltas</p>
              </div>
            </div>

            {item.avaliacoes?.length ? (
              <div className="space-y-1">
                {item.avaliacoes.map((av: any) => (
                  <div key={av.id} className="flex items-center justify-between text-sm px-3 py-2 rounded-xl glass">
                    <span>
                      {av.titulo} <span className="chip-tag ml-1">{av.tipo}</span>
                    </span>
                    <span className={corDaMedia(av.nota)}>
                      {av.nota} <span className="txt-faint text-xs">(peso {av.peso})</span>
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm txt-faint">Nenhuma nota lançada ainda.</p>
            )}
          </article>
        ))}
      </div>
    );
  }

  // ---------- visão do professor ----------
  if (!turmas.length) {
    return <p className="txt-dim">Nenhuma turma vinculada. Cadastre turmas em Administração.</p>;
  }

  const chamadaDaAula: Record<number, string> = (diario?.chamada || {})[aulaSelecionada as number] || {};
  const alunos: any[] = diario?.alunos || [];
  const avaliacoes: any[] = diario?.avaliacoes || [];

  return (
    <div className="space-y-4">
      {/* seleção de turma e aula */}
      <section className="card">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <label className="text-sm txt-dim">
            Turma
            <select
              className="input-field mt-1"
              value={turmaId ?? ''}
              onChange={(e) => setTurmaId(Number(e.target.value))}
            >
              {turmas.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nome || t.codigo} {t.curso ? `— ${t.curso}` : ''}
                </option>
              ))}
            </select>
          </label>

          <label className="text-sm txt-dim">
            Aula
            <select
              className="input-field mt-1"
              value={aulaSelecionada ?? ''}
              onChange={(e) => trocarAula(Number(e.target.value))}
            >
              <option value="">Selecione uma aula</option>
              {(diario?.aulas || []).map((a: AulaDiario) => (
                <option key={a.id} value={a.id}>
                  {a.titulo} — {a.status || 'AGENDADA'}
                </option>
              ))}
            </select>
          </label>
        </div>

        {aulaAtual && (
          <div className="mt-4 space-y-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className="chip-info">Início: {dataBR(aulaAtual.iniciadaEm)}</span>
              <span className="chip-info">Fim: {dataBR(aulaAtual.finalizadaEm)}</span>
              <span className={aulaAtual.status === 'FINALIZADA' ? 'badge-off' : 'badge-ok'}>
                {aulaAtual.status || 'AGENDADA'}
              </span>
              <div className="ml-auto flex gap-2">
                {aulaAtual.status !== 'EM_ANDAMENTO' && aulaAtual.status !== 'FINALIZADA' && (
                  <button onClick={iniciarAula} className="btn-primary text-xs px-3 py-1.5">
                    <PlayCircle className="w-4 h-4" /> Iniciar aula
                  </button>
                )}
                {aulaAtual.status === 'EM_ANDAMENTO' && (
                  <button onClick={finalizarAula} className="btn-secondary text-xs px-3 py-1.5">
                    <StopCircle className="w-4 h-4 neon-amber" /> Finalizar aula
                  </button>
                )}
              </div>
            </div>

            <textarea
              className="input-field"
              rows={3}
              placeholder="Conteúdo ministrado nesta aula..."
              value={conteudo.conteudoMinistrado}
              onChange={(e) => setConteudo({ ...conteudo, conteudoMinistrado: e.target.value })}
            />
            <textarea
              className="input-field"
              rows={2}
              placeholder="Observações"
              value={conteudo.observacoes}
              onChange={(e) => setConteudo({ ...conteudo, observacoes: e.target.value })}
            />
            <div className="flex justify-end">
              <button onClick={salvarConteudo} disabled={salvando} className="btn-primary text-sm">
                {salvando ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                Salvar conteúdo
              </button>
            </div>
          </div>
        )}
      </section>

      {/* chamada */}
      {aulaSelecionada && (
        <section className="card">
          <header className="flex flex-wrap items-center justify-between gap-3 mb-4">
            <h3 className="font-semibold flex items-center gap-2">
              <ClipboardCheck className="w-4 h-4 neon" /> Chamada
            </h3>
            <div className="flex gap-2">
              <button onClick={() => chamadaCompleta('PRESENTE')} className="btn-secondary text-xs px-3 py-1.5">
                Todos presentes
              </button>
              <button onClick={() => chamadaCompleta('AUSENTE')} className="btn-secondary text-xs px-3 py-1.5">
                Todos ausentes
              </button>
            </div>
          </header>

          <div className="space-y-1">
            {alunos.map((aluno) => (
              <div
                key={aluno.id}
                className="flex flex-wrap items-center justify-between gap-2 px-3 py-2 rounded-xl table-row-glass"
              >
                <span className="text-sm">{aluno.nome}</span>
                <div className="flex gap-1">
                  {STATUS_PRESENCA.map((st) => {
                    const ativo = (chamadaDaAula[aluno.id] || 'AUSENTE') === st;
                    return (
                      <button
                        key={st}
                        onClick={() => marcarPresenca(aluno.id, st)}
                        className={`theme-chip ${ativo ? 'theme-chip-active' : ''}`}
                      >
                        {st.charAt(0) + st.slice(1).toLowerCase()}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
            {!alunos.length && <p className="text-sm txt-faint">Nenhum aluno matriculado nesta turma.</p>}
          </div>
        </section>
      )}

      {/* avaliações */}
      <section className="card">
        <h3 className="font-semibold flex items-center gap-2 mb-4">
          <GraduationCap className="w-4 h-4 neon-violet" /> Avaliações
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-6 gap-2 mb-4">
          <select
            className="input-field md:col-span-2"
            value={novaNota.alunoId}
            onChange={(e) => setNovaNota({ ...novaNota, alunoId: e.target.value })}
          >
            <option value="">Aluno</option>
            {alunos.map((a) => (
              <option key={a.id} value={a.id}>
                {a.nome}
              </option>
            ))}
          </select>
          <input
            className="input-field"
            placeholder="Título"
            value={novaNota.titulo}
            onChange={(e) => setNovaNota({ ...novaNota, titulo: e.target.value })}
          />
          <select
            className="input-field"
            value={novaNota.tipo}
            onChange={(e) => setNovaNota({ ...novaNota, tipo: e.target.value })}
          >
            {['PROVA', 'TRABALHO', 'QUIZ', 'PARTICIPACAO', 'PROJETO'].map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
          <input
            className="input-field"
            placeholder="Nota"
            value={novaNota.nota}
            onChange={(e) => setNovaNota({ ...novaNota, nota: e.target.value })}
          />
          <div className="flex gap-2">
            <input
              className="input-field w-20"
              placeholder="Peso"
              value={novaNota.peso}
              onChange={(e) => setNovaNota({ ...novaNota, peso: e.target.value })}
            />
            <button onClick={lancarNota} className="btn-primary text-sm whitespace-nowrap">
              Lançar
            </button>
          </div>
        </div>

        <div className="space-y-1 max-h-64 overflow-y-auto">
          {avaliacoes.map((av) => (
            <div key={av.id} className="flex items-center justify-between text-sm px-3 py-2 rounded-xl table-row-glass">
              <span>
                <strong>{av.alunoNome}</strong> — {av.titulo} <span className="chip-tag ml-1">{av.tipo}</span>
              </span>
              <span className="flex items-center gap-3">
                <span className={corDaMedia(av.nota)}>
                  {av.nota} <span className="txt-faint text-xs">(peso {av.peso})</span>
                </span>
                <button onClick={() => excluirNota(av.id)} className="icon-btn" title="Excluir nota">
                  <Trash2 className="w-4 h-4" />
                </button>
              </span>
            </div>
          ))}
          {!avaliacoes.length && <p className="text-sm txt-faint">Nenhuma avaliação lançada.</p>}
        </div>
      </section>

      {/* consolidado */}
      <section className="card overflow-x-auto">
        <h3 className="font-semibold flex items-center gap-2 mb-4">
          <BookOpen className="w-4 h-4 neon-lime" /> Consolidado da turma
          <span className="chip-tag ml-2">{diario?.totalAulasRealizadas || 0} aula(s) realizada(s)</span>
        </h3>
        <table className="w-full text-sm table-diario">
          <thead>
            <tr className="txt-dim text-left">
              <th className="py-2 pr-4">Aluno</th>
              <th className="py-2 pr-4">Presenças</th>
              <th className="py-2 pr-4">Faltas</th>
              <th className="py-2 pr-4">Frequência</th>
              <th className="py-2 pr-4">Avaliações</th>
              <th className="py-2 pr-4">Média</th>
              <th className="py-2">Situação</th>
            </tr>
          </thead>
          <tbody>
            {(diario?.resumo || []).map((r: any) => (
              <tr key={r.alunoId} className="table-row-glass border-t divider">
                <td className="py-2 pr-4">{r.nome}</td>
                <td className="py-2 pr-4 neon-lime">{r.presencas}</td>
                <td className="py-2 pr-4 text-red-400">{r.faltas}</td>
                <td className="py-2 pr-4">{r.frequencia}%</td>
                <td className="py-2 pr-4">{r.totalAvaliacoes}</td>
                <td className={`py-2 pr-4 font-semibold ${corDaMedia(r.media)}`}>{r.media}</td>
                <td className="py-2">
                  <span className={r.situacao === 'APROVADO' ? 'badge-ok' : 'badge-off'}>{r.situacao}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!(diario?.resumo || []).length && <p className="text-sm txt-faint mt-2">Sem alunos para consolidar.</p>}
      </section>
    </div>
  );
};

export default Diario;
