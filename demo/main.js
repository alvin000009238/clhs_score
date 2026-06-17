import anime from 'animejs';

// ===== i18n Translations =====
const translations = {
  zh: {
    'nav.brand': '壢中成績',
    'hero.title': '壢中成績 app',
    'hero.subtitle': '更聰明的方式，查看你的成績',
    'hero.desc': '一鍵掌握班排與科目表現，為壢中學生量身設計。',
    'hero.cta': '下載最新版本',
    'hero.cta.sub': '適用於 Android 10+',
    'features.title': '功能亮點',
    'feature.login.title': '使用欣河智慧校園平台直接登入',
    'feature.login.desc': '內嵌學校系統登入頁面，登入快速且便利。',
    'feature.overview.title': '一眼掌握全局',
    'feature.overview.desc': '加權平均、班排、類排、百分比，搭配優勢與待加強科目摘要。',
    'feature.analysis.tag': '智慧分析',
    'feature.analysis.title': '深度洞察與建議',
    'feature.analysis.desc': '基於成績走勢提供個人化的學習建議，幫助你精準掌握強弱項，規劃未來的讀書方向。',
    'feature.subjects.title': '每科都看得透徹',
    'feature.subjects.desc': '各科成績與班平均的差距、五標落點、分數分布，還有與上次考試的比較。',
    'feature.simulator.title': '試算你的目標成績',
    'feature.simulator.desc': '拖動滑桿調整各科分數，即時計算調整後的加權平均，規劃你的讀書策略。',
    'feature.trend.title': '追蹤你的進步軌跡',
    'feature.trend.desc': '自動比對同學期歷次考試，清楚看到平均與排名的變化趨勢。',
    'feature.line_graph.title': '視覺化歷次成績',
    'feature.line_graph.desc': '透過折線圖直觀呈現各科成績走勢，支援多科目同時比較，成績起伏一目了然。',
    'feature.timetable.title': '輕鬆查看課表',
    'feature.timetable.desc': '隨時隨地查看每日課表，掌握上課節次與科目。',
    'feature.more.title': '更多功能',
    'feature.more.desc': '持續開發增加中，敬請期待...',
    'privacy.title': '隱私與安全',
    'privacy.nopassword.title': '不保存密碼',
    'privacy.nopassword.desc': '你的密碼只在登入時使用，不會被儲存在任何地方。',
    'privacy.nobackend.title': '無後端伺服器',
    'privacy.nobackend.desc': 'App 不會連線到任何我們維護的伺服器，只會直接與欣河系統連線抓取資料。',
    'privacy.localonly.title': '本機端處理',
    'privacy.localonly.desc': '所有的成績資料與分析都在你的手機本機端直接處理，絕不備份或上傳至雲端。',
    'privacy.logout.title': '登出即清除',
    'privacy.logout.desc': '登出時自動清除本機 session 資料，不留痕跡。',
    'disclaimer.title': '免責聲明',
    'disclaimer.text': '本專案為非官方開發之第三方服務，與壢中及欣河智慧校園平台無任何直接關聯。',
    'footer.github': '在 GitHub 上查看原始碼',
    'footer.license': 'MIT License © 2026',
    'footer.contributor': '由 alvin000009238 開發維護',
    'bottom.cta.title': '準備好開始了嗎？',
    'bottom.cta.desc': '立即下載，體驗最流暢的成績查詢方式。'
  },
  en: {
    'nav.brand': 'CLHS Score',
    'hero.title': 'CLHS Score app',
    'hero.subtitle': 'A smarter way to check your grades',
    'hero.desc': 'Instantly view class rank and subject performance. Built for CLHS students.',
    'hero.cta': 'Download Latest',
    'hero.cta.sub': 'Requires Android 10+',
    'features.title': 'Features',
    'feature.login.title': 'Login with ShinHer Smart Campus',
    'feature.login.desc': 'Embedded school login page for fast and convenient login experience.',
    'feature.overview.title': 'Everything at a glance',
    'feature.overview.desc': 'Weighted average, class & stream rankings, percentile, strength & weakness summary.',
    'feature.analysis.tag': 'Smart Analysis',
    'feature.analysis.title': 'Deep Insights & Suggestions',
    'feature.analysis.desc': 'Personalized learning suggestions based on your grade trends, helping you identify strengths and weaknesses to plan your future study direction.',
    'feature.subjects.title': 'Deep dive into every subject',
    'feature.subjects.desc': 'Score gaps vs. class average, five-point benchmarks, distribution charts, and comparison with previous exams.',
    'feature.simulator.title': 'Simulate your target grades',
    'feature.simulator.desc': 'Drag sliders to adjust scores per subject and instantly calculate the new weighted average.',
    'feature.trend.title': 'Track your progress',
    'feature.trend.desc': 'Automatically compares exams across the semester to visualize average and rank trends.',
    'feature.line_graph.title': 'Visualize Grade Trends',
    'feature.line_graph.desc': 'Intuitively display grade trends for each subject through line graphs, supporting multi-subject comparisons at a glance.',
    'feature.timetable.title': 'Check Timetable Easily',
    'feature.timetable.desc': 'View your daily timetable anytime, anywhere. Keep track of class periods and subjects.',
    'feature.more.title': 'More Features',
    'feature.more.desc': 'More features are continually being added, stay tuned...',
    'privacy.title': 'Privacy & Security',
    'privacy.nopassword.title': 'No password storage',
    'privacy.nopassword.desc': 'Your password is only used during login and is never stored anywhere.',
    'privacy.nobackend.title': 'No Backend Server',
    'privacy.nobackend.desc': 'The app does not connect to any servers maintained by us. It connects directly and exclusively to the ShinHer system.',
    'privacy.localonly.title': '100% Local Processing',
    'privacy.localonly.desc': 'All grade data and analytics are processed directly on your device and are never uploaded to any cloud database.',
    'privacy.logout.title': 'Clean logout',
    'privacy.logout.desc': 'All local session data is wiped on logout, leaving no trace.',
    'disclaimer.title': 'Disclaimer',
    'disclaimer.text': 'This is an unofficial third-party project and is not directly affiliated with CLHS or ShinHer Smart Campus.',
    'footer.github': 'View source on GitHub',
    'footer.license': 'MIT License © 2026',
    'footer.contributor': 'Developed by alvin000009238',
    'bottom.cta.title': 'Ready to get started?',
    'bottom.cta.desc': 'Download now for the smoothest grade checking experience.'
  },
};

