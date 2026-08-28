import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { Loader2, Plus, Pencil, Trash2, X, Save, RefreshCw, Database } from 'lucide-react';

type Row = Record<string, any>;

type FieldType = 'text' | 'textarea' | 'password' | 'select' | 'ref' | 'datetime' | 'number';

type Field = {
  key: string;
  label: string;
  type?: FieldType;
  options?: string[];
  ref?: ResourceKey;
  refLabel?: (row: Row) => string;
  required?: boolean;
  hideInTable?: boolean;
};

type ResourceKey =
  | 'instituicoes'
  | 'periodos'
  | 'cursos'
  | 'turmas'
  | 'usuarios'
  | 'matriculas'
  | 'trilhas'
  | 'aulas';

type Resource = {
  key: ResourceKey;
  label: string;
  columns: { key: string; label: string }[];
  fields: Field[];
};

const RESOURCES: Resource[] = [
  {
    key: 'instituicoes',
    label: 'Instituições',
    columns: [
      { key: 'nome', label: 'Nome' },
      { key: 'codigo', label: 'Código' },
      { key: 'descricao', label: 'Descrição' },
    ],
    fields: [
      { key: 'nome', label: 'Nome', required: true },
      { key: 'codigo', label: 'Código' },
      { key: 'descricao', label: 'Descrição', type: 'textarea' },
    ],
  },
  {
    key: 'periodos',
    label: 'Períodos',
    columns: [
      { key: 'nome', label: 'Nome' },
      { key: 'codigo', label: 'Código' },
      { key: 'instituicao', label: 'Instituição' },
    ],
    fields: [
      { key: 'nome', label: 'Nome', required: true },
      { key: 'codigo', label: 'Código' },
      { key: 'instituicaoId', label: 'Instituição', type: 'ref', ref: 'instituicoes', required: true },
    ],
  },
  {
    key: 'cursos',
    label: 'Cursos',
    columns: [
      { key: 'nome', label: 'Nome' },
      { key: 'codigo', label: 'Código' },
      { key: 'instituicao', label: 'Instituição' },
      { key: 'professor', label: 'Professor' },
    ],
    fields: [
      { key: 'nome', label: 'Nome', required: true },
      { key: 'codigo', label: 'Código' },
      { key: 'descricao', label: 'Descrição', type: 'textarea' },
      { key: 'instituicaoId', label: 'Instituição', type: 'ref', ref: 'instituicoes' },
      { key: 'professorId', label: 'Professor', type: 'ref', ref: 'usuarios' },
    ],
  },
  {
    key: 'turmas',
    label: 'Turmas',
    columns: [
      { key: 'codigo', label: 'Código' },
      { key: 'nome', label: 'Nome' },
      { key: 'curso', label: 'Curso' },
      { key: 'periodo', label: 'Período' },
      { key: 'professor', label: 'Professor' },
    ],
    fields: [
      { key: 'codigo', label: 'Código', required: true },
      { key: 'nome', label: 'Nome' },
      { key: 'cursoId', label: 'Curso', type: 'ref', ref: 'cursos', required: true },
      { key: 'periodoId', label: 'Período', type: 'ref', ref: 'periodos', required: true },
      { key: 'professorId', label: 'Professor', type: 'ref', ref: 'usuarios' },
    ],
  },
  {
    key: 'usuarios',
    label: 'Usuários',
    columns: [
      { key: 'nome', label: 'Nome' },
      { key: 'login', label: 'Login' },
      { key: 'email', label: 'E-mail' },
      { key: 'tipo', label: 'Tipo' },
    ],
    fields: [
      { key: 'nome', label: 'Nome', required: true },
      { key: 'login', label: 'Login', required: true },
      { key: 'email', label: 'E-mail' },
      { key: 'telefone', label: 'Telefone' },
      { key: 'tipo', label: 'Tipo', type: 'select', options: ['ALUNO', 'PROFESSOR', 'ADMIN', 'USER'] },
      { key: 'instituicao', label: 'Instituição (texto)' },
      { key: 'senha', label: 'Senha (deixe vazio para manter)', type: 'password', hideInTable: true },
    ],
  },
  {
    key: 'matriculas',
    label: 'Matrículas',
    columns: [
      { key: 'usuario', label: 'Aluno' },
      { key: 'curso', label: 'Curso' },
      { key: 'turma', label: 'Turma' },
      { key: 'status', label: 'Status' },
    ],
    fields: [
      { key: 'usuarioId', label: 'Aluno', type: 'ref', ref: 'usuarios', required: true },
      { key: 'cursoId', label: 'Curso', type: 'ref', ref: 'cursos', required: true },
      { key: 'turmaId', label: 'Turma', type: 'ref', ref: 'turmas' },
      { key: 'status', label: 'Status', type: 'select', options: ['ATIVA', 'TRANCADA', 'CONCLUIDA'] },
    ],
  },
  {
    key: 'trilhas',
    label: 'Trilhas',
    columns: [
      { key: 'titulo', label: 'Título' },
      { key: 'descricao', label: 'Descrição' },
      { key: 'ativa', label: 'Ativa' },
    ],
    fields: [
      { key: 'titulo', label: 'Título', required: true },
      { key: 'descricao', label: 'Descrição', type: 'textarea' },
      { key: 'conteudoJson', label: 'Conteúdo (JSON)', type: 'textarea' },
    ],
  },
  {
    key: 'aulas',
    label: 'Aulas',
    columns: [
      { key: 'titulo', label: 'Título' },
      { key: 'curso', label: 'Curso' },
      { key: 'turma', label: 'Turma' },
      { key: 'dataAula', label: 'Data' },
      { key: 'status', label: 'Status' },
    ],
    fields: [
      { key: 'titulo', label: 'Título', required: true },
      { key: 'descricao', label: 'Descrição', type: 'textarea' },
      { key: 'cursoId', label: 'Curso', type: 'ref', ref: 'cursos', required: true },
      { key: 'turmaId', label: 'Turma', type: 'ref', ref: 'turmas' },
      { key: 'dataAula', label: 'Data e hora', type: 'datetime' },
      { key: 'duracao', label: 'Duração', },
      { key: 'status', label: 'Status', type: 'select', options: ['AGENDADA', 'EM_ANDAMENTO', 'FINALIZADA', 'CANCELADA'] },
    ],
  },
];

