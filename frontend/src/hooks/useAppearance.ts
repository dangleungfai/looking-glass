import { useEffect, useState } from 'react';
import { getPublicUiConfig, systemSettings } from '../services/api';

export type AppearanceTheme = 'default' | 'techBlue' | 'dark';

const CACHE_KEY = 'lg_appearance';
const EVENT_NAME = 'lg-appearance-changed';

function normalizeAppearance(value?: string): AppearanceTheme {
  if (value === 'techBlue' || value === 'dark') return value;
  return 'default';
}

function readCached(): AppearanceTheme {
  return normalizeAppearance(localStorage.getItem(CACHE_KEY) || undefined);
}

function applyAppearanceToDocument(theme: AppearanceTheme) {
  document.body.setAttribute('data-lg-theme', theme);
}

export function useAppearance(): AppearanceTheme {
  const [appearance, setAppearance] = useState<AppearanceTheme>(readCached);

  useEffect(() => {
    const sync = (value?: string) => {
      const finalTheme = normalizeAppearance(value);
      localStorage.setItem(CACHE_KEY, finalTheme);
      setAppearance(finalTheme);
      applyAppearanceToDocument(finalTheme);
    };

    sync(readCached());

    const token = localStorage.getItem('token');
    if (token) {
      systemSettings.get()
        .then((s) => sync(s?.general?.appearance || readCached()))
        .catch(() => sync(readCached()));
    } else {
      getPublicUiConfig()
        .then((cfg) => sync((cfg as { appearance?: string }).appearance || readCached()))
        .catch(() => sync(readCached()));
    }

    const onChanged = (e: Event) => {
      const detail = (e as CustomEvent<{ appearance?: string } | string>).detail;
      if (typeof detail === 'string') {
        sync(detail);
        return;
      }
      sync(detail?.appearance || readCached());
    };
    window.addEventListener(EVENT_NAME, onChanged);
    return () => window.removeEventListener(EVENT_NAME, onChanged);
  }, []);

  return appearance;
}

