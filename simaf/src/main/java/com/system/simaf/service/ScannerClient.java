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

// FALA COM O SERVIÇO PYTHON
@Component
public class ScannerClient {

    private static final Logger log = LoggerFactory.getLogger(ScannerClient.class);

    private final RestClient http;
    private final String baseUrl;

    public ScannerClient(@Value("${scanner.url}") String url,
            @Value("${scanner.token:}") String token) {
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(120)); // o OCR pode demorar
        RestClient.Builder b = RestClient.builder().baseUrl(this.baseUrl).requestFactory(fabrica);
        if (!token.isBlank()) {
            b = b.defaultHeader("X-Scanner-Token", token);
        }
        this.http = b.build();

        // Uma linha no boot mostra para onde o Spring realmente vai chamar (o token nunca é logado)
        log.info("Scanner configurado: url={} token={}", this.baseUrl,
                token.isBlank() ? "NÃO definido" : "definido");
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
        String alvo = baseUrl + rota;
        long inicio = System.nanoTime();
        try {
            DTOLeituraScanner r = http.post().uri(rota)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(corpo)
                    .retrieve()
                    .body(DTOLeituraScanner.class);
            log.info("Scanner POST {} -> 200 em {} ms", alvo, (System.nanoTime() - inicio) / 1_000_000);
            return r;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String texto = e.getResponseBodyAsString();
            // Log completo (status, cabeçalhos, corpo) para saber QUEM respondeu: o Flask ou um proxy
            log.warn("Scanner POST {} -> {} {} | headers={} | corpo='{}'",
                    alvo, status, e.getStatusText(), e.getResponseHeaders(), abreviar(texto));

            String detalhe = texto.isBlank()
                    ? "(resposta sem corpo; provavelmente não veio do scanner Flask, confira scanner.url)"
                    : abreviar(texto);

            switch (status) {
                // O Flask recusou o ARQUIVO: erro do cliente, repassa o motivo
                case 400, 415, 422 -> throw new ResponseStatusException(HttpStatusCode.valueOf(422),
                        "Serviço de leitura recusou o arquivo: " + detalhe);
                case 413 -> throw new ResponseStatusException(HttpStatusCode.valueOf(413),
                        "Arquivo grande demais para o serviço de leitura");
                // 401/403/404 etc. NÃO são culpa do arquivo: token errado, rota errada, URL errada
                default -> throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Serviço de leitura respondeu " + status + " em " + rota + ": " + detalhe);
            }
        } catch (ResourceAccessException e) {
            log.error("Scanner POST {} sem conexão: {}", alvo, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de leitura indisponível em " + baseUrl + " (" + e.getMessage() + ")");
        }
    }

    private static String abreviar(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }

    // O NOME É FIXO E INTERNO: NUNCA REPASSAMOS O NOME DO ARQUIVO ENVIADO PELO
    // USUÁRIO
    private static ByteArrayResource parte(byte[] dados, String nome) {
        return new ByteArrayResource(dados) {
            @Override
            public String getFilename() {
                return nome;
            }
        };
    }
}