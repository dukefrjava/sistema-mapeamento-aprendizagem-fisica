package com.system.simaf.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.dto.DTOLeituraScanner;

@Component
public class ScannerClient {

    private static final Logger log = LoggerFactory.getLogger(ScannerClient.class);

    private final RestClient http;

    public ScannerClient(@Value("${scanner.url}") String url,
            @Value("${scanner.token:}") String token) {
        log.info("ScannerClient apontando para {}", url); // prova de qual URL está ativa
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(120)); // OCR pode demorar
        RestClient.Builder b = RestClient.builder().baseUrl(url).requestFactory(fabrica);
        if (!token.isBlank()) {
            b = b.defaultHeader("X-Scanner-Token", token);
        }
        this.http = b.build();
    }

    public DTOLeituraScanner analisarPdf(byte[] pdf) {
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("arquivo", parte(pdf, "prova.pdf"));
        return enviar("/analisar-pdf", corpo);
    }

    public DTOLeituraScanner analisarImagem(byte[] modelo, byte[] marcada) {
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("modelo", parte(modelo, "modelo.img"));
        corpo.add("marcada", parte(marcada, "marcada.img"));
        return enviar("/analisar-imagem", corpo);
    }

    private DTOLeituraScanner enviar(String rota, MultiValueMap<String, Object> corpo) {
        try {
            return http.post().uri(rota)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(corpo)
                    .retrieve()
                    .body(DTOLeituraScanner.class);
        } catch (RestClientResponseException e) {
            // O SERVIÇO RESPONDEU COM ERRO: LOGA O STATUS E O CORPO REAIS
            String corpoResposta = e.getResponseBodyAsString();
            log.warn("Scanner respondeu HTTP {} em {}: {}",
                    e.getStatusCode().value(), rota,
                    corpoResposta.isBlank() ? "<corpo vazio>" : corpoResposta);

            HttpStatusCode status = e.getStatusCode().is4xxClientError()
                    ? HttpStatusCode.valueOf(e.getStatusCode().value()) // propaga o status real
                    : HttpStatus.BAD_GATEWAY;                            // 5xx = falha interna do scanner
            throw new ResponseStatusException(status,
                    "Serviço de leitura recusou o arquivo (HTTP %d): %s"
                            .formatted(e.getStatusCode().value(),
                                    corpoResposta.isBlank() ? "sem detalhes" : corpoResposta));
        } catch (ResourceAccessException e) {
            // NEM CHEGOU A CONECTAR: container do scanner desligado, URL errada ou timeout
            log.error("Não foi possível alcançar o scanner em {}: {}", rota, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de leitura indisponível (rode: docker compose up -d scanner)");
        }
    }

    private static ByteArrayResource parte(byte[] dados, String nome) {
        return new ByteArrayResource(dados) {
            @Override
            public String getFilename() {
                return nome;
            }
        };
    }
}