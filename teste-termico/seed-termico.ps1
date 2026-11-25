<#
  SIMAF - cadastra o Inventário de Conceitos Térmicos (Yeo e Zadnik), com o gabarito (Quadro 20) e as
  concepções alternativas por opção de resposta (Quadro 19) da dissertação de Louzada.
  Lê os dados de termico.json (mesma pasta). Pode ser repetido: o que já existe é reaproveitado.
  Uso:  .\seed-termico.ps1 -Email voce@escola.com
        .\seed-termico.ps1 -Email voce@escola.com -Arquivo termico-pt.json   (versão em português)
        .\seed-termico.ps1 -BaseUrl https://SEU-JAVA.onrender.com -Email voce@escola.com
  Salve este arquivo como UTF-8 com BOM (já vem assim).
#>
param(
  [string]$BaseUrl = 'http://localhost:8080',
  [Parameter(Mandatory = $true)][string]$Email,
  [string]$Senha,
  [string]$Arquivo = 'termico.json'
)
$ErrorActionPreference = 'Stop'
if ('é'.Length -ne 1) { throw 'Acentos corrompidos: salve como UTF-8 com BOM.' }
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$BaseUrl = $BaseUrl.TrimEnd('/')
if (-not $Senha) {
  $sec = Read-Host "Senha de $Email" -AsSecureString
  $Senha = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}
$auth = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${Email}:${Senha}"))
$dados = Get-Content -Raw -Encoding UTF8 (Join-Path $PSScriptRoot $Arquivo) | ConvertFrom-Json

function Api([string]$Metodo, [string]$Caminho, $Corpo = $null) {
  $p = @{ Method = $Metodo; Uri = "$BaseUrl$Caminho"; Headers = @{ Authorization = $auth }; TimeoutSec = 180 }
  if ($null -ne $Corpo) {
    $p.ContentType = 'application/json; charset=utf-8'
    $p.Body = [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject $Corpo -Depth 8 -Compress))
  }
  try { $resp = Invoke-RestMethod @p; return $resp }
  catch {
    $msg = $_.ErrorDetails.Message
    Write-Host "ERRO em $Metodo $Caminho : $msg $($_.Exception.Message)" -ForegroundColor Red
    if ($null -ne $Corpo) { Write-Host "Corpo enviado: $(ConvertTo-Json -InputObject $Corpo -Depth 8 -Compress)" -ForegroundColor Yellow }
    throw
  }
}
function Garantir([string]$Caminho, [string]$Campo, $Valor, $Corpo, [string]$Rotulo) {
  $achado = @(Api GET $Caminho) | Where-Object { $_.$Campo -eq $Valor } | Select-Object -First 1
  if ($achado) { Write-Host "  (já existia) $Rotulo" -ForegroundColor DarkGray; return $achado }
  $novo = Api POST $Caminho $Corpo
  Write-Host "  criado: $Rotulo" -ForegroundColor Green
  return $novo
}
function Ref($o) { @{ id = $o.id } }

Write-Host '1/5 Login' -ForegroundColor Cyan
$eu = Api GET '/auth/eu'; Write-Host "  conectada como $($eu.usuario)" -ForegroundColor Green
$prof = @(Api GET '/professores') | Where-Object { $_.email -eq $Email } | Select-Object -First 1
if (-not $prof) { throw "Professor $Email não encontrado" }

Write-Host '2/5 Categorias (dimensões A a D) e concepções' -ForegroundColor Cyan
$cats = @{}
foreach ($c in $dados.categorias) { $cats[$c.codigo] = Garantir '/categorias' 'codigo' $c.codigo @{ codigo = $c.codigo; nome = $c.nome } "categoria $($c.codigo)" }
$concs = @{}
foreach ($c in $dados.concepcoes) {
  $concs[$c.codigo] = Garantir '/concepcoes' 'descricao' $c.descricao @{ descricao = $c.descricao; sugestaoIntervencao = $c.sugestao; categorias = @(Ref $cats[$c.categoria]) } "concepção $($c.codigo)"
}

$dims = @{}
foreach ($d in $dados.dimensoes) { $dims[$d.codigo] = Garantir '/dimensoes' 'codigo' $d.codigo @{ codigo = $d.codigo; tipoRaciocinio = $d.tipoRaciocinio; complexidade = $d.complexidade } "dimensão ACE $($d.codigo)" }

Write-Host '3/5 Questões e avaliação' -ForegroundColor Cyan
$av = @(Api GET '/avaliacoes') | Where-Object { $_.titulo -eq $dados.titulo } | Select-Object -First 1
if ($av) { Write-Host "  (já existia) avaliação id $($av.id)" -ForegroundColor DarkGray }
else {
  $refs = @()
  foreach ($q in $dados.questoes) {
    $alts = @()
    foreach ($a in $q.alternativas) {
      $alts += @{ letra = $a.letra; texto = $a.texto; correta = [bool]$a.correta; concepcoes = @($a.concepcoes | ForEach-Object { Ref $concs[$_] }) }
    }
    $corpoQ = @{ numero = [int]$q.numero; enunciado = $q.enunciado; instrumento = 'Yeo e Zadnik (2001)'; tipo = 'Múltipla escolha'; descartada = [bool]$q.descartada; alternativas = $alts }
    if ($q.dimensao -and $dims[$q.dimensao]) { $corpoQ.dimensao = Ref $dims[$q.dimensao] }
    $nova = Api POST '/questoes' $corpoQ
    $refs += (Ref $nova)
  }
  Write-Host "  $($refs.Count) questões criadas" -ForegroundColor Green
  $av = Api POST '/avaliacoes' @{ titulo = $dados.titulo; data = (Get-Date).ToString('yyyy-MM-dd'); fase = $dados.fase; professor = (Ref $prof); questoes = $refs }
  Write-Host "  avaliação criada (id $($av.id))" -ForegroundColor Green
}

Write-Host '4/5 Alunos de teste' -ForegroundColor Cyan
$turma = @(Api GET '/turmas') | Select-Object -First 1
foreach ($m in 'cientifica', 'ingenua', 'mista') {
  $mat = @{ cientifica = '2026301'; ingenua = '2026302'; mista = '2026303' }[$m]
  $corpo = @{ matricula = $mat; email = "aluno$mat@escola.com" }
  if ($turma) { $corpo.turma = Ref $turma }
  Garantir '/alunos' 'matricula' $mat $corpo "aluno $mat ($m)" | Out-Null
}

Write-Host '5/5 Pronto' -ForegroundColor Cyan
Write-Host "  Em $BaseUrl/scanner.html escolha a avaliação '$($dados.titulo)' e envie, em PDF:"
Write-Host '    aluno 2026301 -> prova-marcada-cientifica.pdf   (esperado 24/24 = 100,0%)'
Write-Host '    aluno 2026302 -> prova-marcada-ingenua.pdf      (esperado  2/24 =   8,3%)'
Write-Host '    aluno 2026303 -> prova-marcada-mista.pdf        (esperado 14/24 =  58,3%)'