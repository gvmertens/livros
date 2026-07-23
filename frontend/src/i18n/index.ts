import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import ptBR from './locales/pt-BR.json';
import enUS from './locales/en-US.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    // Supported locales
    supportedLngs: ['pt-BR', 'en-US'],
    fallbackLng: 'pt-BR',

    // Language detection configuration
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'i18n_language',
      caches: ['localStorage'],
    },

    // Translation resources
    resources: {
      'pt-BR': { translation: ptBR },
      'en-US': { translation: enUS },
    },

    interpolation: {
      // React already escapes values — no need for i18next to do it too
      escapeValue: false,
    },
  });

export default i18n;
