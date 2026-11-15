package co.edu.unbosque.marketplace.reportes.dto;

// Una ciudad y cuantas reservas tiene
public class CiudadDemanda {

    private String ciudad;
    private long reservas;

    public CiudadDemanda() {
    }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public long getReservas() { return reservas; }
    public void setReservas(long reservas) { this.reservas = reservas; }
}