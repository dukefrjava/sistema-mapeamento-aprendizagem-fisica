<#
  SIMAF - cria um cenario de demonstracao: turma, 8 alunos, dimensoes, categorias, concepcoes,
  uma prova de Fisica com respostas de 6 alunos e uma prova para ler pelo scanner.
  Pode ser repetido: o que ja existe e reaproveitado.
  Uso:  .\seed-simaf.ps1 -Email voce@escola.com
        .\seed-simaf.ps1 -BaseUrl https://SEU-JAVA.onrender.com -Email voce@escola.com
  IMPORTANTE: salve este arquivo como UTF-8 com BOM (VS Code: Salvar com codificacao > UTF-8 with BOM).
#>

param(
  [string]$BaseUrl = 'http://localhost:8080',
  [Parameter(Mandatory = $true)][string]$Email,
  [string]$Senha
)
$ErrorActionPreference = 'Stop'
if ([int][char]'é' -ne 233) { throw 'Acentos corrompidos: salve o arquivo como UTF-8 com BOM e rode de novo.' }
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$BaseUrl = $BaseUrl.TrimEnd('/')
if (-not $Senha) {
  $sec = Read-Host "Senha de $Email" -AsSecureString
  $Senha = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}
$auth = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${Email}:${Senha}"))

function Api([string]$Metodo, [string]$Caminho, $Corpo = $null) {
  $p = @{ Method = $Metodo; Uri = "$BaseUrl$Caminho"; Headers = @{ Authorization = $auth }; TimeoutSec = 180 }
  if ($null -ne $Corpo) {
    $p.ContentType = 'application/json; charset=utf-8'
    $p.Body = [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject $Corpo -Depth 8 -Compress))
  }
  try { return Invoke-RestMethod @p }
  catch {
    $msg = $_.ErrorDetails.Message
    if (-not $msg -and $_.Exception.Response) {
      try { $msg = (New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd() } catch {}
    }
    Write-Host "ERRO em $Metodo $Caminho : $msg $($_.Exception.Message)" -ForegroundColor Red
    if ($null -ne $Corpo) { Write-Host "Corpo enviado: $(ConvertTo-Json -InputObject $Corpo -Depth 8 -Compress)" -ForegroundColor Yellow }
    throw
  }
}

function Garantir([string]$Caminho, [string]$Campo, $Valor, $Corpo, [string]$Rotulo) {
  $achado = @(@(Api GET $Caminho) | Where-Object { $_.$Campo -eq $Valor })[0]
  if ($achado) { Write-Host "  (já existia) $Rotulo" -ForegroundColor DarkGray; return $achado }
  $novo = Api POST $Caminho $Corpo
  Write-Host "  criado: $Rotulo" -ForegroundColor Green
  return $novo
}

function Ref($o) {
  if ($null -eq $o) { return $null }
  $val = $o
  if ($val -is [System.Collections.IDictionary] -or ($val.PSObject -and $val.PSObject.Properties['id'])) {
    $val = $val.id
  }
  while ($val -is [System.Array] -or ($val -is [System.Collections.IEnumerable] -and -not ($val -is [string]))) {
    $val = @($val)[0]
  }
  if ($val -is [System.Collections.IDictionary] -or ($val.PSObject -and $val.PSObject.Properties['id'])) {
    $val = $val.id
  }
  return @{ id = $val }
}

function Alt([string]$l, [string]$t, [bool]$ok, $conc = @()) {
  @{ letra = $l; texto = $t; correta = $ok; concepcoes = @($conc | ForEach-Object { @{ id = $_.id } }) }
}
function NovaQuestao($n, $enun, $dim, $alts) {
  $corpo = @{ numero = $n; enunciado = $enun; instrumento = 'Prova de demonstração'; tipo = 'Múltipla escolha'
              descartada = $false; alternativas = @($alts) }
  if ($dim) { $corpo.dimensao = Ref $dim }
  $q = Api POST '/questoes' $corpo
  $mapa = @{}; foreach ($a in $q.alternativas) { $mapa[$a.letra] = $a.id }
  Write-Host "  questão $n criada" -ForegroundColor Green
  return @{ obj = $q; mapa = $mapa }
}

Write-Host '1/7 Login' -ForegroundColor Cyan
$eu = Api GET '/auth/eu'
Write-Host "  conectada como $($eu.usuario)" -ForegroundColor Green
$listaProfs = @(Api GET '/professores')
$prof = $listaProfs | Where-Object { $_.email -eq $Email } | Select-Object -First 1
if (-not $prof) { throw "Professor $Email não encontrado em /professores" }
$profId = (@($prof.id)[0])

