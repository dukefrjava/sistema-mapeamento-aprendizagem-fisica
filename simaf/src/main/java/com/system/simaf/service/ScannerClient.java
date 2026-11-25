package com.system.simaf.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
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

/** Fala com o serviço Python (rede interna). O navegador nunca chama o Python direto. */
@Component
public class ScannerClient {

    private final RestClient http;

    public ScannerClient(@Value("${scanner.url}") String url,
                         @Value("${scanner.token:}") String token) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(120_000); // OCR pode demorar
        RestClient.Builder b = RestClient.builder().baseUrl(url).requestFactory(f);
        if (token != null && !token.isEmpty()) { b.defaultHeader("X-Scanner-Token", token); }
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
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "O servico de leitura recusou o token: confira se SCANNER_TOKEN e igual nos dois servicos");
            }
            HttpStatus status = e.getStatusCode().is4xxClientError()
                    ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "Serviço de leitura recusou o arquivo: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de leitura indisponível ou acordando; tente de novo em 1 minuto");
        }
    }

    /** O nome é fixo e interno: nunca repassamos o nome enviado pelo usuário. */
    private static ByteArrayResource parte(byte[] dados, String nome) {
        return new ByteArrayResource(dados) {
            @Override
            public String getFilename() {
                return nome;
            }
        };
    }
}