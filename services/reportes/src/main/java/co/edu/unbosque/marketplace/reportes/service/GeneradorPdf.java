package co.edu.unbosque.marketplace.reportes.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import co.edu.unbosque.marketplace.reportes.dto.ReporteTabla;

@Component
public class GeneradorPdf {

    private static final Color COLOR_ENCABEZADO = new Color(230, 230, 230);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float TAMANO_TITULO = 14;
    private static final float TAMANO_TEXTO = 10;

    private final Font fuenteTitulo;
    private final Font fuenteTexto;
    private final Font fuenteEncabezado;

    public GeneradorPdf() {
        BaseFont base = cargarFuente();
        this.fuenteTitulo = new Font(base, TAMANO_TITULO, Font.BOLD);
        this.fuenteEncabezado = new Font(base, TAMANO_TEXTO, Font.BOLD);
        this.fuenteTexto = new Font(base, TAMANO_TEXTO, Font.NORMAL);
    }

    public byte[] generar(ReporteTabla tabla) {
        return generar(tabla, true);
    }

    byte[] generar(ReporteTabla tabla, boolean comprimir) {
        Document documento = new Document(PageSize.A4);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setCompressionLevel(comprimir ? 5 : 0);
            documento.open();
            documento.add(new Paragraph(tabla.titulo(), fuenteTitulo));
            documento.add(crearTabla(tabla));
            documento.add(new Paragraph("Generado el " + LocalDateTime.now().format(FORMATO_FECHA), fuenteTexto));
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No fue posible generar el archivo PDF", e);
        }
    }

    private PdfPTable crearTabla(ReporteTabla tabla) {
        List<String> encabezados = tabla.encabezados();
        PdfPTable tablaPdf = new PdfPTable(encabezados.size());
        tablaPdf.setWidthPercentage(100);
        for (String encabezado : encabezados) {
            tablaPdf.addCell(celda(encabezado, fuenteEncabezado, true));
        }
        for (List<String> fila : tabla.filasParaMostrar()) {
            for (String valor : fila) {
                tablaPdf.addCell(celda(valor, fuenteTexto, false));
            }
        }
        return tablaPdf;
    }

    private static PdfPCell celda(String texto, Font fuente, boolean encabezado) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        if (encabezado) {
            celda.setBackgroundColor(COLOR_ENCABEZADO);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        }
        return celda;
    }

    private static BaseFont cargarFuente() {
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible cargar la fuente del archivo PDF", e);
        }
    }
}