// ===== Language Toggle =====
let currentLang = localStorage.getItem('demo-lang') || 'zh';

function setLanguage(lang) {
  const safeLang = lang === 'en' ? 'en' : 'zh';
  currentLang = safeLang;
  localStorage.setItem('demo-lang', safeLang);
  document.documentElement.lang = safeLang === 'zh' ? 'zh-Hant' : 'en';

  const langStrings = safeLang === 'zh' ? translations.zh : translations.en;
  const langMap = new Map(Object.entries(langStrings));
  document.querySelectorAll('[data-i18n]').forEach((el) => {
    const key = el.getAttribute('data-i18n');
    if (langMap.has(key)) {
      el.textContent = langMap.get(key);
    }
  });

  const toggle = document.getElementById('lang-toggle');
  if (toggle) {
    toggle.textContent = lang === 'zh' ? 'EN' : '中文';
  }

  updateDownloadStatsText();
}

// ===== Fetch Download Stats =====
let releaseData = null;
async function initDownloadStats() {
  try {
    const response = await fetch('https://api.github.com/repos/alvin000009238/clhs_score/releases');
    if (response.ok) {
      const data = await response.json();
      if (data.length >= 2) {
        releaseData = {
          latest: {
            tag: data[0].tag_name,
            count: data[0].assets.reduce((sum, asset) => sum + asset.download_count, 0)
          },
          prev: {
            tag: data[1].tag_name,
            count: data[1].assets.reduce((sum, asset) => sum + asset.download_count, 0)
          }
        };
        updateDownloadStatsText();
        const statsEl = document.getElementById('download-stats');
        if (statsEl) statsEl.style.opacity = 1;
      }
    }
  } catch (err) {
    console.error('Failed to fetch stats:', err);
  }
}

