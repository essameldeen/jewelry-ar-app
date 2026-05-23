import React from 'react';
import { Product, CATEGORY_LABELS, CATEGORY_LABELS_AR, RING_STYLE_COLORS } from '../data/products';
import { useLang } from '../context/LanguageContext';

interface Props {
  product: Product;
  onBack: () => void;
  onTryOn: (product: Product) => void;
}

export const ProductDetail: React.FC<Props> = ({ product, onBack, onTryOn }) => {
  const { lang } = useLang();
  const ar = lang === 'ar';
  const colors = RING_STYLE_COLORS[product.ringStyle];
  const cats = ar ? CATEGORY_LABELS_AR : CATEGORY_LABELS;

  return (
    <div style={styles.page}>
      {/* Header */}
      <div style={styles.header}>
        <button style={styles.backBtn} onClick={onBack}>
          <span style={{ fontSize: '1.1rem' }}>{ar ? '→' : '←'}</span> {ar ? 'رجوع' : 'Back'}
        </button>
      </div>

      {/* Hero Image */}
      <div style={styles.imageSection}>
        <img src={product.image} alt={product.name} style={styles.heroImage} />
        <div style={styles.imageBadges}>
          {product.isNew && <span style={styles.badgeNew}>NEW</span>}
          {product.isBestseller && <span style={styles.badgeBest}>BESTSELLER</span>}
        </div>
      </div>

      {/* Content */}
      <div style={styles.content}>
        {/* Category + Style */}
        <div style={styles.metaRow}>
          <span style={styles.category}>{cats[product.category]}</span>
          <span style={styles.styleBadge}>
            <span
              style={{
                display: 'inline-block',
                width: 10,
                height: 10,
                borderRadius: '50%',
                background: colors.primary,
                marginRight: 6,
                border: `1px solid ${colors.shadow}`,
              }}
            />
            {product.ringStyle.replace('_', ' ')}
          </span>
        </div>

        {/* Name */}
        <h1 style={styles.name}>{ar ? product.nameAr : product.name}</h1>

        {/* Rating */}
        <div style={styles.ratingRow}>
          {[1, 2, 3, 4, 5].map((star) => (
            <span
              key={star}
              style={{
                color: star <= product.rating ? '#F59E0B' : '#E5E5E5',
                fontSize: '1.2rem',
              }}
            >
              &#9733;
            </span>
          ))}
          <span style={styles.ratingText}>
            {product.rating} ({product.reviews} reviews)
          </span>
        </div>

        {/* Price */}
        <div style={styles.priceSection}>
          <span style={styles.price}>${product.price.toLocaleString()}</span>
          {product.originalPrice && (
            <>
              <span style={styles.originalPrice}>${product.originalPrice.toLocaleString()}</span>
              <span style={styles.saveBadge}>
                Save ${(product.originalPrice - product.price).toLocaleString()}
              </span>
            </>
          )}
        </div>

        {/* Description */}
        <p style={styles.description}>{ar ? product.descriptionAr : product.description}</p>

        {/* Details */}
        <div style={styles.detailsSection}>
          <h3 style={styles.detailsTitle}>{ar ? 'تفاصيل المنتج' : 'Product Details'}</h3>
          <ul style={styles.detailsList}>
            {(ar
              ? ['ذهب عيار 18 قيراطاً', 'صناعة يدوية', 'خامات لا تسبب الحساسية', 'يشمل تغليف هدايا']
              : product.details
            ).map((detail, i) => (
              <li key={i} style={styles.detailItem}>
                <span style={styles.checkIcon}>&#10003;</span>
                {detail}
              </li>
            ))}
          </ul>
        </div>

        {/* Action Buttons */}
        <div style={styles.actions}>
          <button style={styles.tryOnBtn} onClick={() => onTryOn(product)}>
            {ar ? 'جرّب بالواقع المعزز' : 'Try On with AR'}
          </button>
          <button style={styles.addCartBtn}>
            {ar ? 'أضف إلى السلة' : 'Add to Cart'}
          </button>
        </div>

        {/* Shipping info */}
        <div style={styles.infoCards}>
          <div style={styles.infoCard}>
            <span style={styles.infoIcon}>&#128666;</span>
            <div>
              <strong style={styles.infoTitle}>{ar ? 'شحن مجاني' : 'Free Shipping'}</strong>
              <p style={styles.infoText}>{ar ? 'للطلبات فوق 200$' : 'On orders over $200'}</p>
            </div>
          </div>
          <div style={styles.infoCard}>
            <span style={styles.infoIcon}>&#128260;</span>
            <div>
              <strong style={styles.infoTitle}>{ar ? 'إرجاع 30 يوماً' : '30-Day Returns'}</strong>
              <p style={styles.infoText}>{ar ? 'إرجاع بدون متاعب' : 'Hassle-free returns'}</p>
            </div>
          </div>
          <div style={styles.infoCard}>
            <span style={styles.infoIcon}>&#128142;</span>
            <div>
              <strong style={styles.infoTitle}>{ar ? 'أصالة مضمونة' : 'Authentic'}</strong>
              <p style={styles.infoText}>{ar ? 'معتمد وأصيل' : 'Certified genuine'}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    background: '#FFF9F5',
  },
  header: {
    position: 'sticky',
    top: 0,
    zIndex: 10,
    padding: '14px 20px',
    background: 'rgba(255,249,245,0.9)',
    backdropFilter: 'blur(10px)',
    borderBottom: '1px solid #F0E8E0',
  },
  backBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    padding: '8px 16px',
    borderRadius: 10,
    border: '1px solid #E5D4C0',
    background: '#fff',
    color: '#8B7355',
    fontSize: '0.9rem',
    fontWeight: 500,
  },

  // Image
  imageSection: {
    position: 'relative',
    width: '100%',
    maxWidth: 600,
    margin: '0 auto',
    aspectRatio: '1',
    background: '#F5F0EB',
  },
  heroImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  imageBadges: {
    position: 'absolute',
    top: 16,
    left: 16,
    display: 'flex',
    gap: 8,
  },
  badgeNew: {
    background: 'linear-gradient(135deg, #8B5CF6, #A78BFA)',
    color: '#fff',
    padding: '5px 14px',
    borderRadius: 14,
    fontSize: '0.75rem',
    fontWeight: 700,
    letterSpacing: 1,
  },
  badgeBest: {
    background: 'linear-gradient(135deg, #F472B6, #EC4899)',
    color: '#fff',
    padding: '5px 14px',
    borderRadius: 14,
    fontSize: '0.75rem',
    fontWeight: 700,
    letterSpacing: 1,
  },

  // Content
  content: {
    maxWidth: 600,
    margin: '0 auto',
    padding: '24px 24px 40px',
    animation: 'slideUp 0.4s ease',
  },
  metaRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 8,
  },
  category: {
    fontSize: '0.75rem',
    fontWeight: 600,
    color: '#D946EF',
    textTransform: 'uppercase',
    letterSpacing: 1.5,
  },
  styleBadge: {
    display: 'flex',
    alignItems: 'center',
    padding: '3px 12px',
    borderRadius: 12,
    background: '#F5F0EB',
    fontSize: '0.72rem',
    fontWeight: 500,
    color: '#8B7355',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  name: {
    fontFamily: "'Playfair Display', serif",
    fontSize: '2rem',
    fontWeight: 700,
    color: '#2D2D2D',
    margin: '0 0 10px',
  },
  ratingRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 4,
    marginBottom: 16,
  },
  ratingText: {
    marginLeft: 8,
    fontSize: '0.85rem',
    color: '#A0908B',
  },

  // Price
  priceSection: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 16,
    flexWrap: 'wrap',
  },
  price: {
    fontSize: '1.8rem',
    fontWeight: 700,
    color: '#2D4A2D',
  },
  originalPrice: {
    fontSize: '1.1rem',
    color: '#C0B0A0',
    textDecoration: 'line-through',
  },
  saveBadge: {
    padding: '4px 12px',
    borderRadius: 12,
    background: '#C8A84C',
    color: '#fff',
    fontSize: '0.75rem',
    fontWeight: 600,
  },

  description: {
    fontSize: '1rem',
    lineHeight: 1.7,
    color: '#6B6B6B',
    marginBottom: 24,
  },

  // Details
  detailsSection: {
    marginBottom: 28,
    padding: 20,
    borderRadius: 16,
    background: '#FAF5F0',
    border: '1px solid #F0E8E0',
  },
  detailsTitle: {
    fontFamily: "'Playfair Display', serif",
    fontSize: '1.1rem',
    fontWeight: 600,
    color: '#2D2D2D',
    marginBottom: 14,
  },
  detailsList: {
    listStyle: 'none',
    padding: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: 10,
  },
  detailItem: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    fontSize: '0.9rem',
    color: '#5B5B5B',
  },
  checkIcon: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: 22,
    height: 22,
    borderRadius: '50%',
    background: '#2D4A2D',
    color: '#fff',
    fontSize: '0.65rem',
    fontWeight: 700,
    flexShrink: 0,
  },

  // Actions
  actions: {
    display: 'flex',
    gap: 12,
    marginBottom: 24,
  },
  tryOnBtn: {
    flex: 1,
    padding: '16px 24px',
    borderRadius: 14,
    border: 'none',
    background: '#2D4A2D',
    color: '#fff',
    fontSize: '1rem',
    fontWeight: 600,
    boxShadow: '0 6px 20px rgba(45, 74, 45, 0.35)',
    letterSpacing: 0.3,
  },
  addCartBtn: {
    flex: 1,
    padding: '16px 24px',
    borderRadius: 14,
    border: '2px solid #2D4A2D',
    background: '#fff',
    color: '#2D4A2D',
    fontSize: '1rem',
    fontWeight: 600,
    letterSpacing: 0.3,
  },

  // Info cards
  infoCards: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: 12,
  },
  infoCard: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 6,
    padding: '14px 8px',
    borderRadius: 14,
    background: '#FAF5F0',
    border: '1px solid #F0E8E0',
    textAlign: 'center',
  },
  infoIcon: {
    fontSize: '1.4rem',
  },
  infoTitle: {
    fontSize: '0.75rem',
    color: '#2D2D2D',
    display: 'block',
  },
  infoText: {
    fontSize: '0.65rem',
    color: '#A0908B',
    margin: '2px 0 0',
  },
};
