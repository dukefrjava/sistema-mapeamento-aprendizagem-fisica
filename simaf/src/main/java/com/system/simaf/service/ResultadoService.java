package com.system.simaf.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.dto.DTOEnvioRespostas;
import com.system.simaf.dto.DTOResultadoAluno;
import com.system.simaf.entity.Alternativa;
import com.system.simaf.entity.Aluno;
import com.system.simaf.entity.Avaliacao;
import com.system.simaf.entity.Questao;
import com.system.simaf.entity.Resposta;
import com.system.simaf.repository.AlternativaRepository;
import com.system.simaf.repository.AlunoRepository;
import com.system.simaf.repository.AvaliacaoRepository;
import com.system.simaf.repository.RespostaRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultadoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final RespostaRepository respostaRepository;

    public ResultadoService(AvaliacaoRepository avaliacaoRepository, RespostaRepository respostaRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.respostaRepository = respostaRepository;
    }

    @Transactional(readOnly = true)
    public DTOResultadoAluno resultadoDoAluno(Integer avaliacaoId, Integer alunoId) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        List<Resposta> respostas = respostaRepository.findByAvaliacaoIdAndAlunoId(avaliacaoId, alunoId);
        if (respostas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não possui respostas nesta avaliação");
        }

        // questões descartadas não entram na conta
        int total = (int) avaliacao.getQuestoes().stream().filter(q -> !q.isDescartada()).count();
        int acertos = 0;
        Set<String> concepcoes = new LinkedHashSet<>();
        Map<String, int[]> porDimensao = new HashMap<>(); // [acertos, total]

        for (Resposta r : respostas) {
            Alternativa alt = r.getAlternativa();
            Questao q = alt.getQuestao();
            if (q.isDescartada()) continue;

            boolean acertou = alt.isCorreta();
            if (acertou) {
                acertos++;
            } else {
                alt.getConcepcoes().forEach(c -> concepcoes.add(c.getDescricao()));
            }

            if (q.getDimensao() != null) {
                int[] c = porDimensao.computeIfAbsent(q.getDimensao().getCodigo(), k -> new int[2]);
                c[1]++;
                if (acertou) c[0]++;
            }
        }

        double percentual = total == 0 ? 0 : acertos * 100.0 / total;

        Map<String, Double> acertoPorDimensao = new TreeMap<>();
        porDimensao.forEach((cod, c) -> acertoPorDimensao.put(cod, c[0] * 100.0 / c[1]));

        return new DTOResultadoAluno(alunoId, acertos, total, percentual,
                classificar(percentual), new ArrayList<>(concepcoes), acertoPorDimensao);
    }

    // Faixas de exemplo: defina as reais com seu orientador
    private String classificar(double p) {
        if (p >= 70) return "ADEQUADO";
        if (p >= 40) return "EM_DESENVOLVIMENTO";
        return "CRITICO";
    }
}