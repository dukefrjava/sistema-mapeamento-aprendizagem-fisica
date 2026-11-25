package com.system.simaf.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.dto.DTOResultadoDetalhe;
import com.system.simaf.dto.DTOResultadoDetalhe.AlunoInfo;
import com.system.simaf.dto.DTOResultadoDetalhe.AvaliacaoInfo;
import com.system.simaf.dto.DTOResultadoDetalhe.CategoriaItem;
import com.system.simaf.dto.DTOResultadoDetalhe.ConcepcaoItem;
import com.system.simaf.dto.DTOResultadoDetalhe.DimensaoItem;
import com.system.simaf.dto.DTOResultadoDetalhe.QuestaoItem;
import com.system.simaf.entity.Alternativa;
import com.system.simaf.entity.Aluno;
import com.system.simaf.entity.Avaliacao;
import com.system.simaf.entity.CategoriaConceitual;
import com.system.simaf.entity.ConcepcaoAlternativa;
import com.system.simaf.entity.Questao;
import com.system.simaf.entity.Resposta;
import com.system.simaf.repository.AvaliacaoRepository;
import com.system.simaf.repository.RespostaRepository;

/**
 * Métricas individuais: o que o aluno acertou e errou, em que dimensão ACE, quais concepções
 * alternativas os erros indicam e como ele está em relação à turma na mesma avaliação.
 * Só lê dados; não grava nada.
 */
