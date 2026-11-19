package com.system.simaf.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.system.simaf.entity.Professor;
import com.system.simaf.repository.ProfessorRepository;

// SPRING SECURITY ACHA O PROFESSOR PELO EMAIL E PEGA O HASH DA SENHA
@Service
public class ProfessorUserDetailsService implements UserDetailsService {
    private final ProfessorRepository repository;

    public ProfessorUserDetailsService(ProfessorRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Professor p = repository.findFirstyByEmailIgnoreCaseAndSenhaHashIsNotNullOrderByIdAsc(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não Encontrado"));
        return User.withUsername(p.getEmail())
            .password(p.getSenhaHash())
            .roles("PROFESSOR")
            .build();
    }
}
