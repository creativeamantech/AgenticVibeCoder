window.__agentBridge = {
  getPageText: () => document.body?.innerText ?? '',
  getTitle: () => document.title,
  getUrl: () => window.location.href,
  getLinks: () => Array.from(document.links).map(l => ({text: l.innerText.trim(), href: l.href, id: l.id})),
  getInputs: () => Array.from(document.querySelectorAll('input,textarea,select')).map(el => ({type: el.type, name: el.name, id: el.id, placeholder: el.placeholder, value: el.value})),
  clickByCss: (sel) => { const el = document.querySelector(sel); if(el){el.click();return true;}return false; },
  fillByCss: (sel, val) => { const el = document.querySelector(sel); if(el){el.value=val;el.dispatchEvent(new Event('input',{bubbles:true}));return true;}return false; },
  scrollTo: (x,y) => window.scrollTo(x,y),
  waitForSelector: (sel, ms) => new Promise(res => { const t = Date.now(); const i = setInterval(()=>{ if(document.querySelector(sel)){clearInterval(i);res(true);}else if(Date.now()-t>ms){clearInterval(i);res(false);}},100); })
};
