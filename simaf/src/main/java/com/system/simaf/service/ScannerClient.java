package com.system.simaf.service;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.dto.DTOLeituraScanner;

// FALA COM O SERVIÇO PYTHON
@Component 
public class ScannerClient {

    private final RestClient http;
    
    public ScannerClient(@Value("${scanner.url}") String url) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(120)); // TEMPO DO OCR PODE SER MAIOR
        this.http = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
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
            // O PYTHON RESPONDEU, MAS RECUSOU O ARQUIVO (4xx) OU FALHOU POR DENTRO (5xx)
            HttpStatusCode status = e.getStatusCode().is4xxClientError()
                    ? HttpStatusCode.valueOf(422) : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status,
                    "Serviço de leitura recusou o arquivo: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // NEM CHEGOU A CONECTAR: O CONTAINER DO SCANNER ESTÁ DESLIGADO
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de leitura indisponível (rode: docker compose up -d scanner)");
        }
    }

    // O NOME É FIXO E INTERNO: NUNCA REPASSAMOS O NOME DO ARQUIVO ENVIADO PELO USUÁRIO
    private static ByteArrayResource parte(byte[] dados, String nome) {
        return new ByteArrayResource(dados) {
            @Override 
            public String getFilename() {
                return nome;
            }
        };
    }
}
