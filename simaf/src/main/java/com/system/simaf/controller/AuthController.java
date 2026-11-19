package com.system.simaf.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.dto.DTOProfessorResumo;
import com.system.simaf.dto.DTORegistro;
import com.system.simaf.entity.Professor;
import com.system.simaf.repository.ProfessorRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final ProfessorRepository repository;
    private final PasswordEncoder encoder;
    private final String convite;

     public AuthController(ProfessorRepository repository, PasswordEncoder encoder,
                          @Value("${simaf.convite:}") String convite) {
        this.repository = repository;
        this.encoder = encoder;
        this.convite = convite;
    }

    @PostMapping("/registrar")
    @ResponseStatus((HttpStatus.CREATED))
    public DTOProfessorResumo registrar(@RequestBody DTORegistro dto) {
       // SEM CONVITE CONFIGURADO O CADASTRO FICA FECHADO
       if (convite.isBlank() || dto.convite() == null || !iguais(convite, dto.convite())) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Convite inválido");
        }
        if (vazio(dto.nome()) || vazio(dto.email()) || !dto.email().contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome e um e-mail válido");
        }
        if (dto.senha() == null || dto.senha().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha precisa ter ao menos 8 caracteres");
        }
        String email = dto.email().trim();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        Professor p = new Professor();
        p.setNome(dto.nome().trim());
        p.setEmail(email);
        p.setDisciplina(vazio(dto.disciplina()) ? "-" : dto.disciplina().trim());
        p.setSenhaHash(encoder.encode(dto.senha())); // BCrypt: a senha nunca é guardada em texto
        p = repository.save(p);
        return new DTOProfessorResumo(p.getId(), p.getNome(), p.getEmail());
    }

    // DIZ QUEM ESTÁ LOGADO
    // TESTE DE LOGIN
    @GetMapping("/eu")
    public Map<String, String> eu(Authentication autenticacao) {
        if (autenticacao == null || autenticacao instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }
        return Map.of("usuario", autenticacao.getName());
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

   // COMPARAÇÃO EM TEMPO CONSTANTE, PARA NÃO VAZAR O CONVITE POR DIFERENÇA DE TEMPO
    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
