import { useEffect, useState } from 'react';
import { getPublicUiConfig, systemSettings } from '../services/api';

const CACHE_KEY = 'lg_show_pop_code';

function readCached(): boolean {
  return localStorage.getItem(CACHE_KEY) !== 'false';
}

export function useShowPopCode() {
  const [showPopCode, setShowPopCode] = useState<boolean>(readCached);

  useEffect(() => {
    const sync = (value: boolean) => {
      localStorage.setItem(CACHE_KEY, value ? 'true' : 'false');
      setShowPopCode(value);
    };

    sync(readCached());

    const token = localStorage.getItem('token');
    if (token) {
      systemSettings.get()
        .then((s) => {
          const structured = s?.general?.showPopCode;
          sync(typeof structured === 'boolean' ? structured : true);
        })
        .catch(() => sync(readCached()));
    } else {
      getPublicUiConfig()
        .then((cfg) => sync(cfg.showPopCode))
        .catch(() => sync(readCached()));
    }

    const handleChanged = (e: Event) => {
      const detail = (e as CustomEvent<{ showPopCode?: boolean }>).detail;
      if (typeof detail?.showPopCode === 'boolean') {
        sync(detail.showPopCode);
      }
    };
    window.addEventListener('lg-show-pop-code-changed', handleChanged);
    return () => window.removeEventListener('lg-show-pop-code-changed', handleChanged);
  }, []);

  return showPopCode;
}
