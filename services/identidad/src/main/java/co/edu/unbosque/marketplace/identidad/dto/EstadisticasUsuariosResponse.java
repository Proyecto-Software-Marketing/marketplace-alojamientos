package co.edu.unbosque.marketplace.identidad.dto;

// Cuantos usuarios hay
public class EstadisticasUsuariosResponse {

    private long total;
    private long huespedes;
    private long anfitriones;

    public EstadisticasUsuariosResponse(long total, long huespedes, long anfitriones) {
        this.total = total;
        this.huespedes = huespedes;
        this.anfitriones = anfitriones;
    }

    public long getTotal() { return total; }
    public long getHuespedes() { return huespedes; }
    public long getAnfitriones() { return anfitriones; }
}