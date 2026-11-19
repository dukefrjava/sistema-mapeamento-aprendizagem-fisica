/* Login e chamadas à API (ES5). A senha fica só na sessionStorage: some ao fechar a aba. */
var Simaf = (function () {
  var K = 'simaf.auth', U = 'simaf.user';
  function lerS(k) { try { return sessionStorage.getItem(k); } catch (e) { return null; } }
  function gravarS(k, v) { try { sessionStorage.setItem(k, v); } catch (e) {} }
  function bruto(m, u, corpo, auth, ok, erro) {
    var x = new XMLHttpRequest();
    x.open(m, u, true);
    if (auth) { x.setRequestHeader('Authorization', auth); }
    if (corpo && !(typeof FormData !== 'undefined' && corpo instanceof FormData)) {
      x.setRequestHeader('Content-Type', 'application/json');
      corpo = JSON.stringify(corpo);
    }
    x.onreadystatechange = function () {
      if (x.readyState !== 4) { return; }
      var d = null;
      try { d = JSON.parse(x.responseText); } catch (e) { d = null; }
      if (x.status >= 200 && x.status < 300) { ok(d); } else { erro(x.status, d); }
    };
    x.send(corpo || null);
  }
  function sair() {
    try { sessionStorage.removeItem(K); sessionStorage.removeItem(U); } catch (e) {}
    location.href = '/login.html';
  }
  return {
    bruto: bruto,
    usuario: function () { return lerS(U) || ''; },
    exigir: function () { if (!lerS(K)) { location.href = '/login.html'; } },
    sair: sair,
    ajax: function (m, u, corpo, ok, erro) {
      bruto(m, u, corpo, lerS(K), ok, function (s, d) { if (s === 401) { sair(); return; } erro(s, d); });
    },
    entrar: function (email, senha, ok, erro) {
      var t = 'Basic ' + btoa(unescape(encodeURIComponent(email + ':' + senha)));
      bruto('GET', '/auth/eu', null, t, function (d) { gravarS(K, t); gravarS(U, d.usuario); ok(d); }, erro);
    }
  };
}());