function updateDownloadStatsText() {
  if (!releaseData) return;
  const latestEl = document.getElementById('stat-latest');
  const prevEl = document.getElementById('stat-prev');
  if (!latestEl || !prevEl) return;

  if (currentLang === 'zh') {
    latestEl.textContent = `最新版 (${releaseData.latest.tag}): ${releaseData.latest.count} 次下載`;
    prevEl.textContent = `上一版 (${releaseData.prev.tag}): ${releaseData.prev.count} 次下載`;
  } else {
    latestEl.textContent = `Latest (${releaseData.latest.tag}): ${releaseData.latest.count} dl`;
    prevEl.textContent = `Previous (${releaseData.prev.tag}): ${releaseData.prev.count} dl`;
  }
}

// ===== Reduced Motion Check =====
const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

// ===== Anime.js: Hero Phone Float =====
function initHeroPhone() {
  if (prefersReducedMotion) return;

  const phone = document.getElementById('hero-phone');
  if (!phone) return;

  anime({
    targets: phone,
    translateY: [-10, 10],
    duration: 4000,
    direction: 'alternate',
    loop: true,
    easing: 'easeInOutSine'
  });
}

// ===== Anime.js: Hero Content Entrance =====
function initHeroContent() {
  const heroContent = document.querySelector('.hero-content');
  if (!heroContent) return;

  if (prefersReducedMotion) {
    heroContent.style.opacity = 1;
    return;
  }

  heroContent.style.opacity = 1;
  anime({
    targets: '.hero-content > *',
    opacity: [0, 1],
    translateY: [24, 0],
    duration: 900,
    delay: anime.stagger(150, { start: 200 }),
    easing: 'easeOutCubic'
  });
}

// ===== Scroll Reveal Animations (IntersectionObserver + Anime.js) =====
function initScrollAnimations() {
  if (prefersReducedMotion) {
    // Make everything visible immediately
    document.querySelectorAll('.fade-in').forEach((el) => {
      el.style.opacity = 1;
    });
    return;
  }

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          anime({
            targets: entry.target,
            opacity: [0, 1],
            translateY: [28, 0],
            duration: 800,
            easing: 'easeOutCubic'
          });
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0 }
  );

  document.querySelectorAll('.fade-in').forEach((el) => {
    el.style.opacity = 0;
    observer.observe(el);
  });
}

// ===== Nav Background Effect (IntersectionObserver, no scroll listener) =====
function initNavObserver() {
  const nav = document.getElementById('nav');
  const hero = document.getElementById('hero');
  if (!nav || !hero) return;

  const observer = new IntersectionObserver(
    ([entry]) => {
      if (entry.isIntersecting) {
        nav.classList.remove('scrolled');
      } else {
        nav.classList.add('scrolled');
      }
    },
    { threshold: 0, rootMargin: '-64px 0px 0px 0px' }
  );

  observer.observe(hero);
}

// ===== Init =====
document.addEventListener('DOMContentLoaded', () => {
  setLanguage(currentLang);
  initHeroPhone();
  initHeroContent();
  initScrollAnimations();
  initNavObserver();
  initDownloadStats();

  const toggle = document.getElementById('lang-toggle');
  if (toggle) {
    toggle.addEventListener('click', () => {
      setLanguage(currentLang === 'zh' ? 'en' : 'zh');
    });
  }

  // Scroll to top when clicking nav brand
  const navBrand = document.querySelector('.nav-brand');
  if (navBrand) {
    navBrand.addEventListener('click', () => {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    });
  }
});