Write-Host '2/7 Turma e alunos' -ForegroundColor Cyan
$turma = Garantir '/turmas' 'codigo' 'FIS-9A' @{ codigo = 'FIS-9A'; anoLetivo = 2026; serie = '9º ano'; professor = @{ id = $profId } } 'turma FIS-9A'
$alunos = @{}
foreach ($m in 2026101..2026108) {
  $al = Garantir '/alunos' 'matricula' "$m" @{ matricula = "$m"; email = "aluno$m@escola.com"; turma = (Ref $turma) } "aluno $m"
  $alunos["$m"] = @($al)[0]
}

Write-Host '3/7 Dimensões, categorias e concepções' -ForegroundColor Cyan
$d1 = Garantir '/dimensoes' 'codigo' 'D1' @{ codigo = 'D1'; tipoRaciocinio = 'Dedutivo'; complexidade = 'Média' } 'dimensão D1'
$d2 = Garantir '/dimensoes' 'codigo' 'D2' @{ codigo = 'D2'; tipoRaciocinio = 'Indutivo'; complexidade = 'Alta' } 'dimensão D2'
$c1 = Garantir '/categorias' 'codigo' 'C1' @{ codigo = 'C1'; nome = 'Confusão entre grandezas' } 'categoria C1'
$c2 = Garantir '/categorias' 'codigo' 'C2' @{ codigo = 'C2'; nome = 'Força como causa do movimento' } 'categoria C2'
$c3 = Garantir '/categorias' 'codigo' 'C3' @{ codigo = 'C3'; nome = 'Erro de unidade' } 'categoria C3'
function Conc($desc, $sug, $cat) { Garantir '/concepcoes' 'descricao' $desc @{ descricao = $desc; sugestaoIntervencao = $sug; categorias = @(Ref $cat) } "concepção: $desc" }
$cMult  = Conc 'Multiplica distância e tempo para obter a velocidade' 'Retomar v = Δs/Δt com exemplos do cotidiano' $c1
$cForca = Conc 'Acredita que é preciso uma força para manter o movimento' 'Discutir o princípio da inércia com situações sem atrito' $c2
$cUnid  = Conc 'Não converte as unidades antes de calcular' 'Praticar a conversão entre km/h e m/s' $c3
$cMassa = Conc 'Confunde massa com peso' 'Diferenciar massa (kg) e peso (N) com exemplos' $c1

$hoje = (Get-Date).ToString('yyyy-MM-dd')
$titFis = 'Prova de Cinemática e Dinâmica (exemplo)'
$titScan = 'Prova scanner (demonstração)'
$existentes = @(Api GET '/avaliacoes')

