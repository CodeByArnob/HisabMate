document.addEventListener('DOMContentLoaded', () => {
  // 1. Mobile Navigation Drawer Toggle
  const mobileMenuBtn = document.getElementById('mobile-menu-btn');
  const mobileDrawer = document.getElementById('mobile-nav-drawer');
  const mobileNavLinks = document.querySelectorAll('.mobile-nav-link');

  if (mobileMenuBtn && mobileDrawer) {
    mobileMenuBtn.addEventListener('click', () => {
      const isOpen = mobileDrawer.classList.toggle('open');
      mobileMenuBtn.setAttribute('aria-expanded', String(isOpen));
    });

    mobileNavLinks.forEach((link) => {
      link.addEventListener('click', () => {
        mobileDrawer.classList.remove('open');
        mobileMenuBtn.setAttribute('aria-expanded', 'false');
      });
    });
  }

  // 2. Website Light / Dark Theme Switcher
  const siteThemeToggle = document.getElementById('site-theme-toggle');
  const siteThemeIcon = document.getElementById('site-theme-icon');
  const htmlRoot = document.documentElement;

  if (siteThemeToggle) {
    siteThemeToggle.addEventListener('click', () => {
      const current = htmlRoot.getAttribute('data-site-theme') || 'dark';
      const next = current === 'dark' ? 'light' : 'dark';
      htmlRoot.setAttribute('data-site-theme', next);
      if (siteThemeIcon) {
        siteThemeIcon.textContent = next === 'dark' ? '☀️' : '🌙';
      }
    });
  }

  // 3. Interactive Hero Preview Switcher (Light / Dark / Motion Reel / FX Engine)
  const themeButtons = document.querySelectorAll('[data-hero-theme]');
  const heroImage = document.getElementById('hero-dashboard-img');

  themeButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      themeButtons.forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      const targetSrc = btn.getAttribute('data-img-src');
      const targetAlt = btn.getAttribute('data-img-alt');
      if (heroImage && targetSrc) {
        heroImage.src = targetSrc;
        if (targetAlt) heroImage.alt = targetAlt;
      }
    });
  });

  // 4. Interactive 14-Currency ISO 4217 Offline FX Simulator
  const fxCatalog = {
    BDT: { symbol: '৳', rateToBdt: 1.0, decimals: 2 },
    USD: { symbol: '$', rateToBdt: 120.5, decimals: 2 },
    EUR: { symbol: '€', rateToBdt: 130.2, decimals: 2 },
    GBP: { symbol: '£', rateToBdt: 152.8, decimals: 2 },
    INR: { symbol: '₹', rateToBdt: 1.43, decimals: 2 },
    JPY: { symbol: '¥', rateToBdt: 0.79, decimals: 0 },
    CNY: { symbol: '¥', rateToBdt: 16.7, decimals: 2 },
    SAR: { symbol: 'ر.س ', rateToBdt: 32.1, decimals: 2 },
    AED: { symbol: 'د.إ ', rateToBdt: 32.8, decimals: 2 },
    CAD: { symbol: 'CA$', rateToBdt: 87.5, decimals: 2 },
    AUD: { symbol: 'A$', rateToBdt: 79.2, decimals: 2 },
    SGD: { symbol: 'S$', rateToBdt: 90.1, decimals: 2 },
    MYR: { symbol: 'RM', rateToBdt: 27.4, decimals: 2 },
    KWD: { symbol: 'د.ك ', rateToBdt: 392.0, decimals: 3 }
  };

  function formatWithGrouping(value, decimals, groupingMode) {
    const fixed = Number(value || 0).toFixed(decimals);
    const parts = fixed.split('.');
    let intPart = parts[0];
    const decPart = parts[1] ? '.' + parts[1] : '';

    if (groupingMode === 'south_asian' && intPart.length > 3) {
      const lastThree = intPart.slice(-3);
      const otherNumbers = intPart.slice(0, -3);
      intPart = otherNumbers.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + ',' + lastThree;
    } else {
      intPart = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }
    return intPart + decPart;
  }

  const fxAmountInput = document.getElementById('fx-amount');
  const fxFromSelect = document.getElementById('fx-from');
  const fxToSelect = document.getElementById('fx-to');
  const fxGroupingSelect = document.getElementById('fx-grouping');
  const fxSourceFormatted = document.getElementById('fx-source-formatted');
  const fxTargetFormatted = document.getElementById('fx-target-formatted');
  const fxRateCaption = document.getElementById('fx-rate-caption');

  function updateFxSimulator() {
    if (!fxAmountInput || !fxFromSelect || !fxToSelect || !fxGroupingSelect) return;
    const rawAmount = parseFloat(fxAmountInput.value) || 0;
    const fromCode = fxFromSelect.value;
    const toCode = fxToSelect.value;
    const grouping = fxGroupingSelect.value;

    const fromMeta = fxCatalog[fromCode] || fxCatalog.BDT;
    const toMeta = fxCatalog[toCode] || fxCatalog.USD;

    const amountInBdt = rawAmount * fromMeta.rateToBdt;
    const converted = amountInBdt / toMeta.rateToBdt;
    const crossRate = fromMeta.rateToBdt / toMeta.rateToBdt;

    if (fxSourceFormatted) {
      fxSourceFormatted.textContent = `Source: ${fromMeta.symbol}${formatWithGrouping(rawAmount, fromMeta.decimals, grouping)} ${fromCode}`;
    }
    if (fxTargetFormatted) {
      fxTargetFormatted.textContent = `${toMeta.symbol}${formatWithGrouping(converted, toMeta.decimals, grouping)} ${toCode}`;
    }
    if (fxRateCaption) {
      fxRateCaption.textContent = `Offline Cross Rate: 1 ${fromCode} = ${crossRate.toFixed(4)} ${toCode} (1 ${toCode} = ৳${toMeta.rateToBdt.toFixed(2)} BDT)`;
    }
  }

  [fxAmountInput, fxFromSelect, fxToSelect, fxGroupingSelect].forEach((el) => {
    if (el) {
      el.addEventListener('input', updateFxSimulator);
      el.addEventListener('change', updateFxSimulator);
    }
  });
  updateFxSimulator();

  // 5. Screenshot Gallery Category Filter & Lightbox Modal
  const filterChips = document.querySelectorAll('[data-gallery-filter]');
  const shotCards = document.querySelectorAll('[data-shot-category]');
  const lightboxModal = document.getElementById('lightbox-modal');
  const lightboxImg = document.getElementById('lightbox-img');
  const lightboxTitle = document.getElementById('lightbox-title');
  const lightboxDesc = document.getElementById('lightbox-desc');
  const lightboxClose = document.getElementById('lightbox-close');

  filterChips.forEach((chip) => {
    chip.addEventListener('click', () => {
      filterChips.forEach((c) => c.classList.remove('active'));
      chip.classList.add('active');
      const selected = chip.getAttribute('data-gallery-filter');

      shotCards.forEach((card) => {
        const category = card.getAttribute('data-shot-category');
        if (selected === 'all' || category === selected) {
          card.style.display = 'flex';
        } else {
          card.style.display = 'none';
        }
      });
    });
  });

  shotCards.forEach((card) => {
    card.addEventListener('click', () => {
      const img = card.querySelector('img');
      const title = card.querySelector('h3');
      const desc = card.querySelector('p');
      if (lightboxModal && lightboxImg && img) {
        lightboxImg.src = img.src;
        lightboxImg.alt = img.alt;
        if (lightboxTitle && title) lightboxTitle.textContent = title.textContent;
        if (lightboxDesc && desc) lightboxDesc.textContent = desc.textContent;
        lightboxModal.classList.add('active');
      }
    });
  });

  if (lightboxClose && lightboxModal) {
    lightboxClose.addEventListener('click', () => {
      lightboxModal.classList.remove('active');
    });
    lightboxModal.addEventListener('click', (e) => {
      if (e.target === lightboxModal) {
        lightboxModal.classList.remove('active');
      }
    });
  }

  // 6. IntersectionObserver Scroll Reveal
  const revealElements = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.08 }
    );
    revealElements.forEach((el) => observer.observe(el));
  } else {
    revealElements.forEach((el) => el.classList.add('visible'));
  }
});
