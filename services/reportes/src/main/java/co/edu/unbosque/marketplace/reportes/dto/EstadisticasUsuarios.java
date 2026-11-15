package co.edu.unbosque.marketplace.reportes.dto;


public class EstadisticasUsuarios {

    private long total;
    private long huespedes;
    private long anfitriones;

    public EstadisticasUsuarios() {
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getHuespedes() { return huespedes; }
    public void setHuespedes(long huespedes) { this.huespedes = huespedes; }

    public long getAnfitriones() { return anfitriones; }
    public void setAnfitriones(long anfitriones) { this.anfitriones = anfitriones; }
}