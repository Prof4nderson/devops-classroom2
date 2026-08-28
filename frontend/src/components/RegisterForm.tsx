import React, { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { UserPlus, Terminal, Building2, BookOpen, Layers } from 'lucide-react';

type Opcao = { id: number; nome: string; codigo?: string; instituicaoId?: number; cursoId?: number; periodo?: string };

const RegisterForm: React.FC = () => {
  const [formData, setFormData] = useState({
    login: '',
    senha: '',
    nome: '',
    email: '',
    telefone: '',
    tipo: 'ALUNO',
    bio: '',
    instituicaoId: '',
    cursoId: '',
    turmaId: '',
  });
  const [instituicoes, setInstituicoes] = useState<Opcao[]>([]);
  const [cursos, setCursos] = useState<Opcao[]>([]);
  const [turmas, setTurmas] = useState<Opcao[]>([]);
  const [loading, setLoading] = useState(false);

  // Instituições disponíveis no cadastro
  useEffect(() => {
    fetch('/api/publico/academico/instituicoes')
      .then((r) => (r.ok ? r.json() : []))
      .then(setInstituicoes)
      .catch(() => toast.error('Não foi possível carregar as instituições.'));
  }, []);

  // Cursos da instituição escolhida
  useEffect(() => {
    setCursos([]);
    setTurmas([]);
    setFormData((prev) => ({ ...prev, cursoId: '', turmaId: '' }));
    if (!formData.instituicaoId) return;
    fetch(`/api/publico/academico/cursos?instituicaoId=${formData.instituicaoId}`)
      .then((r) => (r.ok ? r.json() : []))
      .then(setCursos)
      .catch(() => setCursos([]));
  }, [formData.instituicaoId]);

  // Turmas do curso escolhido
  useEffect(() => {
    setTurmas([]);
    setFormData((prev) => ({ ...prev, turmaId: '' }));
    if (!formData.cursoId) return;
    fetch(`/api/publico/academico/turmas?cursoId=${formData.cursoId}`)
      .then((r) => (r.ok ? r.json() : []))
      .then(setTurmas)
      .catch(() => setTurmas([]));
  }, [formData.cursoId]);

  const podeEnviar = useMemo(
    () => Boolean(formData.instituicaoId && formData.cursoId && formData.turmaId),
    [formData.instituicaoId, formData.cursoId, formData.turmaId]
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!podeEnviar) {
      toast.error('Selecione instituição, curso e turma.');
      return;
    }
    setLoading(true);
    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...formData,
          tipo: 'ALUNO',
          instituicaoId: Number(formData.instituicaoId),
          cursoId: Number(formData.cursoId),
          turmaId: Number(formData.turmaId),
        }),
      });

      if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.error || err.message || 'Erro ao criar conta');
      }

      toast.success('Conta criada com sucesso! Faça login.');
      window.location.href = '/login';
    } catch (err: any) {
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const labelClass = 'block text-xs font-semibold uppercase tracking-wider txt-dim mb-2';

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl glass mb-4">
            <Terminal className="w-8 h-8 neon-violet" />
          </div>
          <h1 className="text-3xl font-bold title-glow">Criar Conta</h1>
          <p className="txt-dim mt-2 text-sm">Junte-se ao DevOps Classroom</p>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Nome Completo</label>
              <input
                type="text"
                className="input-field"
                value={formData.nome}
                onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
                required
              />
            </div>
            <div>
              <label className={labelClass}>Login</label>
              <input
                type="text"
                className="input-field"
                value={formData.login}
                onChange={(e) => setFormData({ ...formData, login: e.target.value })}
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Email</label>
              <input
                type="email"
                className="input-field"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                required
              />
            </div>
            <div>
              <label className={labelClass}>Telefone</label>
              <input
                type="text"
                className="input-field"
                value={formData.telefone}
                onChange={(e) => setFormData({ ...formData, telefone: e.target.value })}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Senha</label>
              <input
                type="password"
                className="input-field"
                value={formData.senha}
                onChange={(e) => setFormData({ ...formData, senha: e.target.value })}
                required
                minLength={6}
              />
            </div>
            <div className="flex items-end">
              <div className="w-full rounded-xl border border-cyan-400/20 bg-cyan-400/5 px-3 py-3">
                <p className="text-xs font-semibold uppercase tracking-wider txt-dim">Perfil</p>
                <p className="mt-1 text-sm neon">Aluno</p>
              </div>
            </div>
          </div>

          <div>
            <label className={labelClass}>
              <span className="inline-flex items-center gap-1.5">
                <Building2 className="h-3.5 w-3.5 neon" /> Instituição
              </span>
            </label>
            <select
              className="input-field"
              value={formData.instituicaoId}
              onChange={(e) => setFormData({ ...formData, instituicaoId: e.target.value })}
              required
            >
              <option value="">Selecione a instituição</option>
              {instituicoes.map((i) => (
                <option key={i.id} value={i.id}>
                  {i.nome}
                  {i.codigo ? ` · ${i.codigo}` : ''}
                </option>
              ))}
            </select>
            {instituicoes.length === 0 && (
              <p className="text-xs txt-faint mt-1">Nenhuma instituição cadastrada. Procure o professor.</p>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>
                <span className="inline-flex items-center gap-1.5">
                  <BookOpen className="h-3.5 w-3.5 neon-lime" /> Curso
                </span>
              </label>
              <select
                className="input-field"
                value={formData.cursoId}
                onChange={(e) => setFormData({ ...formData, cursoId: e.target.value })}
                disabled={!formData.instituicaoId}
                required
              >
                <option value="">Selecione o curso</option>
                {cursos.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nome}
                    {c.codigo ? ` · ${c.codigo}` : ''}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className={labelClass}>
                <span className="inline-flex items-center gap-1.5">
                  <Layers className="h-3.5 w-3.5 neon-violet" /> Turma
                </span>
              </label>
              <select
                className="input-field"
                value={formData.turmaId}
                onChange={(e) => setFormData({ ...formData, turmaId: e.target.value })}
                disabled={!formData.cursoId}
                required
              >
                <option value="">Selecione a turma</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome}
                    {t.periodo ? ` · ${t.periodo}` : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className={labelClass}>Bio</label>
            <textarea
              className="input-field"
              rows={3}
              value={formData.bio}
              onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
              placeholder="Conte um pouco sobre você..."
            />
          </div>

          <button type="submit" disabled={loading || !podeEnviar} className="btn-primary w-full py-3 disabled:opacity-50">
            <UserPlus className="w-4 h-4" />
            {loading ? 'Criando...' : 'Criar Conta'}
          </button>
        </form>

        <div className="mt-6 text-center">
          <a href="/login" className="text-sm neon hover:opacity-80 transition-opacity">
            Já tem conta? Fazer login
          </a>
        </div>
      </div>
    </div>
  );
};

export default RegisterForm;
