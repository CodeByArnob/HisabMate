/**
 * HisabMate – World-Class Fintech Experience
 * Powered by GSAP 3.12+, ScrollTrigger, Three.js (r128), and Lottie Web.
 */

document.addEventListener('DOMContentLoaded', () => {
  const isReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // ==========================================
  // 1. Scroll Progress Indicator & Nav Handling
  // ==========================================
  const scrollProgressBar = document.getElementById('scroll-progress-bar');
  window.addEventListener('scroll', () => {
    const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
    if (totalHeight > 0 && scrollProgressBar) {
      const progress = window.scrollY / totalHeight;
      scrollProgressBar.style.transform = `scaleX(${progress})`;
    }
  }, { passive: true });

  // Mobile Drawer Toggle
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

  // Light / Dark Theme Switcher
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
      // Notify Three.js scene of theme color updates if active
      if (window.__hisabmateThreeUpdateTheme) {
        window.__hisabmateThreeUpdateTheme(next);
      }
    });
  }

  // ==========================================
  // 2. Three.js Interactive 3D Hero Experience
  // ==========================================
  const heroCanvas = document.getElementById('hero-three-canvas');
  if (heroCanvas && typeof THREE !== 'undefined' && !isReducedMotion) {
    try {
      const scene = new THREE.Scene();
      const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 1000);
      camera.position.z = 24;

      const renderer = new THREE.WebGLRenderer({
        canvas: heroCanvas,
        alpha: true,
        antialias: true,
        powerPreference: 'high-performance'
      });
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

      function resizeThree() {
        const parent = heroCanvas.parentElement;
        const width = parent.clientWidth || window.innerWidth;
        const height = parent.clientHeight || 600;
        camera.aspect = width / height;
        camera.updateProjectionMatrix();
        renderer.setSize(width, height, false);
      }
      resizeThree();
      window.addEventListener('resize', resizeThree, { passive: true });

      // Ambient & Directional Lights
      const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
      scene.add(ambientLight);

      const dirLight = new THREE.DirectionalLight(0x7c5cff, 1.4);
      dirLight.position.set(10, 15, 12);
      scene.add(dirLight);

      const pointLight = new THREE.PointLight(0x10b981, 1.6, 50);
      pointLight.position.set(-10, -5, 10);
      scene.add(pointLight);

      const cyanPointLight = new THREE.PointLight(0x0ea5e9, 1.2, 40);
      cyanPointLight.position.set(5, -10, 8);
      scene.add(cyanPointLight);

      // Create 3D Currency Tokens
      const tokenSymbols = ['৳', '$', '€', '£', '¥', '₹'];
      const tokenColors = [0x7c5cff, 0x10b981, 0x0ea5e9, 0xf59e0b, 0xec4899, 0x8b5cf6];
      const tokensGroup = new THREE.Group();
      scene.add(tokensGroup);

      const coinGeo = new THREE.CylinderGeometry(1.6, 1.6, 0.3, 32);

      // Helper to generate dynamic canvas texture with crisp currency glyph
      function createCoinTexture(symbol, colorHex) {
        const cvs = document.createElement('canvas');
        cvs.width = 256;
        cvs.height = 256;
        const ctx = cvs.getContext('2d');

        // Background disc
        ctx.fillStyle = '#0f172a';
        ctx.beginPath();
        ctx.arc(128, 128, 120, 0, Math.PI * 2);
        ctx.fill();

        // Rim border
        ctx.strokeStyle = colorHex;
        ctx.lineWidth = 14;
        ctx.stroke();

        // Inner ring
        ctx.strokeStyle = 'rgba(255,255,255,0.2)';
        ctx.lineWidth = 4;
        ctx.beginPath();
        ctx.arc(128, 128, 100, 0, Math.PI * 2);
        ctx.stroke();

        // Glyphs
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 96px "Space Grotesk", sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(symbol, 128, 134);

        const tex = new THREE.CanvasTexture(cvs);
        tex.needsUpdate = true;
        return tex;
      }

      const coinObjects = [];
      const positions = [
        { x: -14, y: 5, z: -4, speed: 0.012, rotX: 0.008, rotY: 0.015 },
        { x: -10, y: -7, z: -2, speed: 0.010, rotX: 0.012, rotY: 0.009 },
        { x: 13, y: 7, z: -5, speed: 0.014, rotX: 0.009, rotY: 0.014 },
        { x: 15, y: -5, z: -3, speed: 0.011, rotX: 0.014, rotY: 0.008 },
        { x: -5, y: 9, z: -8, speed: 0.009, rotX: 0.010, rotY: 0.011 },
        { x: 6, y: -9, z: -6, speed: 0.013, rotX: 0.007, rotY: 0.016 }
      ];

      tokenSymbols.forEach((sym, idx) => {
        const tex = createCoinTexture(sym, '#' + tokenColors[idx].toString(16).padStart(6, '0'));
        const faceMat = new THREE.MeshStandardMaterial({
          map: tex,
          metalness: 0.85,
          roughness: 0.25
        });
        const edgeMat = new THREE.MeshStandardMaterial({
          color: tokenColors[idx],
          metalness: 0.9,
          roughness: 0.2
        });

        const mesh = new THREE.Mesh(coinGeo, [edgeMat, faceMat, faceMat]);
        const pos = positions[idx];
        mesh.position.set(pos.x, pos.y, pos.z);
        mesh.rotation.set(Math.random() * Math.PI, Math.random() * Math.PI, 0);

        tokensGroup.add(mesh);
        coinObjects.push({ mesh, pos, seed: idx * 1.5 });
      });

      // Mouse & Pointer Parallax
      let mouseX = 0;
      let mouseY = 0;
      let targetX = 0;
      let targetY = 0;

      window.addEventListener('mousemove', (e) => {
        mouseX = (e.clientX / window.innerWidth - 0.5) * 2;
        mouseY = (e.clientY / window.innerHeight - 0.5) * 2;
      }, { passive: true });

      // Render Loop with Viewport Visibility Observer (Battery & GPU Saver)
      let animationFrameId = null;
      let isHeroVisible = true;
      let clock = new THREE.Clock();

      function animate() {
        if (!isHeroVisible) return;
        animationFrameId = requestAnimationFrame(animate);

        const elapsedTime = clock.getElapsedTime();

        // Parallax damping
        targetX += (mouseX - targetX) * 0.05;
        targetY += (mouseY - targetY) * 0.05;

        tokensGroup.rotation.y = targetX * 0.25;
        tokensGroup.rotation.x = -targetY * 0.25;

        // Floating dynamic wave physics for coins
        coinObjects.forEach((item) => {
          const { mesh, pos, seed } = item;
          mesh.rotation.x += pos.rotX;
          mesh.rotation.y += pos.rotY;
          mesh.position.y = pos.y + Math.sin(elapsedTime * 1.2 + seed) * 0.8;
          mesh.position.x = pos.x + Math.cos(elapsedTime * 0.8 + seed) * 0.4;
        });

        renderer.render(scene, camera);
      }

      // IntersectionObserver: auto-pause rendering when hero scrolls out of view
      const heroSection = document.querySelector('.hero');
      if ('IntersectionObserver' in window && heroSection) {
        const heroObserver = new IntersectionObserver((entries) => {
          entries.forEach((entry) => {
            isHeroVisible = entry.isIntersecting;
            if (isHeroVisible) {
              clock.start();
              animate();
            } else if (animationFrameId) {
              cancelAnimationFrame(animationFrameId);
            }
          });
        }, { threshold: 0.05 });
        heroObserver.observe(heroSection);
      } else {
        animate();
      }

      window.__hisabmateThreeUpdateTheme = (theme) => {
        if (theme === 'light') {
          ambientLight.intensity = 0.9;
        } else {
          ambientLight.intensity = 0.7;
        }
      };
    } catch (e) {
      console.warn('Three.js initialization safely bypassed:', e);
    }
  }

  // ==========================================
  // 3. GSAP 3.12+ & ScrollTrigger Animations
  // ==========================================
  if (typeof gsap !== 'undefined') {
    if (typeof ScrollTrigger !== 'undefined') {
      gsap.registerPlugin(ScrollTrigger);
    }

    if (!isReducedMotion) {
      // Hero Entrance Timeline
      const heroTl = gsap.timeline({ defaults: { ease: 'power3.out' } });

      heroTl
        .from('.eyebrow', { opacity: 0, y: -16, duration: 0.6 })
        .from('.hero-content h1', { opacity: 0, y: 24, duration: 0.8 }, '-=0.4')
        .from('.hero-lead', { opacity: 0, y: 18, duration: 0.7 }, '-=0.5')
        .from('.hero-cta .btn', { opacity: 0, scale: 0.94, stagger: 0.1, duration: 0.5 }, '-=0.4')
        .from('.release-quick-meta span', { opacity: 0, y: 10, stagger: 0.08, duration: 0.4 }, '-=0.3')
        .from('.metric-item', { opacity: 0, y: 20, stagger: 0.12, duration: 0.6 }, '-=0.3')
        .from('.phone-frame', { opacity: 0, scale: 0.92, rotationY: 8, duration: 0.9 }, '-=0.7')
        .from('.float-card', { opacity: 0, y: 16, stagger: 0.15, duration: 0.6 }, '-=0.5');

      // Floating Cards subtle levitation
      gsap.to('.float-card:nth-child(1)', {
        y: -6,
        duration: 3,
        repeat: -1,
        yoyo: true,
        ease: 'sine.inOut'
      });
      gsap.to('.float-card:nth-child(2)', {
        y: 6,
        duration: 3.4,
        repeat: -1,
        yoyo: true,
        ease: 'sine.inOut',
        delay: 0.5
      });

      // ScrollTrigger Stagger Reveals for Sections
      if (typeof ScrollTrigger !== 'undefined') {
        gsap.utils.toArray('.gsap-section').forEach((sec) => {
          gsap.from(sec.querySelectorAll('.section-header, .feature-card, .download-card, .fin-demo-card, .shot-card, .arch-panel'), {
            scrollTrigger: {
              trigger: sec,
              start: 'top 82%',
              toggleActions: 'play none none none'
            },
            opacity: 0,
            y: 30,
            duration: 0.6,
            stagger: 0.08,
            ease: 'power2.out'
          });
        });

        // Numeric Counter Animations on scroll
        const countElements = document.querySelectorAll('[data-target-count]');
        countElements.forEach((el) => {
          const target = parseFloat(el.getAttribute('data-target-count')) || 0;
          const prefix = el.getAttribute('data-prefix') || '';
          const suffix = el.getAttribute('data-suffix') || '';
          const decimals = parseInt(el.getAttribute('data-decimals') || '0', 10);

          ScrollTrigger.create({
            trigger: el,
            start: 'top 90%',
            once: true,
            onEnter: () => {
              gsap.to({ val: 0 }, {
                val: target,
                duration: 1.8,
                ease: 'power2.out',
                onUpdate: function () {
                  el.textContent = `${prefix}${this.targets()[0].val.toFixed(decimals)}${suffix}`;
                }
              });
            }
          });
        });
      }
    }
  }

  // ==========================================
  // 4. Interactive Financial Analytics & Chart Studio
  // ==========================================
  const analyticsData = {
    '6m': {
      labels: ['May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct'],
      inflow: [210000, 230000, 245000, 220000, 260000, 285000],
      outflow: [120000, 140000, 135000, 150000, 128000, 138000],
      totals: { inflow: '৳14,50,000', outflow: '৳8,11,000', surplus: '+৳6,39,000' }
    },
    '30d': {
      labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
      inflow: [65000, 70000, 75000, 75000],
      outflow: [32000, 38000, 34000, 34000],
      totals: { inflow: '৳2,85,000', outflow: '৳1,38,000', surplus: '+৳1,47,000' }
    },
    'weekly': {
      labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      inflow: [12000, 15000, 8000, 18000, 25000, 9000, 14000],
      outflow: [4500, 6200, 3800, 7100, 12000, 5400, 4800],
      totals: { inflow: '৳1,01,000', outflow: '৳43,800', surplus: '+৳57,200' }
    }
  };

  const chartTabButtons = document.querySelectorAll('.chart-tab-btn');
  const chartInflowPath = document.getElementById('chart-inflow-path');
  const chartOutflowPath = document.getElementById('chart-outflow-path');
  const chartAreaInflow = document.getElementById('chart-area-inflow');
  const chartAreaOutflow = document.getElementById('chart-area-outflow');
  const chartTooltip = document.getElementById('chart-tooltip');
  const kpiInflow = document.getElementById('kpi-inflow');
  const kpiOutflow = document.getElementById('kpi-outflow');
  const kpiSurplus = document.getElementById('kpi-surplus');
  const chartLabelsGroup = document.getElementById('chart-labels-group');

  function renderChart(periodKey) {
    const data = analyticsData[periodKey] || analyticsData['6m'];
    if (kpiInflow) kpiInflow.textContent = data.totals.inflow;
    if (kpiOutflow) kpiOutflow.textContent = data.totals.outflow;
    if (kpiSurplus) kpiSurplus.textContent = data.totals.surplus;

    const width = 600;
    const height = 220;
    const padX = 40;
    const padY = 30;
    const maxVal = Math.max(...data.inflow, ...data.outflow) * 1.15;
    const count = data.labels.length;
    const stepX = (width - padX * 2) / (count - 1);

    function getCoords(values) {
      return values.map((val, idx) => {
        const x = padX + idx * stepX;
        const y = height - padY - (val / maxVal) * (height - padY * 2);
        return { x, y, val };
      });
    }

    const inflowCoords = getCoords(data.inflow);
    const outflowCoords = getCoords(data.outflow);

    function buildSmoothPath(coords) {
      if (!coords.length) return '';
      let path = `M ${coords[0].x} ${coords[0].y}`;
      for (let i = 0; i < coords.length - 1; i++) {
        const p0 = coords[i];
        const p1 = coords[i + 1];
        const cx1 = p0.x + (p1.x - p0.x) / 2;
        const cy1 = p0.y;
        const cx2 = p0.x + (p1.x - p0.x) / 2;
        const cy2 = p1.y;
        path += ` C ${cx1} ${cy1}, ${cx2} ${cy2}, ${p1.x} ${p1.y}`;
      }
      return path;
    }

    const inflowLine = buildSmoothPath(inflowCoords);
    const outflowLine = buildSmoothPath(outflowCoords);
    const bottomY = height - padY;

    const inflowArea = `${inflowLine} L ${inflowCoords[inflowCoords.length - 1].x} ${bottomY} L ${inflowCoords[0].x} ${bottomY} Z`;
    const outflowArea = `${outflowLine} L ${outflowCoords[outflowCoords.length - 1].x} ${bottomY} L ${outflowCoords[0].x} ${bottomY} Z`;

    if (chartInflowPath) chartInflowPath.setAttribute('d', inflowLine);
    if (chartOutflowPath) chartOutflowPath.setAttribute('d', outflowLine);
    if (chartAreaInflow) chartAreaInflow.setAttribute('d', inflowArea);
    if (chartAreaOutflow) chartAreaOutflow.setAttribute('d', outflowArea);

    // Update X-axis labels in SVG
    if (chartLabelsGroup) {
      chartLabelsGroup.innerHTML = '';
      data.labels.forEach((lbl, idx) => {
        const x = padX + idx * stepX;
        const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        text.setAttribute('x', String(x));
        text.setAttribute('y', String(height - 8));
        text.setAttribute('text-anchor', 'middle');
        text.setAttribute('fill', 'var(--text-muted)');
        text.setAttribute('font-size', '11');
        text.setAttribute('font-weight', '600');
        text.textContent = lbl;
        chartLabelsGroup.appendChild(text);
      });
    }

    // GSAP path drawing transition
    if (typeof gsap !== 'undefined' && !isReducedMotion && chartInflowPath && chartOutflowPath) {
      gsap.fromTo([chartInflowPath, chartOutflowPath],
        { strokeDasharray: 800, strokeDashoffset: 800 },
        { strokeDashoffset: 0, duration: 1.2, ease: 'power2.out' }
      );
    }
  }

  chartTabButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      chartTabButtons.forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      const period = btn.getAttribute('data-chart-period');
      renderChart(period);
    });
  });
  renderChart('6m');

  // Chart Hover Scrubber Tooltip
  const chartSvgStage = document.getElementById('chart-svg-stage');
  const chartScrubber = document.getElementById('chart-scrubber');
  if (chartSvgStage && chartTooltip && chartScrubber) {
    chartSvgStage.addEventListener('mousemove', (e) => {
      const rect = chartSvgStage.getBoundingClientRect();
      const relX = e.clientX - rect.left;
      const width = rect.width;
      const activePeriod = document.querySelector('.chart-tab-btn.active')?.getAttribute('data-chart-period') || '6m';
      const curData = analyticsData[activePeriod];
      const count = curData.labels.length;
      const index = Math.min(Math.max(Math.round(((relX - 40) / (width - 80)) * (count - 1)), 0), count - 1);

      const label = curData.labels[index];
      const inf = curData.inflow[index];
      const outf = curData.outflow[index];
      const sur = inf - outf;

      chartTooltip.style.opacity = '1';
      chartTooltip.innerHTML = `<strong>${label}</strong><br><span style="color:var(--accent-emerald);">Inflow: ৳${inf.toLocaleString()}</span><br><span style="color:var(--accent-rose);">Outflow: ৳${outf.toLocaleString()}</span><br><span style="color:var(--accent-cyan); font-weight:700;">Net: +৳${sur.toLocaleString()}</span>`;

      chartScrubber.setAttribute('x1', String((index / (count - 1)) * 520 + 40));
      chartScrubber.setAttribute('x2', String((index / (count - 1)) * 520 + 40));
      chartScrubber.style.opacity = '0.8';
    });

    chartSvgStage.addEventListener('mouseleave', () => {
      chartTooltip.style.opacity = '0';
      chartScrubber.style.opacity = '0';
    });
  }

  // ==========================================
  // 5. Interactive Budget & Savings Pots Studio
  // ==========================================
  const budgetSlider = document.getElementById('budget-slider');
  const budgetFill = document.getElementById('budget-fill');
  const budgetPercentLabel = document.getElementById('budget-percent-label');
  const budgetSpentLabel = document.getElementById('budget-spent-label');
  const budgetRemainingLabel = document.getElementById('budget-remaining-label');
  const budgetStatusBadge = document.getElementById('budget-status-badge');

  const budgetTotal = 50000;

  function updateBudgetMeter() {
    if (!budgetSlider) return;
    const currentSpent = parseFloat(budgetSlider.value) || 0;
    const ratio = (currentSpent / budgetTotal) * 100;
    const remaining = budgetTotal - currentSpent;

    if (budgetFill) {
      budgetFill.style.width = `${Math.min(ratio, 100)}%`;
      if (ratio > 100) {
        budgetFill.style.background = 'linear-gradient(90deg, #f59e0b, #f43f5e)';
      } else if (ratio > 80) {
        budgetFill.style.background = 'linear-gradient(90deg, #10b981, #f59e0b)';
      } else {
        budgetFill.style.background = 'linear-gradient(90deg, var(--accent-emerald), var(--accent-cyan))';
      }
    }

    if (budgetPercentLabel) budgetPercentLabel.textContent = `${Math.round(ratio)}%`;
    if (budgetSpentLabel) budgetSpentLabel.textContent = `৳${currentSpent.toLocaleString()} Spent`;
    if (budgetRemainingLabel) {
      if (remaining >= 0) {
        budgetRemainingLabel.textContent = `৳${remaining.toLocaleString()} Left`;
        budgetRemainingLabel.style.color = 'var(--text-muted)';
      } else {
        budgetRemainingLabel.textContent = `৳${Math.abs(remaining).toLocaleString()} Over`;
        budgetRemainingLabel.style.color = 'var(--accent-rose)';
      }
    }

    if (budgetStatusBadge) {
      if (ratio > 100) {
        budgetStatusBadge.textContent = 'Overbudget Alert';
        budgetStatusBadge.className = 'fin-card-badge';
        budgetStatusBadge.style.background = 'var(--accent-rose-soft)';
        budgetStatusBadge.style.color = 'var(--accent-rose)';
      } else if (ratio >= 80) {
        budgetStatusBadge.textContent = '80% Warning Threshold';
        budgetStatusBadge.className = 'fin-card-badge';
        budgetStatusBadge.style.background = 'var(--accent-amber-soft)';
        budgetStatusBadge.style.color = 'var(--accent-amber)';
      } else {
        budgetStatusBadge.textContent = 'On Track (Safe)';
        budgetStatusBadge.className = 'fin-card-badge';
        budgetStatusBadge.style.background = 'var(--accent-emerald-soft)';
        budgetStatusBadge.style.color = 'var(--accent-emerald)';
      }
    }
  }

  if (budgetSlider) {
    budgetSlider.addEventListener('input', updateBudgetMeter);
    updateBudgetMeter();
  }

  // Interactive Savings Goal Pot Micro-Interaction
  const savingsDepositBtn = document.getElementById('savings-deposit-btn');
  const savingsRingCircle = document.getElementById('savings-ring-circle');
  const savingsCurrentLabel = document.getElementById('savings-current-label');
  const savingsPercentBadge = document.getElementById('savings-percent-badge');
  const coinZone = document.getElementById('coin-particle-zone');

  let currentSavings = 365000;
  const targetSavings = 500000;

  function updateSavingsRing() {
    const ratio = Math.min(currentSavings / targetSavings, 1);
    const circumference = 2 * Math.PI * 38; // r=38 in SVG
    const offset = circumference - ratio * circumference;

    if (savingsRingCircle) {
      savingsRingCircle.style.strokeDashoffset = String(offset);
    }
    if (savingsPercentBadge) {
      savingsPercentBadge.textContent = `${Math.round(ratio * 100)}%`;
    }
    if (savingsCurrentLabel) {
      savingsCurrentLabel.textContent = `৳${currentSavings.toLocaleString()} of ৳5,00,000`;
    }
  }
  updateSavingsRing();

  if (savingsDepositBtn) {
    savingsDepositBtn.addEventListener('click', () => {
      currentSavings = Math.min(currentSavings + 25000, targetSavings);
      updateSavingsRing();

      // Elegant subtle pulse on progress ring
      if (typeof gsap !== 'undefined' && savingsRingCircle && !isReducedMotion) {
        gsap.fromTo(savingsRingCircle, { scale: 1.05 }, { scale: 1, duration: 0.35, ease: 'power2.out' });
      }
    });
  }

  // ==========================================
  // 6. Feature icon rendering
  // Icons are inline SVG in the HTML so they never depend on a third-party
  // animation CDN. A small CSS motion treatment is applied in styles.css.
  // ==========================================

  // ==========================================
  // 7. Interactive 14-Currency ISO 4217 FX Simulator
  // ==========================================
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
  const fxSwapBtn = document.getElementById('fx-swap-btn');
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

  if (fxSwapBtn && fxFromSelect && fxToSelect) {
    fxSwapBtn.addEventListener('click', () => {
      const temp = fxFromSelect.value;
      fxFromSelect.value = fxToSelect.value;
      fxToSelect.value = temp;
      updateFxSimulator();
    });
  }

  [fxAmountInput, fxFromSelect, fxToSelect, fxGroupingSelect].forEach((el) => {
    if (el) {
      el.addEventListener('input', updateFxSimulator);
      el.addEventListener('change', updateFxSimulator);
    }
  });
  updateFxSimulator();

  // ==========================================
  // 8. Screenshot Gallery 3D Tilt & Lightbox
  // ==========================================
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

  // 3D Perspective Tilt on Mouse Movement over Cards
  if (!isReducedMotion) {
    shotCards.forEach((card) => {
      card.addEventListener('mousemove', (e) => {
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left - rect.width / 2;
        const y = e.clientY - rect.top - rect.height / 2;
        const rotX = -(y / (rect.height / 2)) * 6;
        const rotY = (x / (rect.width / 2)) * 6;
        card.style.transform = `perspective(800px) rotateX(${rotX}deg) rotateY(${rotY}deg) translateY(-4px)`;
      });

      card.addEventListener('mouseleave', () => {
        card.style.transform = 'perspective(800px) rotateX(0deg) rotateY(0deg) translateY(0)';
      });
    });
  }

  // Open Lightbox
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
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && lightboxModal.classList.contains('active')) {
        lightboxModal.classList.remove('active');
      }
    });
  }

  // Hero Preview Theme Switcher (Light / Dark / Motion Reel / FX Engine)
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

  // Checksum Copy Buttons
  document.querySelectorAll('.copy-btn').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const textToCopy = btn.getAttribute('data-copy-text');
      if (textToCopy && navigator.clipboard) {
        navigator.clipboard.writeText(textToCopy).then(() => {
          const original = btn.textContent;
          btn.textContent = 'Copied!';
          setTimeout(() => { btn.textContent = original; }, 2000);
        });
      }
    });
  });
});
