import React, { useState } from 'react';
import { Send, X, Loader2, Megaphone } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../services/api';

interface Props {
  aulaId: number;
  alunoId: number;
  alunoNome: string;
  onClose: () => void;
}

const TONS = [
  { valor: 'INFO', rotulo: 'Recado' },
  { valor: 'ELOGIO', rotulo: 'Elogio' },
  { valor: 'ATENCAO', rotulo: 'Atenção' },
];

const SUGESTOES = [
  'Ótima participação hoje!',
  'Pode revisar o último exercício?',
  'Fique atento ao prazo da entrega.',
];

const DirectToast: React.FC<Props> = ({ aulaId, alunoId, alunoNome, onClose }) => {
  const [mensagem, setMensagem] = useState('');
  const [tom, setTom] = useState('INFO');
  const [enviando, setEnviando] = useState(false);

  const enviar = async () => {
    if (!mensagem.trim()) {
      toast.error('Escreva a mensagem');
      return;
    }
    setEnviando(true);
    try {
      await api.post(`/api/recados/aula/${aulaId}`, {
        alunoId,
        mensagem: mensagem.trim(),
        tom,
      });
      toast.success(`Recado enviado para ${alunoNome}`);
      onClose();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Não foi possível enviar o recado');
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold flex items-center gap-2">
            <Megaphone className="w-4 h-4 neon" />
            Recado para {alunoNome}
          </h3>
          <button className="icon-btn" onClick={onClose} aria-label="Fechar">
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-xs txt-faint mb-3">
          Só {alunoNome} vai receber esta mensagem, na hora, sem aparecer no chat da turma.
        </p>

        <label className="ux-label" htmlFor="recado-texto">Mensagem</label>
        <textarea
          id="recado-texto"
          rows={4}
          maxLength={280}
          className="input-field w-full mb-2"
          value={mensagem}
          onChange={(e) => setMensagem(e.target.value)}
          placeholder="Escreva um recado curto…"
        />

        <div className="flex flex-wrap gap-1.5 mb-3">
          {SUGESTOES.map((s) => (
            <button key={s} type="button" className="chip-tag" onClick={() => setMensagem(s)}>
              {s}
            </button>
          ))}
        </div>

        <label className="ux-label" htmlFor="recado-tom">Tom</label>
        <select
          id="recado-tom"
          className="input-field w-full mb-4"
          value={tom}
          onChange={(e) => setTom(e.target.value)}
        >
          {TONS.map((t) => (
            <option key={t.valor} value={t.valor}>{t.rotulo}</option>
          ))}
        </select>

        <div className="flex justify-end gap-2">
          <button className="btn-secondary" onClick={onClose}>Cancelar</button>
          <button className="btn-primary" onClick={enviar} disabled={enviando}>
            {enviando ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
            Enviar
          </button>
        </div>
      </div>
    </div>
  );
};

export default DirectToast;
