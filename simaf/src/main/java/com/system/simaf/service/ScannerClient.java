package com.system.simaf.service;

import java.net.URI;
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
        this.baseUrl = raiz(url);
        log.info("Scanner configurado: url={} token={}", baseUrl, token.isBlank() ? "NÃO definido" : "definido");

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(120)); // OCR pode demorar
        RestClient.Builder b = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica);
        if (!token.isBlank()) {
            b = b.defaultHeader("X-Scanner-Token", token);
        }
        this.http = b.build();
    }

    // Mantém só esquema + host + porta. Se SCANNER_URL vier com caminho (ex.: .../health),
    // o caminho é descartado, senão a chamada vira /health/analisar-imagem (404).
    private static String raiz(String url) {
        URI u = URI.create(url.trim());
        String base = u.getScheme() + "://" + u.getAuthority();
        String caminho = u.getPath();
        if (caminho != null && !caminho.isBlank() && !"/".equals(caminho)) {
            log.warn("scanner.url tinha o caminho '{}'; ignorado, usando apenas {}", caminho, base);
        }
        return base;
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
        try {
            DTOLeituraScanner r = http.post().uri(rota)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(corpo)
                    .retrieve()
                    .body(DTOLeituraScanner.class);
            log.info("Scanner POST {} -> 200", alvo);
            return r;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String texto = e.getResponseBodyAsString();
            log.warn("Scanner POST {} -> {} | corpo='{}'", alvo, status, abreviar(texto));
            String detalhe = texto.isBlank() ? "sem detalhes" : abreviar(texto);

            switch (status) {
                // o scanner recusou o ARQUIVO
                case 400, 415, 422 -> throw new ResponseStatusException(HttpStatusCode.valueOf(422),
                        "Serviço de leitura recusou o arquivo: " + detalhe);
                case 413 -> throw new ResponseStatusException(HttpStatusCode.valueOf(413),
                        "Arquivo grande demais para o serviço de leitura");
                // 401/403/404/5xx = problema de configuração ou do scanner, NÃO do arquivo.
                // Sempre 502, assim um 404 no app só pode ser do próprio Spring.
                default -> throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Scanner respondeu HTTP " + status + " em " + alvo + ": " + detalhe);
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

    private static ByteArrayResource parte(byte[] dados, String nome) {
        return new ByteArrayResource(dados) {
            @Override
            public String getFilename() {
                return nome;
            }
        };
    }
}