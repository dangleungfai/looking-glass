import { useEffect, useState } from 'react';
import { getPublicUiConfig, systemSettings } from '../services/api';

const DEFAULT_SYSTEM_NAME = 'LOOKING GLASS';
const SYSTEM_NAME_CACHE_KEY = 'lg_system_name';
const SHOW_POP_CODE_CACHE_KEY = 'lg_show_pop_code';

function readCachedSystemName() {
  const v = localStorage.getItem(SYSTEM_NAME_CACHE_KEY);
  return (v || '').trim() || DEFAULT_SYSTEM_NAME;
}

export function useSystemName() {
  const [systemName, setSystemName] = useState(readCachedSystemName);

  useEffect(() => {
    const syncName = (name?: string) => {
      const finalName = (name || '').trim() || DEFAULT_SYSTEM_NAME;
      localStorage.setItem(SYSTEM_NAME_CACHE_KEY, finalName);
      setSystemName(finalName);
      document.title = finalName;
    };

    syncName(readCachedSystemName());

    const token = localStorage.getItem('token');
    if (token) {
      systemSettings.get()
        .then((s) => {
          const fromStructured = s?.general?.systemName;
          const fromStructuredShowPopCode = s?.general?.showPopCode;
          const finalShow = typeof fromStructuredShowPopCode === 'boolean' ? fromStructuredShowPopCode : true;
          localStorage.setItem(SHOW_POP_CODE_CACHE_KEY, finalShow ? 'true' : 'false');
          syncName(fromStructured || readCachedSystemName());
        })
        .catch(() => syncName(readCachedSystemName()));
    } else {
      getPublicUiConfig()
        .then((cfg) => {
          syncName(cfg.systemName);
          localStorage.setItem(SHOW_POP_CODE_CACHE_KEY, cfg.showPopCode ? 'true' : 'false');
        })
        .catch(() => syncName(readCachedSystemName()));
    }

    const handleNameChanged = (e: Event) => {
      const detail = (e as CustomEvent<string>).detail;
      syncName(detail || readCachedSystemName());
    };
    window.addEventListener('lg-system-name-changed', handleNameChanged);
    return () => {
      window.removeEventListener('lg-system-name-changed', handleNameChanged);
    };
  }, []);

  return systemName;
}
