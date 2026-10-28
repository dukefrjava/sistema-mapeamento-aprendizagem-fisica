package com.system.simaf.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.dto.DTOEnvioRespostas;
import com.system.simaf.entity.Alternativa;
import com.system.simaf.entity.Aluno;
import com.system.simaf.entity.Avaliacao;
import com.system.simaf.entity.Questao;
import com.system.simaf.entity.Resposta;
import com.system.simaf.repository.AlternativaRepository;
import com.system.simaf.repository.AlunoRepository;
import com.system.simaf.repository.AvaliacaoRepository;
import com.system.simaf.repository.RespostaRepository;

import jakarta.transaction.Transactional;

@Service
public class RespostaService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AlunoRepository alunoRepository;
    private final AlternativaRepository alternativaRepository;
    private final RespostaRepository respostaRepository;

    public RespostaService(AvaliacaoRepository avaliacaoRepository, AlunoRepository alunoRepository,
                           AlternativaRepository alternativaRepository, RespostaRepository respostaRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.alunoRepository = alunoRepository;
        this.alternativaRepository = alternativaRepository;
        this.respostaRepository = respostaRepository;
    }

    @Transactional
    public void enviar(Integer avaliacaoId, DTOEnvioRespostas dto) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));
        Aluno aluno = alunoRepository.findById(dto.alunoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));

        if (!respostaRepository.findByAvaliacaoIdAndAlunoId(avaliacaoId, aluno.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este aluno já enviou respostas");
        }

        Set<Integer> questoesDaAvaliacao = avaliacao.getQuestoes().stream()
                .map(Questao::getId).collect(Collectors.toSet());
        Set<Integer> questoesRespondidas = new HashSet<>();

        for (Integer altId : dto.alternativasIds()) {
            Alternativa alt = alternativaRepository.findById(altId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alternativa " + altId + " não existe"));
            Integer questaoId = alt.getQuestao().getId();

            if (!questoesDaAvaliacao.contains(questaoId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa " + altId + " não pertence a esta avaliação");
            }
            if (!questoesRespondidas.add(questaoId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questão " + questaoId + " respondida mais de uma vez");
            }

            Resposta r = new Resposta();
            r.setAluno(aluno);
            r.setAvaliacao(avaliacao);
            r.setAlternativa(alt);
            respostaRepository.save(r);
        }
    }
}