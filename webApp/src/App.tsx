import React, { useState } from 'react';
import { ProductList } from './components/ProductList';
import { ProductDetail } from './components/ProductDetail';
import { ARTryOn } from './components/ARTryOn';
import { Product } from './data/products';
import { LanguageProvider, useLang } from './context/LanguageContext';

type Screen =
  | { type: 'list' }
  | { type: 'detail'; product: Product }
  | { type: 'ar'; product: Product };

function AppInner() {
  const [screen, setScreen] = useState<Screen>({ type: 'list' });
  const { lang } = useLang();

  return (
    <div dir={lang === 'ar' ? 'rtl' : 'ltr'} style={{ minHeight: '100vh' }}>
      {screen.type === 'list' && (
        <ProductList
          onProductClick={(product) => setScreen({ type: 'detail', product })}
          onTryOn={(product) => setScreen({ type: 'ar', product })}
        />
      )}
      {screen.type === 'detail' && (
        <ProductDetail
          product={screen.product}
          onBack={() => setScreen({ type: 'list' })}
          onTryOn={(product) => setScreen({ type: 'ar', product })}
        />
      )}
      {screen.type === 'ar' && (
        <ARTryOn
          product={screen.product}
          onBack={() => setScreen({ type: 'detail', product: screen.product })}
        />
      )}
    </div>
  );
}

function App() {
  return (
    <LanguageProvider>
      <AppInner />
    </LanguageProvider>
  );
}

export default App;
