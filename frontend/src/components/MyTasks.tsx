import React, { useEffect, useState } from 'react';
import { CheckCircle2, Circle, Users, CalendarClock, Loader2, ListChecks } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../services/api';

interface Marca {
  indice: number;
  alunoId: number | null;
  alunoNome: string | null;
  concluidoEm: string | null;
}

interface MinhaAtividade {
  id: number;
  titulo: string;
  assunto: string | null;
  descricao: string | null;
  prazoEntrega: string | null;
  turmaNome: string | null;
  professorNome: string | null;
  tarefas: string[];
  minhaEquipeId: number | null;
  minhaEquipeNome: string | null;
  colegas: { id: number; nome: string }[];
  minhasConcluidas: Marca[];
}

const MyTasks: React.FC = () => {
  const [atividades, setAtividades] = useState<MinhaAtividade[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [ocupado, setOcupado] = useState<string | null>(null);

  const carregar = async () => {
    try {
      const resp = await api.get('/api/atividades-grupo/minhas');
      setAtividades(resp.data || []);
    } catch {
      toast.error('Não foi possível carregar suas tarefas');
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    carregar();
  }, []);

  const alternar = async (atividade: MinhaAtividade, indice: number, concluida: boolean) => {
    const chave = `${atividade.id}-${indice}`;
    setOcupado(chave);
    try {
      if (concluida) {
        await api.delete(`/api/atividades-grupo/${atividade.id}/tarefas/${indice}/concluir`);
      } else {
        await api.post(`/api/atividades-grupo/${atividade.id}/tarefas/${indice}/concluir`);
      }
      const resp = await api.get('/api/atividades-grupo/minhas');
      setAtividades(resp.data || []);
      if (!concluida) toast.success('Tarefa concluída');
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Não foi possível atualizar a tarefa');
    } finally {
      setOcupado(null);
    }
  };

  if (carregando) {
    return (
      <p className="card txt-dim flex items-center gap-2">
        <Loader2 className="w-4 h-4 animate-spin" /> Carregando suas tarefas…
      </p>
    );
  }

  if (atividades.length === 0) {
    return <p className="card txt-dim">Você ainda não faz parte de nenhuma atividade em grupo.</p>;
  }

  return (
    <section className="space-y-4">
      {atividades.map((a) => {
        const concluidas = new Map(a.minhasConcluidas.map((m) => [m.indice, m]));
        const total = a.tarefas.length;
        const feitas = a.tarefas.filter((_, i) => concluidas.has(i)).length;
        const progresso = total > 0 ? Math.round((feitas / total) * 100) : 0;

        return (
          <article key={a.id} className="card">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <h2 className="font-semibold flex items-center gap-2">
                  <ListChecks className="w-4 h-4 neon" />
                  {a.titulo}
                </h2>
                {a.assunto && <p className="text-sm txt-dim mt-1">{a.assunto}</p>}
                <div className="flex flex-wrap items-center gap-2 mt-2">
                  {a.minhaEquipeNome && (
                    <span className="chip-info">
                      <Users className="w-3.5 h-3.5 neon-lime" />
                      {a.minhaEquipeNome}
                    </span>
                  )}
                  {a.turmaNome && <span className="chip-tag">{a.turmaNome}</span>}
                  {a.prazoEntrega && (
                    <span className="chip-info">
                      <CalendarClock className="w-3.5 h-3.5 neon-amber" />
                      Entrega {new Date(a.prazoEntrega).toLocaleString('pt-BR')}
                    </span>
                  )}
                </div>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold">{progresso}%</p>
                <p className="text-xs txt-faint">{feitas} de {total} tarefas</p>
              </div>
            </div>

            <div className="meter my-4">
              <span style={{ width: `${progresso}%` }} />
            </div>

            {a.colegas.length > 0 && (
              <p className="text-xs txt-faint mb-3">
                Com você: {a.colegas.map((c) => c.nome).join(', ')}
              </p>
            )}

            <div className="space-y-2">
              {a.tarefas.length === 0 && <p className="text-sm txt-dim">Nenhuma tarefa cadastrada.</p>}
              {a.tarefas.map((tarefa, i) => {
                const marca = concluidas.get(i);
                const chave = `${a.id}-${i}`;
                return (
                  <button
                    key={i}
                    type="button"
                    className="tarefa-linha w-full text-left"
                    disabled={ocupado === chave}
                    onClick={() => alternar(a, i, !!marca)}
                  >
                    {ocupado === chave ? (
                      <Loader2 className="w-4 h-4 animate-spin shrink-0 mt-0.5" />
                    ) : marca ? (
                      <CheckCircle2 className="w-4 h-4 neon-lime shrink-0 mt-0.5" />
                    ) : (
                      <Circle className="w-4 h-4 txt-faint shrink-0 mt-0.5" />
                    )}
                    <span className="min-w-0">
                      <span className={`text-sm block ${marca ? 'tarefa-feita' : ''}`}>{tarefa}</span>
                      {marca && (
                        <span className="text-xs txt-faint">
                          concluída por {marca.alunoNome}
                          {marca.concluidoEm ? ` em ${new Date(marca.concluidoEm).toLocaleString('pt-BR')}` : ''}
                        </span>
                      )}
                    </span>
                  </button>
                );
              })}
            </div>
          </article>
        );
      })}
    </section>
  );
};

export default MyTasks;
