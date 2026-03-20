import { useEffect, useState } from 'react';

const CACHE_BUSTER_KEY = 'lg_logo_ver';

function nextLogoVersion() {
  const v = Date.now().toString();
  localStorage.setItem(CACHE_BUSTER_KEY, v);
  return v;
}

function currentLogoVersion() {
  return localStorage.getItem(CACHE_BUSTER_KEY) || '1';
}

function buildUrl() {
  return `/api/public/logo?v=${encodeURIComponent(currentLogoVersion())}`;
}

export function bumpLogoVersion() {
  nextLogoVersion();
  window.dispatchEvent(new CustomEvent('lg-logo-changed'));
}

export function useLogoUrl() {
  const [url, setUrl] = useState(buildUrl);

  useEffect(() => {
    const onChanged = () => setUrl(buildUrl());
    window.addEventListener('lg-logo-changed', onChanged);
    return () => {
      window.removeEventListener('lg-logo-changed', onChanged);
    };
  }, []);

  return url;
}
