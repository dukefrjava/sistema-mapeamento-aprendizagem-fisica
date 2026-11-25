/* Menu lateral único (ES5). Coloque <aside class="sidebar" id="menu"></aside> na página,
   carregue este arquivo DEPOIS de simaf.js e antes do script da página. */
var Menu = (function () {
  function ic(p) {
    return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + p + '</svg>';
  }
  var ICONES = {
    dash: ic('<rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/>'),
    cam: ic('<path d="M4 8h3l2-3h6l2 3h3v11H4z"/><circle cx="12" cy="13" r="3.5"/>'),
    pasta: ic('<path d="M3 7h7l2 2h9v10H3z"/>'),
    seta: ic('<path d="M7 10l5 5 5-5"/>')
  };
  var CADASTROS = [
    ['turmas', 'Turmas'], ['alunos', 'Alunos'], ['avaliacoes', 'Avaliações'], ['questoes', 'Questões'],
    ['concepcoes', 'Concepções alternativas'], ['categorias', 'Categorias conceituais'], ['dimensoes', 'Dimensões ACE']
  ];
  var aberto = false;

  function el(tag, classe, texto) {
    var e = document.createElement(tag);
    if (classe) { e.className = classe; }
    if (texto !== undefined && texto !== null) { e.textContent = texto; }
    return e;
  }
  function link(href, icone, texto) {
    var a = el('a');
    a.href = href;
    if (icone) { var s = el('span', 'mi'); s.innerHTML = icone; a.appendChild(s); }
    a.appendChild(el('span', '', texto));
    return a;
  }
  function paginaAtual() {
    var p = location.pathname;
    if (p.indexOf('cadastros') >= 0) { return 'cadastros'; }
    if (p.indexOf('scanner') >= 0) { return 'scanner'; }
    return 'dashboard';
  }
  function abaAtual() { return location.hash.replace('#', '') || 'turmas'; }

  function montar() {
    var raiz = document.getElementById('menu');
    if (!raiz) { return; }
    var pag = paginaAtual();
    raiz.innerHTML = '';

    var marca = link('/dashboard.html', null, 'SIMAF');
    marca.className = 'brand';
    var img = el('img'); img.src = '/img/mascote.png'; img.alt = ''; img.width = 28; img.height = 28;
    marca.insertBefore(img, marca.firstChild);
    raiz.appendChild(marca);

    var d = link('/dashboard.html', ICONES.dash, 'Dashboard'); if (pag === 'dashboard') { d.className = 'ativo'; }
    var c = link('/scanner.html', ICONES.cam, 'Ler prova'); if (pag === 'scanner') { c.className = 'ativo'; }
    raiz.appendChild(d); raiz.appendChild(c);

    // grupo Cadastros: botão que abre/fecha a lista de todas as opções
    if (pag === 'cadastros') { aberto = true; }
    var g = link('/cadastros.html', ICONES.pasta, 'Cadastros');
    g.className = 'grupo' + (pag === 'cadastros' ? ' ativo' : '');
    var sp = el('span', 'seta'); sp.innerHTML = ICONES.seta; g.appendChild(sp);
    g.onclick = function () {
      if (pag === 'cadastros') { aberto = !aberto; montar(); return false; }
      aberto = !aberto; montar(); return false;
    };
    raiz.appendChild(g);

    if (aberto) {
      var sub = el('div', 'sub'), i, a;
      for (i = 0; i < CADASTROS.length; i++) {
        a = el('a', '', CADASTROS[i][1]);
        a.href = '/cadastros.html#' + CADASTROS[i][0];
        if (pag === 'cadastros' && abaAtual() === CADASTROS[i][0]) { a.className = 'ativo'; }
        sub.appendChild(a);
      }
      raiz.appendChild(sub);
    }

    var rodape = el('div', 'rodape');
    var nome = el('div', '', Simaf.usuario() || 'Professor(a)'); nome.id = 'nomeProf';
    var sair = el('a', '', 'Sair'); sair.id = 'sair'; sair.href = '#';
    sair.onclick = function () { Simaf.sair(); return false; };
    rodape.appendChild(nome); rodape.appendChild(sair);
    raiz.appendChild(rodape);
  }

  if (window.addEventListener) { window.addEventListener('hashchange', montar, false); }
  montar();
  return { montar: montar };
}());
