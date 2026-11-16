package co.edu.unbosque.marketplace.auditoria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import co.edu.unbosque.marketplace.auditoria.model.EventoAuditoria;

public interface EventoAuditoriaRepository
		extends JpaRepository<EventoAuditoria, Long>, JpaSpecificationExecutor<EventoAuditoria> {

}
