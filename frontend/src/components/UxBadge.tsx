import React, { useEffect, useRef, useState } from 'react';
import { Palette, X, RotateCcw, Moon, Zap, Sun, Sparkles } from 'lucide-react';
import { useUx, ThemeName } from '../contexts/UxContext';

const TEMAS: { id: ThemeName; label: string; icone: React.ReactNode; titulo: string }[] = [
  { id: 'atual', label: 'Escuro', icone: <Moon className="w-3.5 h-3.5" />, titulo: 'Escuro fluorescente (padrão)' },
  { id: 'holo', label: 'Claro holo', icone: <Sun className="w-3.5 h-3.5" />, titulo: 'Claro holográfico' },
  { id: 'cyber', label: 'Cyber', icone: <Zap className="w-3.5 h-3.5" />, titulo: 'Dark neon cyberpunk' },
];

const ATALHOS_BASE = ['#00ffd1', '#7cff4f', '#00e5ff', '#ff3ea5', '#b026ff', '#ffb300', '#ff4d6d', '#4f8cff'];

const UxBadge: React.FC = () => {
  const { ux, atualizar, trocarTema, restaurar } = useUx();
  const [aberto, setAberto] = useState(false);
  const painelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!aberto) return;
    const aoClicarFora = (e: MouseEvent) => {
      if (painelRef.current && !painelRef.current.contains(e.target as Node)) setAberto(false);
    };
    const aoTeclar = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setAberto(false);
    };
    document.addEventListener('mousedown', aoClicarFora);
    document.addEventListener('keydown', aoTeclar);
    return () => {
      document.removeEventListener('mousedown', aoClicarFora);
      document.removeEventListener('keydown', aoTeclar);
    };
  }, [aberto]);

  return (
    <div className="ux-badge-root" ref={painelRef}>
      {aberto && (
        <div className="ux-panel" role="dialog" aria-label="Personalização da interface">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 neon" />
              <h2 className="font-semibold text-sm">Personalizar interface</h2>
            </div>
            <button className="icon-btn" onClick={() => setAberto(false)} aria-label="Fechar">
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="ux-field">
            <label className="ux-label">Tema</label>
            <div className="flex flex-wrap gap-1.5">
              {TEMAS.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  title={t.titulo}
                  aria-pressed={ux.tema === t.id}
                  onClick={() => trocarTema(t.id)}
                  className={`theme-chip ${ux.tema === t.id ? 'theme-chip-active' : ''}`}
                >
                  {t.icone}
                  {t.label}
                </button>
              ))}
            </div>
          </div>

          <div className="ux-field">
            <label className="ux-label" htmlFor="ux-cor-base">Cor base</label>
            <div className="flex items-center gap-2">
              <input
                id="ux-cor-base"
                type="color"
                className="ux-color"
                value={ux.corBase}
                onChange={(e) => atualizar({ corBase: e.target.value })}
              />
              <input
                type="text"
                className="input-field flex-1 font-mono text-xs"
                value={ux.corBase}
                onChange={(e) => {
                  const v = e.target.value.trim();
                  if (/^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/.test(v)) atualizar({ corBase: v });
                }}
                aria-label="Código hexadecimal da cor base"
              />
            </div>
            <div className="flex flex-wrap gap-1.5 mt-2">
              {ATALHOS_BASE.map((c) => (
                <button
                  key={c}
                  type="button"
                  title={c}
                  aria-label={`Usar a cor ${c}`}
                  onClick={() => atualizar({ corBase: c })}
                  className={`ux-swatch ${ux.corBase.toLowerCase() === c ? 'ux-swatch-active' : ''}`}
                  style={{ background: c }}
                />
              ))}
            </div>
          </div>

          <div className="ux-field">
            <label className="ux-label" htmlFor="ux-cor-apoio">Cor de apoio</label>
            <div className="flex items-center gap-2">
              <input
                id="ux-cor-apoio"
                type="color"
                className="ux-color"
                value={ux.corApoio}
                onChange={(e) => atualizar({ corApoio: e.target.value })}
              />
              <span className="text-xs txt-dim">Usada em gradientes e destaques secundários</span>
            </div>
          </div>

          <div className="ux-field">
            <label className="ux-label" htmlFor="ux-fonte">
              Tamanho da fonte <span className="txt-faint">({Math.round(ux.escalaFonte * 100)}%)</span>
            </label>
            <input
              id="ux-fonte"
              type="range"
              min={0.85}
              max={1.25}
              step={0.05}
              value={ux.escalaFonte}
              onChange={(e) => atualizar({ escalaFonte: Number(e.target.value) })}
              className="ux-range"
            />
          </div>

          <div className="ux-field">
            <label className="ux-label" htmlFor="ux-raio">
              Arredondamento dos cartões <span className="txt-faint">({ux.arredondamento}px)</span>
            </label>
            <input
              id="ux-raio"
              type="range"
              min={0}
              max={32}
              step={2}
              value={ux.arredondamento}
              onChange={(e) => atualizar({ arredondamento: Number(e.target.value) })}
              className="ux-range"
            />
          </div>

          <div className="ux-field">
            <label className="ux-label" htmlFor="ux-brilho">
              Intensidade do brilho <span className="txt-faint">({ux.brilho}px)</span>
            </label>
            <input
              id="ux-brilho"
              type="range"
              min={0}
              max={40}
              step={2}
              value={ux.brilho}
              onChange={(e) => atualizar({ brilho: Number(e.target.value) })}
              className="ux-range"
            />
          </div>

          <div className="ux-field">
            <label className="ux-label" htmlFor="ux-densidade">
              Densidade do espaçamento <span className="txt-faint">({Math.round(ux.densidade * 100)}%)</span>
            </label>
            <input
              id="ux-densidade"
              type="range"
              min={0.8}
              max={1.25}
              step={0.05}
              value={ux.densidade}
              onChange={(e) => atualizar({ densidade: Number(e.target.value) })}
              className="ux-range"
            />
          </div>

          <label className="flex items-center gap-2 text-sm cursor-pointer mb-3">
            <input
              type="checkbox"
              checked={ux.reduzirAnimacoes}
              onChange={(e) => atualizar({ reduzirAnimacoes: e.target.checked })}
            />
            Reduzir animações
          </label>

          <button type="button" className="btn-secondary w-full justify-center" onClick={restaurar}>
            <RotateCcw className="w-4 h-4" />
            Restaurar padrão
          </button>
        </div>
      )}

      <button
        type="button"
        className="ux-fab"
        onClick={() => setAberto((v) => !v)}
        aria-expanded={aberto}
        aria-label="Abrir personalização da interface"
        title="UX — personalizar cores, fontes e tema"
      >
        <Palette className="w-4 h-4" />
        <span>UX</span>
      </button>
    </div>
  );
};

export default UxBadge;
