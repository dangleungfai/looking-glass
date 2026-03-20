import { useEffect, useState } from 'react';
import { getPublicUiConfig, systemSettings } from '../services/api';
import { DEFAULT_HOME_INTRO_TEXT } from '../constants/systemContent';

const CACHE_KEY = 'lg_home_intro_text';

function readCached() {
  return localStorage.getItem(CACHE_KEY) || DEFAULT_HOME_INTRO_TEXT;
}

export function useHomeIntroText() {
  const [homeIntroText, setHomeIntroText] = useState<string>(readCached);

  useEffect(() => {
    const sync = (value?: string) => {
      const finalText = (value || '').trim() || DEFAULT_HOME_INTRO_TEXT;
      localStorage.setItem(CACHE_KEY, finalText);
      setHomeIntroText(finalText);
    };

    sync(readCached());

    const token = localStorage.getItem('token');
    if (token) {
      systemSettings.get()
        .then((s) => {
          sync(s?.general?.homeIntroText || readCached());
        })
        .catch(() => sync(readCached()));
    } else {
      getPublicUiConfig()
        .then((cfg) => sync(cfg.homeIntroText))
        .catch(() => sync(readCached()));
    }

    const handleChanged = (e: Event) => {
      const detail = (e as CustomEvent<{ homeIntroText?: string }>).detail;
      sync(detail?.homeIntroText || readCached());
    };
    const handleStorage = (e: StorageEvent) => {
      if (e.key === CACHE_KEY) {
        sync(e.newValue || '');
      }
    };
    window.addEventListener('lg-home-intro-text-changed', handleChanged);
    window.addEventListener('storage', handleStorage);
    return () => {
      window.removeEventListener('lg-home-intro-text-changed', handleChanged);
      window.removeEventListener('storage', handleStorage);
    };
  }, []);

  return homeIntroText;
}
