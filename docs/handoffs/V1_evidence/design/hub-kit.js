// Spade hub, design evidence 2026-10-09. Throwaway mockup helpers: suit sprite, card rendering, state switch.
// State: open the file with #turn (default) or #showdown, or press S to toggle.

const SPRITE = `
<svg width="0" height="0" style="position:absolute" aria-hidden="true">
  <symbol id="s-spade" viewBox="0 0 100 100"><path d="M50 3C50 3 7 36 7 61c0 15 11 26 25 26 7 0 12-3 15-6l-5 16h16l-5-16c3 3 8 6 15 6 14 0 25-11 25-26C93 36 50 3 50 3Z"/></symbol>
  <symbol id="s-heart" viewBox="0 0 100 100"><path d="M50 93S5 62 5 33C5 17 17 6 31 6c9 0 15 5 19 12 4-7 10-12 19-12 14 0 26 11 26 27 0 29-45 60-45 60Z"/></symbol>
  <symbol id="s-diamond" viewBox="0 0 100 100"><path d="M50 3 87 50 50 97 13 50Z"/></symbol>
  <symbol id="s-club" viewBox="0 0 100 100"><circle cx="50" cy="27" r="21"/><circle cx="27" cy="58" r="21"/><circle cx="73" cy="58" r="21"/><path d="M50 40 56 97H44Z"/><circle cx="50" cy="55" r="11"/></symbol>
  <symbol id="mark" viewBox="0 0 100 112">
    <defs><clipPath id="mark-clip"><path d="M50 8C50 8 10 38 10 62c0 14 10 24 23 24 7 0 12-3 15-6l-5 18h14l-5-18c3 3 8 6 15 6 13 0 23-10 23-24C90 38 50 8 50 8Z"/></clipPath></defs>
    <path d="M50 8C50 8 10 38 10 62c0 14 10 24 23 24 7 0 12-3 15-6l-5 18h14l-5-18c3 3 8 6 15 6 13 0 23-10 23-24C90 38 50 8 50 8Z" fill="none" stroke="currentColor" stroke-width="6" stroke-linejoin="round"/>
    <g clip-path="url(#mark-clip)" stroke="currentColor" stroke-width="3.2" stroke-linecap="round">
      <path d="M26 58v8M33 50v24M40 40v40M47 30v56M54 36v44M61 46v28M68 52v16M75 57v6"/>
    </g>
  </symbol>
</svg>`;

const SUITS = { S: "spade", H: "heart", D: "diamond", C: "club" };
const SUIT_NAMES = { S: "spades", H: "hearts", D: "diamonds", C: "clubs" };
const RANK_NAMES = { A: "ace", K: "king", Q: "queen", J: "jack" };

function renderCards(root = document) {
  root.querySelectorAll(".pcard[data-card]").forEach((el) => {
    const code = el.dataset.card;
    const s = code.slice(-1);
    const r = code.slice(0, -1);
    const use = `<svg aria-hidden="true"><use href="#s-${SUITS[s]}"/></svg>`;
    el.classList.add(s === "H" || s === "D" ? "red" : "black");
    el.innerHTML = `<span class="idx"><b>${r}</b>${use}</span>${use.replace("<svg", '<svg class="pip"')}`; // one index only: a rotated 9 reads as a 6 from across the room
    el.setAttribute("role", "img");
    el.setAttribute("aria-label", `${RANK_NAMES[r] || r} of ${SUIT_NAMES[s]}`);
  });
}

function applyState() {
  document.body.dataset.state = location.hash === "#showdown" ? "showdown" : "turn";
}

document.body.insertAdjacentHTML("afterbegin", SPRITE);
renderCards();
applyState();
window.addEventListener("hashchange", applyState);
window.addEventListener("keydown", (e) => {
  if (e.key.toLowerCase() === "s") location.hash = document.body.dataset.state === "turn" ? "#showdown" : "#turn";
});
