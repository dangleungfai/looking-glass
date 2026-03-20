import { useEffect, useState } from 'react';
import { getPublicUiConfig, systemSettings } from '../services/api';

const CACHE_KEY = 'lg_footer_text';

function readCached() {
  return localStorage.getItem(CACHE_KEY) || '';
}

export function useFooterText() {
  const [footerText, setFooterText] = useState<string>(readCached);

  useEffect(() => {
    const sync = (value?: string) => {
      const finalText = (value || '').trim();
      localStorage.setItem(CACHE_KEY, finalText);
      setFooterText(finalText);
    };

    sync(readCached());

    const token = localStorage.getItem('token');
    if (token) {
      systemSettings.get()
        .then((s) => {
          sync(s?.general?.footerText || readCached());
        })
        .catch(() => sync(readCached()));
    } else {
      getPublicUiConfig()
        .then((cfg) => sync(cfg.footerText))
        .catch(() => sync(readCached()));
    }

    const handleChanged = (e: Event) => {
      const detail = (e as CustomEvent<{ footerText?: string }>).detail;
      sync(detail?.footerText || readCached());
    };
    window.addEventListener('lg-footer-text-changed', handleChanged);
    return () => window.removeEventListener('lg-footer-text-changed', handleChanged);
  }, []);

  return footerText;
}
