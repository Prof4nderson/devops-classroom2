import React, { useEffect, useState } from 'react';
import { Megaphone, Pin, Plus, Pencil, Trash2, X, AlertTriangle, Info, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../services/api';

export interface Aviso {
  id: number;
  titulo: string;
  conteudo: string;
  prioridade: 'INFORMATIVO' | 'IMPORTANTE' | 'URGENTE';
  fixado: boolean;
  validoAte: string | null;
  turmaId: number | null;
  turmaNome: string | null;
  autorNome: string | null;
  criadoEm: string | null;
}

interface Props {
  isProfessor: boolean;
}

const VAZIO = {
  id: 0,
  titulo: '',
  conteudo: '',
  prioridade: 'INFORMATIVO' as Aviso['prioridade'],
  fixado: false,
  validoAte: '',
};

const NoticeBoard: React.FC<Props> = ({ isProfessor }) => {
  const [avisos, setAvisos] = useState<Aviso[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [form, setForm] = useState<typeof VAZIO | null>(null);
  const [salvando, setSalvando] = useState(false);

  const carregar = async () => {
    try {
      const resp = await api.get('/api/avisos');
      setAvisos(resp.data || []);
    } catch {
      setAvisos([]);
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    carregar();
  }, []);

  const abrirNovo = () => setForm({ ...VAZIO });

  const abrirEdicao = (a: Aviso) =>
    setForm({
      id: a.id,
      titulo: a.titulo,
      conteudo: a.conteudo,
      prioridade: a.prioridade,
      fixado: a.fixado,
      validoAte: a.validoAte ? a.validoAte.slice(0, 16) : '',
    });

  const salvar = async () => {
    if (!form) return;
    if (!form.titulo.trim() || !form.conteudo.trim()) {
      toast.error('Preencha título e texto do aviso');
      return;
    }
    setSalvando(true);
    try {
      const payload = {
        titulo: form.titulo.trim(),
        conteudo: form.conteudo.trim(),
        prioridade: form.prioridade,
        fixado: form.fixado,
        validoAte: form.validoAte || null,
      };
      if (form.id) await api.put(`/api/avisos/${form.id}`, payload);
      else await api.post('/api/avisos', payload);
      toast.success(form.id ? 'Aviso atualizado' : 'Aviso publicado');
      setForm(null);
      await carregar();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Não foi possível salvar o aviso');
    } finally {
      setSalvando(false);
    }
  };

  const excluir = async (a: Aviso) => {
    if (!window.confirm(`Excluir o aviso "${a.titulo}"?`)) return;
    try {
      await api.delete(`/api/avisos/${a.id}`);
      setAvisos((lista) => lista.filter((x) => x.id !== a.id));
      toast.success('Aviso excluído');
    } catch {
      toast.error('Não foi possível excluir');
    }
  };

  const classe = (p: Aviso['prioridade']) =>
    p === 'URGENTE' ? 'aviso-item aviso-urgente' : p === 'IMPORTANTE' ? 'aviso-item aviso-importante' : 'aviso-item';

  return (
    <section className="card mb-5">
      <div className="flex items-center justify-between gap-3 mb-4">
        <div className="flex items-center gap-2">
          <Megaphone className="w-5 h-5 neon" />
          <h2 className="font-semibold">Quadro de avisos</h2>
        </div>
        {isProfessor && (
          <button className="btn-primary" onClick={abrirNovo}>
            <Plus className="w-4 h-4" />
            Novo aviso
          </button>
        )}
      </div>

      {carregando && (
        <p className="text-sm txt-dim flex items-center gap-2">
          <Loader2 className="w-4 h-4 animate-spin" /> Carregando avisos…
        </p>
      )}

      {!carregando && avisos.length === 0 && (
        <p className="text-sm txt-dim">Nenhum aviso publicado no momento.</p>
      )}

      <div className="space-y-3">
        {avisos.map((a) => (
          <article key={a.id} className={classe(a.prioridade)}>
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  {a.fixado && <Pin className="w-3.5 h-3.5 neon-amber" />}
                  {a.prioridade === 'URGENTE' ? (
                    <AlertTriangle className="w-3.5 h-3.5 text-red-400" />
                  ) : (
                    <Info className="w-3.5 h-3.5 neon" />
                  )}
                  <h3 className="font-semibold text-sm">{a.titulo}</h3>
                  {a.turmaNome && <span className="chip-tag">{a.turmaNome}</span>}
                </div>
                <p className="text-sm txt-dim mt-1.5 whitespace-pre-wrap">{a.conteudo}</p>
                <p className="text-xs txt-faint mt-2">
                  {a.autorNome ? `${a.autorNome} · ` : ''}
                  {a.criadoEm ? new Date(a.criadoEm).toLocaleString('pt-BR') : ''}
                </p>
              </div>
              {isProfessor && (
                <div className="flex items-center gap-1 shrink-0">
                  <button className="icon-btn" title="Editar" onClick={() => abrirEdicao(a)}>
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button className="icon-btn" title="Excluir" onClick={() => excluir(a)}>
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              )}
            </div>
          </article>
        ))}
      </div>

      {form && isProfessor && (
        <div className="modal-backdrop" onClick={() => setForm(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold">{form.id ? 'Editar aviso' : 'Novo aviso'}</h3>
              <button className="icon-btn" onClick={() => setForm(null)} aria-label="Fechar">
                <X className="w-4 h-4" />
              </button>
            </div>

            <label className="ux-label" htmlFor="aviso-titulo">Título</label>
            <input
              id="aviso-titulo"
              className="input-field w-full mb-3"
              value={form.titulo}
              onChange={(e) => setForm({ ...form, titulo: e.target.value })}
            />

            <label className="ux-label" htmlFor="aviso-texto">Texto</label>
            <textarea
              id="aviso-texto"
              rows={5}
              className="input-field w-full mb-3"
              value={form.conteudo}
              onChange={(e) => setForm({ ...form, conteudo: e.target.value })}
            />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
              <div>
                <label className="ux-label" htmlFor="aviso-prio">Prioridade</label>
                <select
                  id="aviso-prio"
                  className="input-field w-full"
                  value={form.prioridade}
                  onChange={(e) => setForm({ ...form, prioridade: e.target.value as Aviso['prioridade'] })}
                >
                  <option value="INFORMATIVO">Informativo</option>
                  <option value="IMPORTANTE">Importante</option>
                  <option value="URGENTE">Urgente</option>
                </select>
              </div>
              <div>
                <label className="ux-label" htmlFor="aviso-validade">Válido até (opcional)</label>
                <input
                  id="aviso-validade"
                  type="datetime-local"
                  className="input-field w-full"
                  value={form.validoAte}
                  onChange={(e) => setForm({ ...form, validoAte: e.target.value })}
                />
              </div>
            </div>

            <label className="flex items-center gap-2 text-sm mb-4 cursor-pointer">
              <input
                type="checkbox"
                checked={form.fixado}
                onChange={(e) => setForm({ ...form, fixado: e.target.checked })}
              />
              Fixar no topo do quadro
            </label>

            <div className="flex justify-end gap-2">
              <button className="btn-secondary" onClick={() => setForm(null)}>Cancelar</button>
              <button className="btn-primary" onClick={salvar} disabled={salvando}>
                {salvando && <Loader2 className="w-4 h-4 animate-spin" />}
                Publicar
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
};

export default NoticeBoard;
