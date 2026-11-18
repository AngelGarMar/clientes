package com.transporte.clientes.repositories;

import com.transporte.clientes.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByCelularAndEstatus(String celular, int estatus);
    Optional<Cliente> findByIdAndEstatus(Integer id, int estatus);
    List<Cliente> findAllByEstatus(int estatus);
    Optional<Cliente> findByCelularAndIdNotAndEstatus(String celular, Integer id, Integer estatus);
}