@Service
public class ResultadoDetalheService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final RespostaRepository respostaRepository;

    public ResultadoDetalheService(AvaliacaoRepository avaliacaoRepository,
                                   RespostaRepository respostaRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.respostaRepository = respostaRepository;
    }

    @Transactional(readOnly = true)
    public DTOResultadoDetalhe detalhe(Integer avaliacaoId, Integer alunoId) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        // questões que valem nota, na ordem da prova
        List<Questao> questoes = new ArrayList<>();
        for (Questao q : avaliacao.getQuestoes()) {
            if (!q.isDescartada()) questoes.add(q);
        }
        questoes.sort(Comparator.comparing((Questao q) -> numeroDe(q) != null ? numeroDe(q) : Integer.MAX_VALUE)
                .thenComparing(Questao::getId));

        // alternativa escolhida por aluno e por questão (se houver duplicata, vale a última)
        Map<Integer, Map<Integer, Alternativa>> porAluno = new LinkedHashMap<>();
        Map<Integer, Aluno> alunos = new HashMap<>();
        for (Resposta r : respostaRepository.findByAvaliacaoId(avaliacaoId)) {
            alunos.put(r.getAluno().getId(), r.getAluno());
            Alternativa alt = r.getAlternativa();
            Integer qid = alt.getQuestao().getId();
            porAluno.computeIfAbsent(r.getAluno().getId(), k -> new HashMap<>()).put(qid, alt);
        }

        Map<Integer, Alternativa> minhas = porAluno.get(alunoId);
        if (minhas == null || minhas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não possui respostas nesta avaliação");
        }

        // ---- turma: acerto por questão, por dimensão e nota de cada aluno ----
        Map<Integer, int[]> turmaQuestao = new HashMap<>();   // [acertos, respondentes]
        Map<String, int[]> turmaDimensao = new HashMap<>();   // [acertos, total]
        Map<Integer, Double> percentualPorAluno = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Integer, Alternativa>> e : porAluno.entrySet()) {
            int acertosAluno = 0;
            for (Questao q : questoes) {
                Alternativa a = e.getValue().get(q.getId());
                if (a == null) continue;
                int[] tq = turmaQuestao.computeIfAbsent(q.getId(), k -> new int[2]);
                tq[1]++;
                if (a.isCorreta()) { tq[0]++; acertosAluno++; }
                if (q.getDimensao() != null) {
                    int[] td = turmaDimensao.computeIfAbsent(q.getDimensao().getCodigo(), k -> new int[2]);
                    td[1]++;
                    if (a.isCorreta()) td[0]++;
                }
            }
            percentualPorAluno.put(e.getKey(), pct(acertosAluno, questoes.size()));
        }

        // ---- o aluno ----
        List<QuestaoItem> itens = new ArrayList<>();
        Map<String, int[]> minhaDimensao = new LinkedHashMap<>();
        Map<String, int[]> errosPorCategoria = new LinkedHashMap<>();
        Map<String, String> nomeCategoria = new HashMap<>();
        Map<String, int[]> ocorrenciaConcepcao = new LinkedHashMap<>();
        Map<String, ConcepcaoAlternativa> concepcaoPorDescricao = new HashMap<>();
        int acertos = 0, erros = 0, semResposta = 0, pos = 0;

        for (Questao q : questoes) {
            pos++;
            Integer n = numeroDe(q);
            int numero = n != null ? n : pos;
            Alternativa certa = q.getAlternativas().stream().filter(Alternativa::isCorreta).findFirst().orElse(null);
            String letraCerta = certa == null ? "-" : certa.getLetra();
            int[] tq = turmaQuestao.get(q.getId());
            double pctTurma = tq == null ? 0 : pct(tq[0], tq[1]);

            Alternativa marcada = minhas.get(q.getId());
            if (marcada == null) {
                semResposta++;
                itens.add(new QuestaoItem(numero, q.getEnunciado(), false, false, null, letraCerta, pctTurma, List.of()));
                continue;
            }
            boolean acertou = marcada.isCorreta();
            List<String> descricoes = new ArrayList<>();
            if (acertou) {
                acertos++;
            } else {
                erros++;
                for (ConcepcaoAlternativa c : marcada.getConcepcoes()) {
                    descricoes.add(c.getDescricao());
                    ocorrenciaConcepcao.computeIfAbsent(c.getDescricao(), k -> new int[1])[0]++;
                    concepcaoPorDescricao.putIfAbsent(c.getDescricao(), c);
                    for (CategoriaConceitual cat : c.getCategorias()) {
                        errosPorCategoria.computeIfAbsent(cat.getCodigo(), k -> new int[1])[0]++;
                        nomeCategoria.put(cat.getCodigo(), cat.getNome());
                    }
                }
            }
            if (q.getDimensao() != null) {
                int[] d = minhaDimensao.computeIfAbsent(q.getDimensao().getCodigo(), k -> new int[2]);
                d[1]++;
                if (acertou) d[0]++;
            }
            itens.add(new QuestaoItem(numero, q.getEnunciado(), true, acertou,
                    marcada.getLetra() + ") " + marcada.getTexto(), letraCerta, pctTurma, descricoes));
        }

        List<DimensaoItem> dimensoes = new ArrayList<>();
        minhaDimensao.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            int[] t = turmaDimensao.get(e.getKey());
            dimensoes.add(new DimensaoItem(e.getKey(), e.getValue()[0], e.getValue()[1],
                    pct(e.getValue()[0], e.getValue()[1]), t == null ? 0 : pct(t[0], t[1])));
        });

        List<CategoriaItem> categorias = new ArrayList<>();
        errosPorCategoria.forEach((cod, v) -> categorias.add(new CategoriaItem(cod, nomeCategoria.get(cod), v[0])));
        categorias.sort(Comparator.comparingInt(CategoriaItem::erros).reversed()
                .thenComparing(CategoriaItem::codigo));

        List<ConcepcaoItem> concepcoes = new ArrayList<>();
        ocorrenciaConcepcao.forEach((desc, v) -> {
            ConcepcaoAlternativa c = concepcaoPorDescricao.get(desc);
            List<String> cods = new ArrayList<>();
            c.getCategorias().forEach(cat -> cods.add(cat.getCodigo()));
            cods.sort(String::compareTo);
            concepcoes.add(new ConcepcaoItem(desc, c.getSugestaoIntervencao(), v[0], cods));
        });
        concepcoes.sort(Comparator.comparingInt(ConcepcaoItem::ocorrencias).reversed()
                .thenComparing(ConcepcaoItem::descricao));

        double percentual = pct(acertos, questoes.size());
        double media = percentualPorAluno.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        int posicao = 1;
        for (double p : percentualPorAluno.values()) {
            if (p > percentual) posicao++;
        }

        Aluno aluno = alunos.get(alunoId);
        String turma = aluno.getTurma() == null ? null : aluno.getTurma().getCodigo();

        return new DTOResultadoDetalhe(
                new AlunoInfo(aluno.getId(), aluno.getMatricula(), turma),
                new AvaliacaoInfo(avaliacao.getId(), avaliacao.getTitulo()),
                acertos, erros, semResposta, questoes.size(), percentual, classificar(percentual),
                media, posicao, percentualPorAluno.size(),
                itens, dimensoes, categorias, concepcoes);
    }

    private static double pct(int parte, int total) {
        return total == 0 ? 0 : parte * 100.0 / total;
    }

    /** Usa o campo "numero" da questão quando existir; senão a ordem do id. */
    private static Integer numeroDe(Questao q) {
        try {
            Object n = q.getClass().getMethod("getNumero").invoke(q);
            if (n instanceof Number num) return num.intValue();
        } catch (ReflectiveOperationException ignorada) {
            // entidade sem numero: usa a posição
        }
        return null;
    }

    // Mesmas faixas do ResultadoService (critério próprio, a validar com o orientador)
    private static String classificar(double p) {
        if (p >= 70) return "ADEQUADO";
        if (p >= 40) return "EM_DESENVOLVIMENTO";
        return "CRITICO";
    }
}
