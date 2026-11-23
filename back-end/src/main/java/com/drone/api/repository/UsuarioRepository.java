package com.drone.api.repository;

import com.drone.api.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"empresa", "empresa.telefones"})
    Optional<Usuario> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"empresa", "empresa.telefones"})
    Optional<Usuario> findByResetToken(String resetToken);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
