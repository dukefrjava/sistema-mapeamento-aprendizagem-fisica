package com.system.simaf.service;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.dto.DTODashboard;
import com.system.simaf.dto.DTODashboard.ItemAluno;
import com.system.simaf.dto.DTODashboard.ItemConcepcao;
import com.system.simaf.dto.DTODashboard.ItemDificuldade;
import com.system.simaf.dto.DTODashboard.ItemQuestao;
import com.system.simaf.entity.Alternativa;
import com.system.simaf.entity.Aluno;
import com.system.simaf.entity.Avaliacao;
import com.system.simaf.entity.CategoriaConceitual;
import com.system.simaf.entity.ConcepcaoAlternativa;
import com.system.simaf.entity.Questao;
import com.system.simaf.entity.Resposta;
import com.system.simaf.repository.AvaliacaoRepository;
import com.system.simaf.repository.RespostaRepository;

@Service
public class DashboardService {
    private final AvaliacaoRepository avaliacaoRepository;
    private final RespostaRepository respostaRepository;

    public DashboardService(AvaliacaoRepository avaliacaoRepository, RespostaRepository respostaRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.respostaRepository = respostaRepository;
    }

    // CONTADOR MUTAVEL SIMPLES PARA AGRUPAR POR ROTULOS
    private static final class Contagem {
        final String rotulo;
        final String extra;
        int n;

        Contagem(String rotulo, String extra) {
            this.rotulo = rotulo;
            this.extra = extra;
        }
    }

    @Transactional(readOnly = true)
    public DTODashboard gerar(Integer avaliacaoId) {
        Avaliacao av = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));

        // QUESTÕES DESCARTADAS NÃO ENTRAM EM NENHUMA CONTA (MESMA REGRA DO RESULTADO DO ALUNO)
        List<Questao> questoes = av.getQuestoes().stream()
                .filter(q -> !q.isDescartada())
                .sorted(Comparator.comparing(q -> q.getNumero() != null ? q.getNumero() : q.getId()))
                .toList();
        int total = questoes.size();

        Set<Integer> idsValidos = new HashSet<>();
        Map<Integer, int[]> porQuestao = new LinkedHashMap<>(); // id -> [respondentes, acertos]
        for (Questao q : questoes) {
            idsValidos.add(q.getId());
            porQuestao.put(q.getId(), new int[2]);
        }

        Map<Integer, Aluno> alunos = new LinkedHashMap<>();
        Map<Integer, Integer> acertosPorAluno = new HashMap<>();
        Map<String, int[]> porDimensao = new TreeMap<>(); // codigo -> [respondentes, acertos]
        Map<String, Contagem> categorias = new HashMap<>();
        Map<Integer, Contagem> concepcoes = new HashMap<>();

        for (Resposta r : respostaRepository.findByAvaliacaoId(avaliacaoId)) {
            Alternativa alt = r.getAlternativa();
            Questao q = alt.getQuestao();
            if (!idsValidos.contains(q.getId())) {
                continue;
            }
            Aluno aluno = r.getAluno();
            alunos.putIfAbsent(aluno.getId(), aluno);
            acertosPorAluno.putIfAbsent(aluno.getId(), 0);

            int[] pq = porQuestao.get(q.getId());
            pq[0]++;
            boolean acertou = alt.isCorreta();
            if (acertou) {
                pq[1]++;
                acertosPorAluno.merge(aluno.getId(), 1, Integer::sum);
            } else {
                // CADA RESPOSTA ERRADA CONTA UMA VEZ POR CATEGORIA, MESMO QUE VÁRIAS
                // CONCEPÇÕES DA ALTERNATIVA PERTENÇAM À MESMA CATEGORIA.
                Set<String> categoriasDestaResposta = new HashSet<>();
                for (ConcepcaoAlternativa c : alt.getConcepcoes()) {
                    concepcoes.computeIfAbsent(c.getId(),
                            k -> new Contagem(c.getDescricao(), c.getSugestaoIntervencao())).n++;
                    for (CategoriaConceitual cat : c.getCategorias()) {
                        categorias.computeIfAbsent(cat.getCodigo(), k -> new Contagem(cat.getNome(), cat.getCodigo()));
                        categoriasDestaResposta.add(cat.getCodigo());
                    }
                }
                for (String codigo : categoriasDestaResposta) {
                    categorias.get(codigo).n++;
                }
            }
            if (q.getDimensao() != null) {
                int[] d = porDimensao.computeIfAbsent(q.getDimensao().getCodigo(), k -> new int[2]);
                d[0]++;
                if (acertou) {
                    d[1]++;
                }
            }
        }

        Map<String, Integer> distribuicao = new LinkedHashMap<>();
        distribuicao.put("ADEQUADO", 0);
        distribuicao.put("EM_DESENVOLVIMENTO", 0);
        distribuicao.put("CRITICO", 0);

        List<ItemAluno> listaAlunos = new ArrayList<>();
        double somaPercentuais = 0;
        for (Aluno a : alunos.values()) {
            int acertos = acertosPorAluno.get(a.getId());
            double percentual = total == 0 ? 0 : acertos * 100.0 / total;
            String desempenho = classificar(percentual);
            distribuicao.merge(desempenho, 1, Integer::sum);
            somaPercentuais += percentual;
            listaAlunos.add(new ItemAluno(a.getId(), a.getMatricula(), acertos, total, arredondar(percentual), desempenho));
        }
        listaAlunos.sort(Comparator.comparingDouble(ItemAluno::percentual)); // QUEM PRECISA DE AJUDA PRIMEIRO
        double media = alunos.isEmpty() ? 0 : somaPercentuais / alunos.size();

        List<ItemQuestao> listaQuestoes = new ArrayList<>();
        for (Questao q : questoes) {
            int[] pq = porQuestao.get(q.getId());
            double pct = pq[0] == 0 ? 0 : pq[1] * 100.0 / pq[0];
            listaQuestoes.add(new ItemQuestao(q.getNumero(), resumir(q.getEnunciado()), pq[0], pq[1], arredondar(pct)));
        }

        List<ItemDificuldade> listaCategorias = categorias.values().stream()
                .sorted(Comparator.comparingInt((Contagem c) -> c.n).reversed().thenComparing(c -> c.rotulo))
                .map(c -> new ItemDificuldade(c.extra, c.rotulo, c.n))
                .toList();

        List<ItemConcepcao> listaConcepcoes = concepcoes.values().stream()
                .sorted(Comparator.comparingInt((Contagem c) -> c.n).reversed().thenComparing(c -> c.rotulo))
                .map(c -> new ItemConcepcao(c.rotulo, c.extra, c.n))
                .toList();

        Map<String, Double> acertoPorDimensao = new LinkedHashMap<>();
        porDimensao.forEach((codigo, d) -> acertoPorDimensao.put(codigo, arredondar(d[1] * 100.0 / d[0])));

        return new DTODashboard(av.getId(), av.getTitulo(), alunos.size(), arredondar(media), distribuicao,
                listaQuestoes, listaCategorias, listaConcepcoes, acertoPorDimensao, listaAlunos);
    }

    // MESMAS FAIXAS DO RESULTADOSERVICE
    // exemplos -> definir
    private String classificar(double percentual) {
        if (percentual >= 70) {
            return "ADEQUADO";
        }
        if (percentual >= 40) {
            return "EM_DESENVOLVIMENTO";
        }
        return "CRITICO";
    }

    private static double arredondar(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static String resumir(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= 80 ? texto : texto.substring(0, 77) + "...";
    }
}