const refLabelFor = (resource: ResourceKey, row: Row): string => {
  if (resource === 'usuarios') return `${row.nome ?? row.login} (${row.tipo ?? ''})`;
  if (resource === 'turmas') return `${row.codigo}${row.curso ? ` · ${row.curso}` : ''}`;
  return row.nome ?? row.titulo ?? row.codigo ?? `#${row.id}`;
};

const AdminPanel: React.FC = () => {
  const [active, setActive] = useState<ResourceKey>('instituicoes');
  const [data, setData] = useState<Partial<Record<ResourceKey, Row[]>>>({});
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [form, setForm] = useState<Row | null>(null);
  const [salvando, setSalvando] = useState(false);

  const resource = useMemo(() => RESOURCES.find((r) => r.key === active)!, [active]);

  const fetchResource = useCallback(async (key: ResourceKey) => {
    const resp = await api.get(`/api/admin/${key}`);
    setData((prev) => ({ ...prev, [key]: Array.isArray(resp.data) ? resp.data : [] }));
  }, []);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setErro(null);
    try {
      const refs = new Set<ResourceKey>([active]);
      resource.fields.forEach((f) => f.ref && refs.add(f.ref));
      await Promise.all([...refs].map((k) => fetchResource(k)));
    } catch (e: any) {
      setErro(e?.response?.data?.message || 'Não foi possível carregar os dados.');
    } finally {
      setLoading(false);
    }
  }, [active, resource, fetchResource]);

  useEffect(() => { loadAll(); }, [loadAll]);

  const rows = data[active] ?? [];

  const abrirNovo = () => {
    const inicial: Row = {};
    resource.fields.forEach((f) => { inicial[f.key] = f.type === 'select' ? (f.options?.[0] ?? '') : ''; });
    setForm(inicial);
  };

  const abrirEdicao = (row: Row) => {
    const inicial: Row = { id: row.id };
    resource.fields.forEach((f) => {
      const value = row[f.key];
      inicial[f.key] = f.type === 'password' ? '' : value === null || value === undefined ? '' : String(value).slice(0, f.type === 'datetime' ? 16 : undefined);
    });
    setForm(inicial);
  };

  const salvar = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form) return;
    setSalvando(true);
    setErro(null);
    try {
      const payload: Row = {};
      resource.fields.forEach((f) => {
        const value = form[f.key];
        if (f.type === 'password' && !value) return;
        payload[f.key] = value === '' ? null : value;
      });
      if (form.id) await api.put(`/api/admin/${active}/${form.id}`, payload);
      else await api.post(`/api/admin/${active}`, payload);
      setForm(null);
      await fetchResource(active);
    } catch (e: any) {
      setErro(e?.response?.data?.message || 'Não foi possível salvar o registro.');
    } finally {
      setSalvando(false);
    }
  };

  const excluir = async (row: Row) => {
    if (!window.confirm('Confirma a exclusão deste registro?')) return;
    setErro(null);
    try {
      await api.delete(`/api/admin/${active}/${row.id}`);
      await fetchResource(active);
    } catch (e: any) {
      setErro(e?.response?.data?.message || 'Não foi possível excluir o registro.');
    }
  };

  return (
    <section className="space-y-4">
      <div className="card flex flex-wrap items-center gap-2">
        <div className="rounded-xl glass p-2.5"><Database className="h-4 w-4 neon" /></div>
        {RESOURCES.map((r) => (
          <button
            key={r.key}
            onClick={() => setActive(r.key)}
            className={`chip-tag ${active === r.key ? 'chip-info' : ''}`}
          >
            {r.label}
          </button>
        ))}
        <div className="ml-auto flex gap-2">
          <button onClick={loadAll} className="btn-ghost"><RefreshCw className="h-4 w-4" /> Atualizar</button>
          <button onClick={abrirNovo} className="btn-primary"><Plus className="h-4 w-4" /> Novo</button>
        </div>
      </div>

      {erro && <div className="card text-sm" style={{ color: '#fca5a5' }}>{erro}</div>}

      <div className="card overflow-x-auto">
        {loading ? (
          <div className="flex items-center gap-2 txt-dim text-sm"><Loader2 className="h-4 w-4 animate-spin" /> Carregando {resource.label.toLowerCase()}…</div>
        ) : rows.length === 0 ? (
          <p className="txt-dim text-sm">Nenhum registro cadastrado.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left txt-faint">
                <th className="py-2 pr-3">#</th>
                {resource.columns.map((c) => <th key={c.key} className="py-2 pr-3">{c.label}</th>)}
                <th className="py-2 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className="border-t divider">
                  <td className="py-2 pr-3 txt-faint">{row.id}</td>
                  {resource.columns.map((c) => (
                    <td key={c.key} className="py-2 pr-3">
                      {typeof row[c.key] === 'boolean' ? (row[c.key] ? 'Sim' : 'Não') : (row[c.key] ?? '—')}
                    </td>
                  ))}
                  <td className="py-2">
                    <div className="flex justify-end gap-2">
                      <button onClick={() => abrirEdicao(row)} className="btn-ghost" title="Editar"><Pencil className="h-3.5 w-3.5" /></button>
                      <button onClick={() => excluir(row)} className="btn-ghost" title="Excluir"><Trash2 className="h-3.5 w-3.5" /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {form && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <form onSubmit={salvar} className="card w-full max-w-lg max-h-[85vh] overflow-y-auto space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold title-glow">
                {form.id ? 'Editar' : 'Novo'} · {resource.label}
              </h3>
              <button type="button" onClick={() => setForm(null)} className="btn-ghost"><X className="h-4 w-4" /></button>
            </div>

            {resource.fields.map((f) => (
              <label key={f.key} className="block">
                <span className="text-xs txt-faint">{f.label}{f.required ? ' *' : ''}</span>
                {f.type === 'textarea' ? (
                  <textarea
                    className="input-field w-full mt-1"
                    rows={3}
                    value={form[f.key] ?? ''}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                  />
                ) : f.type === 'select' ? (
                  <select
                    className="input-field w-full mt-1"
                    value={form[f.key] ?? ''}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                  >
                    {f.options?.map((o) => <option key={o} value={o}>{o}</option>)}
                  </select>
                ) : f.type === 'ref' ? (
                  <select
                    className="input-field w-full mt-1"
                    value={form[f.key] ?? ''}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                    required={f.required}
                  >
                    <option value="">Selecione…</option>
                    {(data[f.ref!] ?? []).map((o) => (
                      <option key={o.id} value={o.id}>{refLabelFor(f.ref!, o)}</option>
                    ))}
                  </select>
                ) : (
                  <input
                    className="input-field w-full mt-1"
                    type={f.type === 'password' ? 'password' : f.type === 'datetime' ? 'datetime-local' : 'text'}
                    value={form[f.key] ?? ''}
                    required={f.required && f.type !== 'password'}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                  />
                )}
              </label>
            ))}

            <button type="submit" disabled={salvando} className="btn-primary w-full justify-center">
              {salvando ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              Salvar
            </button>
          </form>
        </div>
      )}
    </section>
  );
};

export default AdminPanel;
