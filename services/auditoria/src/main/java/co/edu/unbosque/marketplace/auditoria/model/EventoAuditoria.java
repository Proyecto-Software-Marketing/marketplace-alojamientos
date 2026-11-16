package co.edu.unbosque.marketplace.auditoria.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "evento_auditoria", indexes = {
		@Index(name = "idx_evento_auditoria_fecha", columnList = "fecha"),
		@Index(name = "idx_evento_auditoria_usuario", columnList = "id_usuario"),
		@Index(name = "idx_evento_auditoria_entidad_accion", columnList = "entidad, accion")
})
public class EventoAuditoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "id_usuario")
	private Long usuarioId;

	@Column(name = "entidad", nullable = false, length = 40)
	private String entidad;

	@Column(name = "id_entidad")
	private Long entidadId;

	@Column(name = "accion", nullable = false, length = 60)
	private String accion;

	@Column(name = "resultado", nullable = false, length = 30)
	private String resultado;

	@Column(name = "detalle", length = 1000)
	private String detalle;

	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	protected EventoAuditoria() {
	}

	public EventoAuditoria(Long usuarioId, String entidad, Long entidadId, String accion, String resultado,
			String detalle, LocalDateTime fecha) {
		this.usuarioId = usuarioId;
		this.entidad = entidad;
		this.entidadId = entidadId;
		this.accion = accion;
		this.resultado = resultado;
		this.detalle = detalle;
		this.fecha = fecha;
	}

	public Long getId() {
		return id;
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public String getEntidad() {
		return entidad;
	}

	public Long getEntidadId() {
		return entidadId;
	}

	public String getAccion() {
		return accion;
	}

	public String getResultado() {
		return resultado;
	}

	public String getDetalle() {
		return detalle;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

}
