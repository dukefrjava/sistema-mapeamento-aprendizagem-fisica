param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,          # Ex: "https://simaf-java.onrender.com"

    [Parameter(Mandatory = $true)]
    [string]$Email,

    [Parameter(Mandatory = $true)]
    [string]$Senha,

    [string]$Convite = ""        # Obrigatório apenas se for registrar o professor pela primeira vez
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")

# 1. Registrar professor (se informado $Convite)
if (-not [string]::IsNullOrWhiteSpace($Convite)) {
    Write-Host "Registrando professor em $BaseUrl/auth/registrar ..." -ForegroundColor Cyan
    $registroBody = @{
        email  = $Email
        senha  = $Senha
        convite = $Convite
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/registrar" -ContentType "application/json" -Body $registroBody | Out-Null
        Write-Host "Professor registrado (ou já existente)." -ForegroundColor Green
    }
    catch {
        Write-Warning "Aviso no /auth/registrar (pode já existir): $($_.Exception.Message)"
    }
}

# 2. Montar header Basic Auth para as demais requisições
$pair = "$Email`:$Senha"
$encodedCreds = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
$headers = @{
    Authorization = "Basic $encodedCreds"
    "Content-Type"  = "application/json; charset=utf-8"
}

function Invoke-SimafPost {
    param(
        [string]$Path,
        [hashtable]$Payload
    )
    $json = $Payload | ConvertTo-Json -Depth 10
    return Invoke-RestMethod -Method Post -Uri "$BaseUrl$Path" -Headers $headers -Body $json
}

# --- AJUSTE OS CAMINHOS E CAMPOS ABAIXO CONFORME SEUS CONTROLLERS ---

# 3. Turma
$turma = Invoke-SimafPost -Path "/turmas" -Payload @{
    nome = "Turma Demo Física"
    # ano = 2026
}
$turmaId = $turma.id
Write-Host "Turma criada: ID=$turmaId" -ForegroundColor Green

# 4. Quatro alunos
$nomesAlunos = @("Aluno 1", "Aluno 2", "Aluno 3", "Aluno 4")
$alunosIds = @()
foreach ($nome in $nomesAlunos) {
    $aluno = Invoke-SimafPost -Path "/alunos" -Payload @{
        nome    = $nome
        turmaId = $turmaId
    }
    $alunosIds += $aluno.id
    Write-Host "Aluno criado: $nome (ID=$($aluno.id))" -ForegroundColor Green
}

# 5. Dimensão, Categoria e Concepção
$dimensao = Invoke-SimafPost -Path "/dimensoes" -Payload @{
    nome = "Dimensão Demo"
}

$categoria = Invoke-SimafPost -Path "/categorias" -Payload @{
    nome       = "Categoria Demo"
    dimensaoId = $dimensao.id
}

$concepcao = Invoke-SimafPost -Path "/concepcoes" -Payload @{
    descricao   = "Concepção alternativa de teste"
    categoriaId = $categoria.id
}

# 6. Duas questões (com numero)
$q1 = Invoke-SimafPost -Path "/questoes" -Payload @{
    numero      = 1
    enunciado   = "Questão 1 de demonstração"
    concepcaoId = $concepcao.id
}

$q2 = Invoke-SimafPost -Path "/questoes" -Payload @{
    numero      = 2
    enunciado   = "Questão 2 de demonstração"
    concepcaoId = $concepcao.id
}

# 7. Avaliação
$avaliacao = Invoke-SimafPost -Path "/avaliacoes" -Payload @{
    titulo     = "Avaliação Diagnóstica Demo"
    turmaId    = $turmaId
    questaoIds = @($q1.id, $q2.id)
}
Write-Host "Avaliação criada: ID=$($avaliacao.id)" -ForegroundColor Green

Write-Host "Cenário base concluído. Agora use scanner.html com limpa.png e marcada.png e abra dashboard.html." -ForegroundColor Yellow