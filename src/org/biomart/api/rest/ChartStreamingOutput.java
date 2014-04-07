package org.biomart.api.rest;

import org.biomart.api.chart.ChartUtilities;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author jhsu
 */
public class ChartStreamingOutput implements StreamingOutput {
    private final JFreeChart chart;
    private final int width;
    private final int height;
    private final String format;

    public ChartStreamingOutput(final JFreeChart chart, final int width, final int height,
            final String format) {
        this.chart = chart;
        this.width = width;
        this.height = height;
        this.format = format.toUpperCase();
    }

    public void write(OutputStream out) throws IOException {
        try {
            Method method = this.getClass().getDeclaredMethod("writeChartAs" + this.format,
                    new Class[] {OutputStream.class});
            method.invoke(this, new Object[] {out});
        } catch (NoSuchMethodException e) {
            throw new WebApplicationException(400);
        } catch (IllegalAccessException e) {
            throw new WebApplicationException(400);
        } catch (InvocationTargetException e) {
            throw new WebApplicationException(400);
        }
    }

    private void writeChartAsPNG(OutputStream out) throws IOException {
        ChartUtilities.writeChartAsPNG(out, this.chart, this.width, this.height);
    }
    private void writeChartAsJPEG(OutputStream out) throws IOException {
        ChartUtilities.writeChartAsJPEG(out, this.chart, this.width, this.height);
    }
}
