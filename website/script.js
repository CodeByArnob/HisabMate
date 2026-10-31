document.addEventListener('DOMContentLoaded', () => {
  // Interactive Hero Light / Dark Dashboard Switcher
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

  // Screenshot Gallery Category Filter
  const filterChips = document.querySelectorAll('[data-gallery-filter]');
  const shotCards = document.querySelectorAll('[data-shot-category]');

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
});
