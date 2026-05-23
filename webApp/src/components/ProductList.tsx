import React, { useState } from 'react';
import { Product, products, Category, CATEGORY_LABELS, CATEGORY_LABELS_AR } from '../data/products';
import { useLang } from '../context/LanguageContext';

interface Props {
  onProductClick: (product: Product) => void;
  onTryOn: (product: Product) => void;
}

const G_DARK = '#2D4A2D';
const G_MID = '#3D5C3D';
const GOLD = '#C8A84C';
const CREAM = '#F5F0E8';
const WHITE = '#FFFFFF';
const TEXT_DARK = '#1A2E1A';
const TEXT_MED = '#5A6E5A';

const allCategories: ('all' | Category)[] = ['all', 'necklace', 'ring', 'bracelet', 'luxury'];

export const ProductList: React.FC<Props> = ({ onProductClick, onTryOn }) => {
  const { lang, toggle } = useLang();
  const ar = lang === 'ar';
  const cats = ar ? CATEGORY_LABELS_AR : CATEGORY_LABELS;

  const [selectedCategory, setSelectedCategory] = useState<'all' | Category>('all');
  const [heroIndex, setHeroIndex] = useState(0);

  const featured = products.slice(0, 3);
  const hero = featured[heroIndex];
  const sideCards = featured.filter((_, i) => i !== heroIndex);

  const filtered =
    selectedCategory === 'all'
      ? products
      : products.filter((p) => p.category === selectedCategory);

  return (
    <div style={s.page}>
      {/* ── Navbar ── */}
      <nav style={s.nav}>
        <div style={s.navInner}>
          <div style={s.logo}>
            <span style={s.logoLeaf}>🌿</span>
            <span style={s.logoText}>LOGO</span>
          </div>
          <div style={s.navLinks}>
            {['Home', 'Categories', 'Collections', 'Featured'].map((item, i) => (
              <a
                key={item}
                href="#"
                style={i === 0 ? s.navLinkActive : s.navLink}
                onClick={(e) => e.preventDefault()}
              >
                {item}
              </a>
            ))}
          </div>
          <div style={s.navIcons}>
            <span style={s.navIcon}>🔍</span>
            <span style={s.navIcon}>🛒</span>
            <button onClick={toggle} style={s.langBtn}>
              {ar ? 'EN' : 'AR'}
            </button>
          </div>
        </div>
      </nav>

      {/* ── Hero ── */}
      <section style={s.heroSection}>
        {/* Left — featured product */}
        <div style={s.heroLeft}>
          <div style={s.heroImgCard}>
            <img src={hero.image} alt={hero.name} style={s.heroImg} />
            <div style={s.heroDots}>
              {featured.map((_, i) => (
                <button
                  key={i}
                  onClick={() => setHeroIndex(i)}
                  style={{ ...s.dot, ...(i === heroIndex ? s.dotActive : {}) }}
                />
              ))}
            </div>
          </div>
          <div style={s.heroMeta}>
            <h2 style={s.heroName}>{ar ? hero.nameAr : hero.name}</h2>
            <p style={s.heroDesc}>{ar ? hero.descriptionAr : hero.description}</p>
            <button style={s.heroBtn} onClick={() => onTryOn(hero)}>
              {ar ? 'استكشف الآن' : 'Explore Now'}&nbsp;&nbsp;📷
            </button>
          </div>
        </div>

        {/* Right — 3 mini cards */}
        <div style={s.heroRight}>
          {sideCards.map((p) => (
            <div key={p.id} style={s.sideCard} onClick={() => onProductClick(p)}>
              <div style={s.sideImgWrap}>
                <img src={p.image} alt={p.name} style={s.sideImg} />
                {p.isBestseller && <div style={s.goldRibbon} />}
              </div>
              <div style={s.sideBody}>
                <h3 style={s.sideName}>{ar ? p.nameAr : p.name}</h3>
                <p style={s.sideDesc}>{ar ? p.descriptionAr : p.description}</p>
                <button
                  style={s.sideBtn}
                  onClick={(e) => { e.stopPropagation(); onTryOn(p); }}
                >
                  {ar ? 'استكشف' : 'Explore Now'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Category Filters ── */}
      <div style={s.filterWrap}>
        <div style={s.filterRow}>
          {allCategories.map((cat) => {
            const active = selectedCategory === cat;
            return (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                style={{ ...s.filterBtn, ...(active ? s.filterBtnActive : {}) }}
              >
                {cat === 'all' ? (ar ? 'الكل' : 'All') : cats[cat]}
              </button>
            );
          })}
        </div>
        <p style={s.resultCount}>{filtered.length} {ar ? 'منتج' : 'items'}</p>
      </div>

      {/* ── Product Grid ── */}
      <div style={s.grid}>
        {filtered.map((p, i) => (
          <div
            key={p.id}
            style={{ ...s.card, animation: `fadeIn 0.4s ease ${i * 0.06}s both` }}
            onClick={() => onProductClick(p)}
          >
            <div style={s.cardImgWrap}>
              <img src={p.image} alt={p.name} style={s.cardImg} />
              <div style={s.badges}>
                {p.isNew && <span style={s.badgeNew}>NEW</span>}
                {p.isBestseller && <span style={s.badgeBest}>BESTSELLER</span>}
                {p.originalPrice && (
                  <span style={s.badgeSale}>
                    {Math.round((1 - p.price / p.originalPrice) * 100)}% OFF
                  </span>
                )}
              </div>
              <button
                style={s.tryBtn}
                onClick={(e) => { e.stopPropagation(); onTryOn(p); }}
              >
                {ar ? 'جرّب' : 'Try On'}
              </button>
            </div>
            <div style={s.cardBody}>
              <p style={s.cardCat}>{cats[p.category]}</p>
              <h3 style={s.cardName}>{ar ? p.nameAr : p.name}</h3>
              <div style={s.stars}>
                {Array.from({ length: Math.floor(p.rating) }).map((_, i) => (
                  <span key={i} style={{ color: GOLD, fontSize: '0.8rem' }}>★</span>
                ))}
                <span style={s.reviews}>({p.reviews})</span>
              </div>
              <div style={s.priceRow}>
                <span style={s.price}>${p.price.toLocaleString()}</span>
                {p.originalPrice && (
                  <span style={s.origPrice}>${p.originalPrice.toLocaleString()}</span>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* ── Footer ── */}
      <footer style={s.footer}>
        <span style={{ fontSize: '1.4rem', color: GOLD }}>🌿</span>
        <span style={s.footerText}>{ar ? 'حيث تلتقي الأناقة بالطبيعة في كل تفصيلة.' : 'Where elegance meets nature in every detail.'}</span>
      </footer>
    </div>
  );
};

const s: Record<string, React.CSSProperties> = {
  page: { minHeight: '100vh', background: CREAM, fontFamily: "'Inter', sans-serif" },

  // Nav
  nav: { background: G_DARK, height: 64, display: 'flex', alignItems: 'center', position: 'sticky', top: 0, zIndex: 100 },
  navInner: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', maxWidth: 1240, margin: '0 auto', padding: '0 40px' },
  logo: { display: 'flex', alignItems: 'center', gap: 8 },
  logoLeaf: { fontSize: '1.4rem', color: GOLD },
  logoText: { fontFamily: "'Playfair Display', serif", fontSize: '1.25rem', fontWeight: 700, color: GOLD, letterSpacing: 3 },
  navLinks: { display: 'flex', gap: 36, alignItems: 'center' },
  navLink: { color: 'rgba(255,255,255,0.75)', textDecoration: 'none', fontSize: '0.9rem' },
  navLinkActive: { color: WHITE, textDecoration: 'underline', textUnderlineOffset: 5, fontSize: '0.9rem', fontWeight: 500 },
  navIcons: { display: 'flex', gap: 20 },
  navIcon: { color: WHITE, fontSize: '1.2rem', cursor: 'pointer' },
  langBtn: { background: 'transparent', border: `1.5px solid ${GOLD}`, color: GOLD, borderRadius: 6, padding: '4px 10px', fontSize: '0.78rem', fontWeight: 700, cursor: 'pointer', letterSpacing: 1 },

  // Hero section
  heroSection: {
    display: 'flex',
    gap: 24,
    padding: '32px 40px',
    maxWidth: 1240,
    margin: '0 auto',
    alignItems: 'stretch',
  },
  heroLeft: { flex: '0 0 58%', display: 'flex', flexDirection: 'column', background: WHITE, borderRadius: 20, overflow: 'hidden', boxShadow: '0 4px 24px rgba(45,74,45,0.1)' },
  heroImgCard: { position: 'relative', flex: '1 1 auto', minHeight: 340, background: '#EAE4D8', overflow: 'hidden' },
  heroImg: { width: '100%', height: '100%', objectFit: 'cover', display: 'block' },
  heroDots: { position: 'absolute', bottom: 16, left: 0, right: 0, display: 'flex', justifyContent: 'center', gap: 8 },
  dot: { width: 10, height: 10, borderRadius: '50%', border: 'none', background: 'rgba(255,255,255,0.55)', cursor: 'pointer', padding: 0 },
  dotActive: { background: WHITE, transform: 'scale(1.2)' },
  heroMeta: { padding: '24px 32px 32px', textAlign: 'center' },
  heroName: { fontFamily: "'Playfair Display', serif", fontSize: '2rem', fontWeight: 700, color: TEXT_DARK, margin: '0 0 10px' },
  heroDesc: { color: TEXT_MED, fontSize: '0.95rem', lineHeight: 1.65, margin: '0 0 22px' },
  heroBtn: { background: G_DARK, color: WHITE, border: 'none', padding: '13px 36px', borderRadius: 50, fontSize: '0.95rem', fontWeight: 500, cursor: 'pointer', letterSpacing: 0.3 },

  // Side cards
  heroRight: { flex: '1 1 42%', display: 'flex', flexDirection: 'column', gap: 16 },
  sideCard: { flex: '1 1 0', background: WHITE, borderRadius: 20, overflow: 'hidden', cursor: 'pointer', boxShadow: '0 2px 16px rgba(45,74,45,0.09)', display: 'flex', flexDirection: 'column' },
  sideImgWrap: { position: 'relative', width: '100%', flex: '1 1 100px', background: '#EAE4D8', overflow: 'hidden', minHeight: 120 },
  sideImg: { width: '100%', height: '100%', objectFit: 'cover', display: 'block' },
  goldRibbon: { position: 'absolute', top: 0, right: 0, width: 44, height: 44, background: GOLD, clipPath: 'polygon(0 0, 100% 0, 100% 100%)' },
  sideBody: { padding: '12px 16px 16px' },
  sideName: { fontFamily: "'Playfair Display', serif", fontSize: '1rem', fontWeight: 600, color: TEXT_DARK, margin: '0 0 5px' },
  sideDesc: { color: '#7A8E7A', fontSize: '0.78rem', lineHeight: 1.5, margin: '0 0 12px' },
  sideBtn: { background: G_DARK, color: WHITE, border: 'none', padding: '9px 0', borderRadius: 50, fontSize: '0.82rem', fontWeight: 500, cursor: 'pointer', width: '100%' },

  // Filters
  filterWrap: { maxWidth: 1240, margin: '0 auto', padding: '20px 40px 8px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 },
  filterRow: { display: 'flex', gap: 10, flexWrap: 'wrap', justifyContent: 'center' },
  filterBtn: { padding: '9px 24px', borderRadius: 25, border: `1.5px solid #C8B896`, background: WHITE, color: TEXT_MED, fontSize: '0.85rem', fontWeight: 500 },
  filterBtnActive: { background: G_DARK, color: WHITE, border: `1.5px solid ${G_DARK}`, fontWeight: 600 },
  resultCount: { fontSize: '0.8rem', color: '#9AA89A' },

  // Grid
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: 20, padding: '16px 40px 48px', maxWidth: 1240, margin: '0 auto' },

  // Card
  card: { borderRadius: 18, overflow: 'hidden', background: WHITE, boxShadow: '0 2px 14px rgba(45,74,45,0.07)', cursor: 'pointer', border: `1px solid rgba(200,184,150,0.35)` },
  cardImgWrap: { position: 'relative', width: '100%', aspectRatio: '1', overflow: 'hidden', background: '#EAE4D8' },
  cardImg: { width: '100%', height: '100%', objectFit: 'cover', transition: 'transform 0.4s' },
  badges: { position: 'absolute', top: 10, left: 10, display: 'flex', gap: 6, flexWrap: 'wrap' },
  badgeNew: { background: G_DARK, color: WHITE, padding: '3px 10px', borderRadius: 12, fontSize: '0.62rem', fontWeight: 700, letterSpacing: 1 },
  badgeBest: { background: GOLD, color: WHITE, padding: '3px 10px', borderRadius: 12, fontSize: '0.62rem', fontWeight: 700, letterSpacing: 1 },
  badgeSale: { background: '#C0553A', color: WHITE, padding: '3px 10px', borderRadius: 12, fontSize: '0.62rem', fontWeight: 700, letterSpacing: 1 },
  tryBtn: { position: 'absolute', bottom: 12, right: 12, padding: '7px 20px', borderRadius: 25, border: 'none', background: G_DARK, color: WHITE, fontSize: '0.78rem', fontWeight: 600, boxShadow: '0 4px 12px rgba(45,74,45,0.35)' },
  cardBody: { padding: '13px 15px 17px' },
  cardCat: { fontSize: '0.68rem', fontWeight: 600, color: G_MID, textTransform: 'uppercase', letterSpacing: 1.5, marginBottom: 4 },
  cardName: { fontFamily: "'Playfair Display', serif", fontSize: '1rem', fontWeight: 600, color: TEXT_DARK, margin: '0 0 6px' },
  stars: { display: 'flex', alignItems: 'center', gap: 2, marginBottom: 8 },
  reviews: { fontSize: '0.73rem', color: '#9AA89A', marginLeft: 4 },
  priceRow: { display: 'flex', alignItems: 'center', gap: 10 },
  price: { fontSize: '1.05rem', fontWeight: 700, color: G_DARK },
  origPrice: { fontSize: '0.85rem', color: '#B0ADA8', textDecoration: 'line-through' },

  // Footer
  footer: { background: G_DARK, padding: '28px 24px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 14 },
  footerText: { color: 'rgba(255,255,255,0.82)', fontSize: '0.95rem', fontStyle: 'italic' },
};
