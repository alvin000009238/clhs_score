import anime from 'animejs';

// ===== i18n Translations =====
const translations = {
  zh: {
    'nav.brand': '壢中 Pocket',
    'hero.title': '壢中 Pocket',
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
    'faq.title': '常見問題',
    'faq.official.question': '壢中 Pocket 是學校官方 App 嗎？與中大壢中或欣河智慧校園有合作關係嗎？',
    'faq.official.answer': '不是。壢中 Pocket 是學生獨立開發的非官方第三方 App，與壢中及欣河智慧校園平台無任何直接關聯。',
    'faq.password.question': 'App 會取得、儲存或傳送我的帳號密碼嗎？',
    'faq.password.answer': 'App 不會讀取或保存你輸入的密碼；帳號密碼由內嵌的學校登入頁面透過 HTTPS 直接送到校務系統。登入成功後，App 會取得學號與查詢所需的登入狀態保存在你的裝置上，不會傳送給開發者。',
    'faq.session.question': 'App 會保存哪些登入資料？登入狀態、Cookies 和驗證權杖會如何處理？',
    'faq.session.answer': '為維持登入，App 會在手機上保存學號、Cookies 與驗證權杖，並使用 Android 系統金鑰加密。啟用生物辨識時會再以 PIN 與硬體金鑰保護；啟用段考提醒功能時會另存最長 48 小時的加密臨時登入狀態。登出會清除這些 Session 資料。',
    'faq.personal.question': '我的成績、課表、學號和其他個人資料會被上傳嗎？開發者能看到這些資料嗎？',
    'faq.personal.answer': '成績、課表、姓名、班級、座號與學號只在你的手機和學校系統之間處理，不會傳送到開發者伺服器，開發者也無法從 App 遠端查看。App 可能會將不包含上述個人資料的功能使用事件與計數傳送至 Firebase Analytics，用於了解功能使用情況與改善 App，且不會設定可識別個人的使用者 ID。',
    'faq.freshness.question': 'App 顯示的是即時資料還是本機快取？如何重新取得最新資料？',
    'faq.freshness.answer': 'App 會優先顯示上次成功查詢的本機快取，因此不保證是最新資料。請在成績頁下拉重新整理，即可略過快取並向學校系統查詢。',
    'faq.source.question': '五標、標準差與各分數區間人數等資料來自哪裡？',
    'faq.source.answer': '這些資料均來自學校系統回傳的同一份成績資料，App 僅負責整理、計算落點與繪製圖表。',
    'footer.github': '在 GitHub 上查看原始碼',
    'footer.license': 'MIT License © 2026',
    'footer.contributor': '由 alvin000009238 開發維護',
    'bottom.cta.title': '準備好開始了嗎？',
    'bottom.cta.desc': '立即下載，體驗最流暢的成績查詢方式。'
  },
  en: {
    'nav.brand': 'CLHS Pocket',
    'hero.title': 'CLHS Pocket',
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
    'faq.title': 'FAQ',
    'faq.official.question': 'Is CLHS Pocket an official school app or affiliated with CLHS or ShinHer Smart Campus?',
    'faq.official.answer': 'No. CLHS Pocket is an unofficial third-party app independently developed by a student. It has no direct affiliation with CLHS or ShinHer Smart Campus.',
    'faq.password.question': 'Does the app access, store, or transmit my username and password?',
    'faq.password.answer': 'The app does not read or store the password you enter. The embedded school login page sends your credentials directly to the school system over HTTPS. After login, the app stores your student number and the session data needed for queries on your device and does not send them to the developer.',
    'faq.session.question': 'What login data is stored, and how are sessions, cookies, and verification tokens handled?',
    'faq.session.answer': 'To keep you signed in, the app stores your student number, cookies, and verification token on your phone using Android system-key encryption. Biometric protection adds PIN and hardware-key encryption. Enabling the grade reminder feature creates a separate encrypted temporary session for up to 48 hours. Signing out clears this session data.',
    'faq.personal.question': 'Are my grades, timetable, student number, or other personal data uploaded? Can the developer see them?',
    'faq.personal.answer': 'Grades, timetables, names, classes, seat numbers, and student numbers are processed only between your phone and the school system. They are not sent to a developer server, and the developer cannot remotely view them through the app. The app may send feature-usage events and counts that do not contain this personal data to Firebase Analytics to understand feature usage and improve the app, and it does not set a personally identifiable user ID.',
    'faq.freshness.question': 'Does the app show live data or a local cache? How can I get the latest data?',
    'faq.freshness.answer': 'The app first displays the most recent successful local cache, so it may not contain the latest data. Pull down on the grades screen to bypass the cache and query the school system.',
    'faq.source.question': 'Where do the five-point benchmarks, standard deviation, and score-range counts come from?',
    'faq.source.answer': 'All of this information comes from the same set of grade data returned by the school system. The app only organizes the data, calculates your position, and draws charts.',
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
