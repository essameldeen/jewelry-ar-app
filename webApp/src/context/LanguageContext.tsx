import React, { createContext, useContext, useState } from 'react';

export type Lang = 'en' | 'ar';

interface LangCtx {
  lang: Lang;
  toggle: () => void;
}

const Ctx = createContext<LangCtx>({ lang: 'en', toggle: () => {} });

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [lang, setLang] = useState<Lang>('en');
  const toggle = () => setLang((l) => (l === 'en' ? 'ar' : 'en'));
  return <Ctx.Provider value={{ lang, toggle }}>{children}</Ctx.Provider>;
};

export const useLang = () => useContext(Ctx);
