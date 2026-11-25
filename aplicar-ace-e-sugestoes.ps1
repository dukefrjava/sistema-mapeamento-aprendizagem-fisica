<#
  SIMAF - aplica, em um banco que JA tem o inventario termico cadastrado, as dimensoes ACE das questoes
  e as sugestoes de intervencao das concepcoes (dados de termico-pt.json). Pode ser repetido.
  Precisa do Java com o InstrumentoAjusteController (rotas PUT novas) ja publicado.
  Uso:  .\aplicar-ace-e-sugestoes.ps1 -BaseUrl https://SEU-JAVA.onrender.com -Email voce@escola.com
  Salve este arquivo como UTF-8 com BOM (ja vem assim).
#>
param(
  [string]$BaseUrl = 'http://localhost:8080',
  [Parameter(Mandatory = $true)][string]$Email,
  [string]$Senha,
  [string]$Arquivo = 'teste-termico/termico.json'
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
    Write-Host "ERRO em $Metodo $Caminho : $($_.ErrorDetails.Message) $($_.Exception.Message)" -ForegroundColor Red
    throw
  }
}

Write-Host '1/3 Dimensoes ACE' -ForegroundColor Cyan
$dims = @{}
$existentes = @(Api GET '/dimensoes')
foreach ($d in $dados.dimensoes) {
  $x = $existentes | Where-Object { $_.codigo -eq $d.codigo } | Select-Object -First 1
  if (-not $x) { $x = Api POST '/dimensoes' @{ codigo = $d.codigo; tipoRaciocinio = $d.tipoRaciocinio; complexidade = $d.complexidade }; Write-Host "  criada: $($d.codigo)" -ForegroundColor Green }
  else { Write-Host "  (ja existia) $($d.codigo)" -ForegroundColor DarkGray }
  $dims[$d.codigo] = $x
}

Write-Host '2/3 Questoes -> dimensao' -ForegroundColor Cyan
$questoes = @(Api GET '/questoes')
$ok = 0
foreach ($q in $dados.questoes) {
  if (-not $q.dimensao) { continue }
  $alvo = $questoes | Where-Object { $_.numero -eq $q.numero -and $_.instrumento -like 'Yeo*' } | Select-Object -First 1
  if (-not $alvo) { Write-Host "  questao $($q.numero) nao encontrada" -ForegroundColor Yellow; continue }
  Api PUT "/questoes/$($alvo.id)/dimensao" @{ id = $dims[$q.dimensao].id } | Out-Null
  $ok++
}
Write-Host "  $ok questoes atualizadas" -ForegroundColor Green

Write-Host '3/3 Concepcoes -> sugestao de intervencao' -ForegroundColor Cyan
$concs = @(Api GET '/concepcoes')
$ok = 0
foreach ($c in $dados.concepcoes) {
  $alvo = $concs | Where-Object { $_.descricao -eq $c.descricao } | Select-Object -First 1
  if (-not $alvo) { Write-Host "  concepcao $($c.codigo) nao encontrada" -ForegroundColor Yellow; continue }
  Api PUT "/concepcoes/$($alvo.id)/sugestao" @{ sugestaoIntervencao = $c.sugestao } | Out-Null
  $ok++
}
Write-Host "  $ok concepcoes atualizadas" -ForegroundColor Green
Write-Host 'Pronto. Recarregue o dashboard (Ctrl+F5).' -ForegroundColor Cyan
