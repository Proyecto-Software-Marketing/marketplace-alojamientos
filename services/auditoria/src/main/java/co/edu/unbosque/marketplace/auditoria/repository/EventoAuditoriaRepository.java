package co.edu.unbosque.marketplace.auditoria.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

}
