import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

export type ThemeName = 'atual' | 'cyber' | 'holo';

export interface UxSettings {
  tema: ThemeName;
  corBase: string;
  corApoio: string;
  escalaFonte: number; // 0.85 – 1.25
  arredondamento: number; // px do raio dos cartões
  brilho: number; // px do glow neon
  densidade: number; // 0.8 – 1.2
  reduzirAnimacoes: boolean;
}

export const UX_PADRAO: UxSettings = {
  tema: 'atual',
  corBase: '#00ffd1',
  corApoio: '#7cff4f',
  escalaFonte: 1,
  arredondamento: 16,
  brilho: 18,
  densidade: 1,
  reduzirAnimacoes: false,
};

/** Cores padrão de cada tema — usadas quando o usuário restaura ou troca de tema. */
export const CORES_DO_TEMA: Record<ThemeName, { base: string; apoio: string }> = {
  atual: { base: '#00ffd1', apoio: '#7cff4f' },
  cyber: { base: '#00e5ff', apoio: '#ff3ea5' },
  holo: { base: '#0091a7', apoio: '#1f9d55' },
};

const STORAGE_KEY = 'devops-classroom-ux';
const LEGACY_THEME_KEY = 'devops-classroom-theme';

function hexParaRgb(hex: string): [number, number, number] {
  const limpo = hex.replace('#', '').trim();
  const completo = limpo.length === 3 ? limpo.split('').map((c) => c + c).join('') : limpo;
  const valor = parseInt(completo.slice(0, 6) || '000000', 16);
  return [(valor >> 16) & 255, (valor >> 8) & 255, valor & 255];
}

export function rgba(hex: string, alpha: number): string {
  const [r, g, b] = hexParaRgb(hex);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

/** Clareia (fator > 0) ou escurece (fator < 0) uma cor hexadecimal. */
function ajustar(hex: string, fator: number): string {
  const [r, g, b] = hexParaRgb(hex);
  const mover = (c: number) => Math.round(fator >= 0 ? c + (255 - c) * fator : c * (1 + fator));
  return `#${[mover(r), mover(g), mover(b)].map((c) => c.toString(16).padStart(2, '0')).join('')}`;
}

/** Preto ou branco, conforme o contraste com a cor escolhida. */
function corDeContraste(hex: string): string {
  const [r, g, b] = hexParaRgb(hex);
  const luminancia = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminancia > 0.6 ? '#05110f' : '#ffffff';
}

function carregar(): UxSettings {
  if (typeof window === 'undefined') return UX_PADRAO;
  try {
    const bruto = localStorage.getItem(STORAGE_KEY);
    if (bruto) return { ...UX_PADRAO, ...JSON.parse(bruto) } as UxSettings;
    const temaAntigo = localStorage.getItem(LEGACY_THEME_KEY) as ThemeName | null;
    if (temaAntigo && CORES_DO_TEMA[temaAntigo]) {
      return { ...UX_PADRAO, tema: temaAntigo, ...{ corBase: CORES_DO_TEMA[temaAntigo].base, corApoio: CORES_DO_TEMA[temaAntigo].apoio } };
    }
  } catch {
    /* preferências inválidas: volta ao padrão */
  }
  return UX_PADRAO;
}

export function aplicarUx(cfg: UxSettings) {
  if (typeof document === 'undefined') return;
  const raiz = document.documentElement;

  raiz.setAttribute('data-theme', cfg.tema);

  raiz.style.setProperty('--neon', cfg.corBase);
  raiz.style.setProperty('--neon-2', cfg.corApoio);
  raiz.style.setProperty('--neon-soft', rgba(cfg.corBase, 0.14));
  raiz.style.setProperty('--glass-border-lit', rgba(cfg.corBase, 0.45));
  raiz.style.setProperty('--on-accent', corDeContraste(cfg.corBase));
  raiz.style.setProperty('--code-fg', cfg.corApoio);
  raiz.style.setProperty(
    '--title-shadow',
    cfg.tema === 'holo' ? '0 1px 0 rgba(255,255,255,0.9)' : `0 0 26px ${rgba(cfg.corBase, 0.35)}`
  );
  raiz.style.setProperty('--neon-hover', ajustar(cfg.corBase, 0.18));

  raiz.style.setProperty('--glow-strength', `${cfg.brilho}px`);
  raiz.style.setProperty('--radius-card', `${cfg.arredondamento}px`);
  raiz.style.setProperty('--pad-scale', String(cfg.densidade));
  raiz.style.fontSize = `${16 * cfg.escalaFonte}px`;

  raiz.classList.toggle('ux-sem-animacao', cfg.reduzirAnimacoes);
}

interface UxContextValue {
  ux: UxSettings;
  atualizar: (parcial: Partial<UxSettings>) => void;
  trocarTema: (tema: ThemeName) => void;
  restaurar: () => void;
}

const UxContext = createContext<UxContextValue | null>(null);

export const UxProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [ux, setUx] = useState<UxSettings>(() => carregar());

  useEffect(() => {
    aplicarUx(ux);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(ux));
    } catch {
      /* armazenamento indisponível */
    }
  }, [ux]);

  const atualizar = useCallback((parcial: Partial<UxSettings>) => {
    setUx((atual) => ({ ...atual, ...parcial }));
  }, []);

  const trocarTema = useCallback((tema: ThemeName) => {
    setUx((atual) => {
      const padraoAnterior = CORES_DO_TEMA[atual.tema];
      const novoPadrao = CORES_DO_TEMA[tema];
      // Só troca as cores automaticamente se o usuário ainda usa as do tema atual.
      const usaPadrao =
        atual.corBase.toLowerCase() === padraoAnterior.base.toLowerCase() &&
        atual.corApoio.toLowerCase() === padraoAnterior.apoio.toLowerCase();
      return usaPadrao
        ? { ...atual, tema, corBase: novoPadrao.base, corApoio: novoPadrao.apoio }
        : { ...atual, tema };
    });
  }, []);

  const restaurar = useCallback(() => setUx(UX_PADRAO), []);

  const valor = useMemo(() => ({ ux, atualizar, trocarTema, restaurar }), [ux, atualizar, trocarTema, restaurar]);

  return <UxContext.Provider value={valor}>{children}</UxContext.Provider>;
};

export function useUx(): UxContextValue {
  const ctx = useContext(UxContext);
  if (!ctx) throw new Error('useUx precisa estar dentro de UxProvider');
  return ctx;
}
