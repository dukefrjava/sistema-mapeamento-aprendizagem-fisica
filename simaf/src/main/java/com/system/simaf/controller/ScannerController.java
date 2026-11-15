package com.system.simaf.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.system.simaf.dto.DTOLeituraProva;
import com.system.simaf.service.ScannerService;

@RestController
@RequestMapping("/avaliacoes/{avaliacaoId}/alunos/{alunoId}/escanear")
public class ScannerController {

    private final ScannerService service;

    public ScannerController(ScannerService service) {
        this.service = service;
    }

    // PROVA EM PDF MARCADA. COM SALVAR=FALSE (PADRÃO) SÓ MOSTRA O QUE FOI LIDO
    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DTOLeituraProva pdf(@PathVariable Integer avaliacaoId, @PathVariable Integer alunoId,
                               @RequestParam("arquivo") MultipartFile arquivo,
                               @RequestParam(defaultValue = "false") boolean salvar) {
        return service.lerPdf(avaliacaoId, alunoId, arquivo, salvar);
    }

    // FOTO DA PROVA MARCADA + IMAGEM DO MODELO LIMPO
    @PostMapping(value = "/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DTOLeituraProva imagem(@PathVariable Integer avaliacaoId, @PathVariable Integer alunoId,
                                  @RequestParam("modelo") MultipartFile modelo,
                                  @RequestParam("marcada") MultipartFile marcada,
                                  @RequestParam(defaultValue = "false") boolean salvar) {
        return service.lerImagem(avaliacaoId, alunoId, modelo, marcada, salvar);
    }
}