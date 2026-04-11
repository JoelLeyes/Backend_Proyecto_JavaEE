public class Adjunto {
    private int idAdjunto;
    private String tipoArchivo;
    private String nombreArchivo;
    private String urlArchivo;

    private Mensaje mensaje;

    public Adjunto() {}
    public Adjunto(int idAdjunto, String tipoArchivo, String nombreArchivo, String urlArchivo, Mensaje mensaje) {
        this.idAdjunto = idAdjunto;
        this.tipoArchivo = tipoArchivo;
        this.nombreArchivo = nombreArchivo;
        this.urlArchivo = urlArchivo;
        this.mensaje = mensaje;
    }

    public void setIdAdjunto(int idAdjunto) { this.idAdjunto = idAdjunto; }
    public void setTipoArchivo(String TipoArchivo) { this.TipoArchivo = TipoArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }
    public void setMensaje(Mensaje mensaje) { this.mensaje = mensaje; }

    public int getIdAdjunto() { return idAdjunto; }
    public String getTipoArchivo() { return TipoArchivo; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getUrlArchivo() { return urlArchivo; }
    public Mensaje getMensaje() { return mensaje; }

}