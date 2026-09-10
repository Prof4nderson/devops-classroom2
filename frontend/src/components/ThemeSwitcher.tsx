import React, { useEffect, useState } from 'react';
import { Sparkles, Zap, Sun } from 'lucide-react';

export type ThemeName = 'atual' | 'cyber' | 'holo';

const STORAGE_KEY = 'devops-classroom-theme';

const TEMAS: { id: ThemeName; label: string; icon: React.ReactNode; titulo: string }[] = [
  { id: 'atual', label: 'Atual', icon: <Sparkles className="w-3.5 h-3.5" />, titulo: 'Visual atual (glass fluorescente)' },
  { id: 'cyber', label: 'Cyber', icon: <Zap className="w-3.5 h-3.5" />, titulo: 'Dark neon cyberpunk' },
  { id: 'holo', label: 'Holo', icon: <Sun className="w-3.5 h-3.5" />, titulo: 'Light holográfico' },
];

export function aplicarTemaSalvo() {
  const salvo = (localStorage.getItem(STORAGE_KEY) as ThemeName) || 'atual';
  document.documentElement.setAttribute('data-theme', salvo);
  return salvo;
}

const ThemeSwitcher: React.FC = () => {
  const [tema, setTema] = useState<ThemeName>('atual');

  useEffect(() => {
    setTema(aplicarTemaSalvo());
  }, []);

  const trocar = (novo: ThemeName) => {
    setTema(novo);
    localStorage.setItem(STORAGE_KEY, novo);
    document.documentElement.setAttribute('data-theme', novo);
  };

  return (
    <div className="theme-switch" role="group" aria-label="Selecionar tema visual">
      {TEMAS.map((t) => (
        <button
          key={t.id}
          type="button"
          title={t.titulo}
          aria-pressed={tema === t.id}
          onClick={() => trocar(t.id)}
          className={`theme-chip ${tema === t.id ? 'theme-chip-active' : ''}`}
        >
          {t.icon}
          {t.label}
        </button>
      ))}
    </div>
  );
};

export default ThemeSwitcher;
