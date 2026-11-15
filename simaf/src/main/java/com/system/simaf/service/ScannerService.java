package com.system.simaf.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.dto.DTOEnvioRespostas;
import com.system.simaf.dto.DTOLeituraProva;
import com.system.simaf.dto.DTOLeituraScanner;
import com.system.simaf.entity.Alternativa;
import com.system.simaf.entity.Avaliacao;
import com.system.simaf.entity.Questao;
import com.system.simaf.repository.AvaliacaoRepository;

@Service
public class ScannerService {

    private static final long LIMITE_BYTES = 15L * 1024 * 1024;
    private static final HttpStatusCode INVALIDO = HttpStatusCode.valueOf(422);

    private final ScannerClient client;
    private final AvaliacaoRepository avaliacaoRepository;
    private final RespostaService respostaService;

    public ScannerService(ScannerClient client, AvaliacaoRepository avaliacaoRepository,
                          RespostaService respostaService) {
        this.client = client;
        this.avaliacaoRepository = avaliacaoRepository;
        this.respostaService = respostaService;
    }

    @Transactional
    public DTOLeituraProva lerPdf(Integer avaliacaoId, Integer alunoId, MultipartFile arquivo, boolean salvar) {
        byte[] pdf = validar(arquivo, true);
        return processar(avaliacaoId, alunoId, client.analisarPdf(pdf), salvar);
    }

    @Transactional
    public DTOLeituraProva lerImagem(Integer avaliacaoId, Integer alunoId, MultipartFile modelo,
                                     MultipartFile marcada, boolean salvar) {
        byte[] m = validar(modelo, false);
        byte[] c = validar(marcada, false);
        return processar(avaliacaoId, alunoId, client.analisarImagem(m, c), salvar);
    }

    private DTOLeituraProva processar(Integer avaliacaoId, Integer alunoId,
                                      DTOLeituraScanner leitura, boolean salvar) {
        Avaliacao av = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        List<Questao> questoes = questoesNumeradas(av);
        Map<String, String> detectadas = leitura.respostas() == null ? Map.of() : leitura.respostas();
        List<String> avisos = new ArrayList<>(leitura.avisos() == null ? List.of() : leitura.avisos());
        List<Integer> ids = new ArrayList<>();
        Set<String> esperadas = new HashSet<>();

        for (Questao q : questoes) {
            String chave = "Q" + q.getNumero();
            esperadas.add(chave);
            String letra = detectadas.get(chave);
            if (letra == null) {
                avisos.add(chave + ": sem marcação detectada");
                continue;
            }
            Alternativa alt = q.getAlternativas().stream()
                    .filter(a -> a.getLetra().equalsIgnoreCase(letra))
                    .findFirst().orElse(null);
            if (alt == null) {
                avisos.add(chave + ": a letra '" + letra + "' não existe nas alternativas cadastradas");
            } else {
                ids.add(alt.getId());
            }
        }
        for (String chave : detectadas.keySet()) {
            if (!esperadas.contains(chave)) {
                avisos.add(chave + " foi lida no arquivo, mas a avaliação não tem questão com esse número");
            }
        }

        boolean salvo = false;
        if (salvar && !ids.isEmpty()) {
            respostaService.enviar(avaliacaoId, new DTOEnvioRespostas(alunoId, ids));
            salvo = true;
        }
        return new DTOLeituraProva(detectadas, ids, avisos, salvo);
    }

    // QUESTÕES DA AVALIAÇÃO ORDENADAS POR NÚMERO; 
    // RECUSA NÚMEROS FALTANDO OU REPETIDOS
    private List<Questao> questoesNumeradas(Avaliacao av) {
        List<Questao> lista = av.getQuestoes().stream()
                .sorted(Comparator.comparing(Questao::getNumero, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (lista.isEmpty()) {
            throw new ResponseStatusException(INVALIDO, "A avaliação não tem questões");
        }
        Set<Integer> vistos = new HashSet<>();
        for (Questao q : lista) {
            if (q.getNumero() == null) {
                throw new ResponseStatusException(INVALIDO, "A questão " + q.getId() + " não tem número");
            }
            if (!vistos.add(q.getNumero())) {
                throw new ResponseStatusException(INVALIDO, "Número " + q.getNumero() + " repetido na avaliação");
            }
        }
        return lista;
    }

    // VALIDA TAMANHO E O CONTEÚDO REAL (ASSINATURA), IGNORANDO NOME E CONTENT-TYPE ENVIADOS
    private byte[] validar(MultipartFile f, boolean pdf) {
        if (f == null || f.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo ausente ou vazio");
        }
        if (f.getSize() > LIMITE_BYTES) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(413), "Arquivo acima de 15 MB");
        }
        byte[] b;
        try {
            b = f.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo");
        }
        boolean ok = pdf ? comeca(b, '%', 'P', 'D', 'F', '-')
                : comeca(b, 0x89, 'P', 'N', 'G') || comeca(b, 0xFF, 0xD8, 0xFF);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    pdf ? "Envie um PDF válido" : "Envie imagens PNG ou JPEG válidas");
        }
        return b;
    }

    private static boolean comeca(byte[] b, int... assinatura) {
        if (b.length < assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if ((b[i] & 0xFF) != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}