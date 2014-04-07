package org.biomart.api.chart;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang.StringUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jibble.epsgraphics.EpsGraphics2D;
import org.w3c.dom.DOMImplementation;

/**
 *
 * @author jhsu
 */
public class ChartUtilities implements ChartConstants {
    public static void writeChart(String format, OutputStream out, JFreeChart chart,
            int width, int height) throws NoSuchMethodException,
            ClassNotFoundException, IllegalAccessException {

        Class<?> cls = Class.forName("org.biomart.api.chart.ChartUtilities");
        Method method = cls.getMethod("writeChartAs" + format.toUpperCase(), new Class[]{
            OutputStream.class, JFreeChart.class, Integer.class, Integer.class
        });

        try {
            method.invoke(null, new Object[] {out, chart, width, height});
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
        }
    }

    public static void writeChartAsPNG(OutputStream out, JFreeChart chart,
            Integer width, Integer height) throws IOException {
        org.jfree.chart.ChartUtilities.writeChartAsPNG(out, chart, width, height);
    }

    public static void writeChartAsJPEG(OutputStream out, JFreeChart chart,
            Integer width, Integer height) throws IOException {
        org.jfree.chart.ChartUtilities.writeChartAsJPEG(out, chart, width, height);
    }

    /*
     * See: http://www.jfree.org/phpBB2/viewtopic.php?p=50534&sid=82acabeaba160d3153b4da742e6caff0#p50534
     */
    public static void writeChartAsPDF(OutputStream out, JFreeChart chart,
            Integer width, Integer height) throws IOException {
        try {
            Rectangle pagesize = new Rectangle(width, height); 
            Document document = new Document(pagesize, 50, 50, 50, 50); 
            PdfWriter writer = PdfWriter.getInstance(document, out); 
            document.open(); 
            PdfContentByte cb = writer.getDirectContent(); 
            PdfTemplate tp = cb.createTemplate(width, height); 
            Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper()); 
            Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height); 
            chart.draw(g2, r2D); 
            g2.dispose(); 
            cb.addTemplate(tp, 0, 0); 
            document.close(); 
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /*
     * See: http://www.jfree.org/phpBB2/viewtopic.php?p=50534&sid=82acabeaba160d3153b4da742e6caff0#p50534
     */
    public static void writeChartAsEPS(OutputStream out, JFreeChart chart,
            Integer width, Integer height) throws IOException {
        Graphics2D g = new EpsGraphics2D();
        chart.draw(g, new java.awt.Rectangle(width, height));
        out.write(g.toString().getBytes());
        out.close();
    }

    /*
     * See: http://www.jfree.org/phpBB2/viewtopic.php?p=50534&sid=82acabeaba160d3153b4da742e6caff0#p50534
     */
    public static void writeChartAsSVG(OutputStream out, JFreeChart chart,
            Integer width, Integer height) throws IOException {
        DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
        org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setSVGCanvasSize(new Dimension(width, height));
        chart.draw(svgGenerator,new java.awt.Rectangle(width, height));

        Writer writer = new BufferedWriter(new OutputStreamWriter(out));
        svgGenerator.stream(writer, false);
    }

    public static void writeChartAsTIFF(OutputStream out, JFreeChart chart,
            Integer width, Integer height) throws IOException {
        BufferedImage chartImage = chart.createBufferedImage(width, height, null); 
        ImageIO.write(chartImage, "tif", out);
    }


    public static void styleStackedBarChart(JFreeChart chart) {
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
        plot.setRangeGridlinePaint(Color.DARK_GRAY);
        plot.setBackgroundPaint(Color.WHITE);
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setBaseItemLabelsVisible(true);
        for (int i=0; i<BAR_SERIES_COLOURS.length; i++) {
            renderer.setSeriesPaint(i, BAR_SERIES_COLOURS[i]);
        }
    }

    public static double[][] getDataFromString(String str) {
        final String[] rows = StringUtils.split(str, '|');
        final double[][] chartData = new double[rows.length][];

        // Initialize data rows
        for (int i=0; i<rows.length; i++) {
            final String[] row = StringUtils.split(rows[i], ',');
            chartData[i] = new double[row.length];
            for (int j=0; j<row.length; j++) {
                // Parse as single-precision float to avoid infinite loop bug
                // http://www.exploringbinary.com/java-hangs-when-converting-2-2250738585072012e-308/
                chartData[i][j] = new Float(row[j]).doubleValue();
            }
        }
        return chartData;
    }

}