Write-Host '4/7 Prova de Física' -ForegroundColor Cyan
$av = @($existentes | Where-Object { $_.titulo -eq $titFis -or $_.titulo -like '*Cinem*tica*' })[0]
if ($av) {
  Write-Host "  (já existia) avaliação id $($av.id): $($av.titulo)" -ForegroundColor DarkGray
  $questoesAvaliacao = @($av.questoes)
  if ($questoesAvaliacao.Count -eq 4) {
    $resp = [ordered]@{ '2026101' = 'ABAA'; '2026102' = 'ABAB'; '2026103' = 'AABA'; '2026104' = 'BBAB'; '2026105' = 'BABB'; '2026106' = 'ACCD' }
    foreach ($m in $resp.Keys) {
      $ids = @()
      for ($i = 0; $i -lt 4; $i++) {
        $letra = "$($resp[$m][$i])"
        $alt = @($questoesAvaliacao[$i].alternativas | Where-Object { $_.letra -eq $letra })[0]
        if ($alt) { $ids += @($alt.id)[0] }
      }
      if ($ids.Count -eq 4) {
        $alunoId = @($alunos[$m].id)[0]
        try {
          Api POST "/avaliacoes/$($av.id)/respostas" @{ alunoId = $alunoId; alternativasIds = $ids } | Out-Null
          Write-Host "  respostas do aluno $m gravadas" -ForegroundColor Green
        }
        catch {
          $statusCode = $null
          if ($_.Exception.Response) {
            try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
          }
          if ($statusCode -eq 409 -or ($_.Exception.Message -match '\(409\)|Conflict')) {
            Write-Host "  (já existiam) respostas do aluno $m" -ForegroundColor DarkGray
          } else {
            throw
          }
        }
      }
    }
  } else {
    Write-Host "  (aviso) avaliação existente não retornou 4 questões em `$av.questoes; pulando respostas automáticas" -ForegroundColor Yellow
  }
} else {
  $qs = @(
    (NovaQuestao 1 'Um carro percorre 120 km em 2 h. Qual é a velocidade média do carro?' $d1 @(
      (Alt 'A' '60 km/h' $true), (Alt 'B' '240 km/h' $false @($cMult)), (Alt 'C' '118 km/h' $false), (Alt 'D' '122 km/h' $false))),
    (NovaQuestao 2 'Um bloco desliza numa superfície sem atrito, com velocidade constante. Se nenhuma força atuar sobre ele, o que acontece com a velocidade?' $d1 @(
      (Alt 'A' 'Diminui até parar' $false @($cForca)), (Alt 'B' 'Permanece constante' $true), (Alt 'C' 'Aumenta gradualmente' $false @($cForca)), (Alt 'D' 'Inverte o sentido' $false))),
    (NovaQuestao 3 'Qual é a velocidade, em m/s, de um corpo que se move a 72 km/h?' $d2 @(
      (Alt 'A' '20 m/s' $true), (Alt 'B' '72 m/s' $false @($cUnid)), (Alt 'C' '259,2 m/s' $false @($cUnid)), (Alt 'D' '7,2 m/s' $false))),
    (NovaQuestao 4 'Uma pessoa tem massa de 60 kg. Qual é o peso dela, adotando g = 10 m/s²?' $d2 @(
      (Alt 'A' '600 N' $true), (Alt 'B' '60 N' $false @($cMassa)), (Alt 'C' '6 N' $false), (Alt 'D' '60 kg' $false @($cMassa))))
  )
  $av = Api POST '/avaliacoes' @{ titulo = $titFis; data = $hoje; fase = '1ª fase'; professor = (Ref $prof); questoes = @($qs | ForEach-Object { Ref $_.obj }) }
  Write-Host "  avaliação criada (id $($av.id))" -ForegroundColor Green
  $resp = [ordered]@{ '2026101' = 'ABAA'; '2026102' = 'ABAB'; '2026103' = 'AABA'; '2026104' = 'BBAB'; '2026105' = 'BABB'; '2026106' = 'ACCD' }
  foreach ($m in $resp.Keys) {
    $ids = @()
    for ($i = 0; $i -lt 4; $i++) {
      $ids += @($qs[$i].mapa["$($resp[$m][$i])"])[0]
    }
    $alunoId = @($alunos[$m].id)[0]
    Api POST "/avaliacoes/$($av.id)/respostas" @{ alunoId = $alunoId; alternativasIds = $ids } | Out-Null
    Write-Host "  respostas do aluno $m gravadas" -ForegroundColor Green
  }
}

Write-Host '5/7 Prova para o scanner' -ForegroundColor Cyan
$avScan = @($existentes | Where-Object { $_.titulo -eq $titScan -or $_.titulo -like '*scanner*' })[0]
if ($avScan) {
  Write-Host "  (já existia) avaliação scanner id $(@($avScan.id)[0]): $($avScan.titulo)" -ForegroundColor DarkGray
} else {
  $s1 = NovaQuestao 1 'Quanto é 1 + 1?' $null @((Alt 'A' '5' $false), (Alt 'B' '2' $true), (Alt 'C' '65' $false))
  $s2 = NovaQuestao 2 'Quanto é 64 + 1?' $null @((Alt 'A' '5' $false), (Alt 'B' '2' $false), (Alt 'C' '65' $true))
  $av2 = Api POST '/avaliacoes' @{ titulo = $titScan; data = $hoje; fase = '1ª fase'; professor = (Ref $prof); questoes = @((Ref $s1.obj), (Ref $s2.obj)) }
  Write-Host "  avaliação criada (id $(@($av2.id)[0]))" -ForegroundColor Green
}

Write-Host '6/7 Conferindo' -ForegroundColor Cyan
Write-Host "  avaliações: $(@(Api GET '/avaliacoes').Count) | alunos: $(@(Api GET '/alunos').Count) | questões: $(@(Api GET '/questoes').Count)"
Write-Host '7/7 Pronto' -ForegroundColor Cyan
Write-Host "  Dashboard: $BaseUrl/dashboard.html  (escolha a prova de Cinemática)"
Write-Host "  Scanner:   $BaseUrl/scanner.html  (prova scanner, aluno 2026107, limpa.png e marcada.png)" 

