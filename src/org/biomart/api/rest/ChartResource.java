package org.biomart.api.rest;

import org.biomart.api.chart.ChartUtilities;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.biomart.common.resources.Log;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;

/**
 * @author jhsu
 */
@Path("martservice/chart")
public class ChartResource {
    private static final int MAX_AGE = 24 * 60 * 60;
    private static final String DEFAULT_FORMAT = "png";

    private @Context ServletContext servletContext;
    private @Context HttpServletRequest request;

    @Path("bar/stacked")
    @GET
    @Produces({"image/*", "application/pdf"})
    public Response getStackedBarChart(
            @QueryParam("f") @DefaultValue(DEFAULT_FORMAT) String format,
            @QueryParam("t") @DefaultValue("") String title,
            @QueryParam("cl") @DefaultValue("") String categoryLabel,
            @QueryParam("sl") @DefaultValue("") String seriesLabel,
            @QueryParam("ck") String categoryKeys,
            @QueryParam("sk") String seriesKeys,
            @QueryParam("d") String data,
            @QueryParam("w") @DefaultValue("400") Integer width,
            @QueryParam("h") @DefaultValue("300") Integer height) {
        return drawStackedBarChart(format.toLowerCase(), title, categoryLabel, seriesLabel,
                categoryKeys, seriesKeys, data, width, height);
        
    }
    
    @Path("bar")
    @GET
    @Produces({"image/*", "application/pdf"})
    public Response getBarChart(
            @QueryParam("f") @DefaultValue(DEFAULT_FORMAT) String format,
            @QueryParam("t") @DefaultValue("") String title,
            @QueryParam("cl") @DefaultValue("") String categoryLabel,
            @QueryParam("sl") @DefaultValue("") String seriesLabel,
            @QueryParam("ck") String categoryKeys,
            @QueryParam("sk") String seriesKeys,
            @QueryParam("d") String data,
            @QueryParam("w") @DefaultValue("400") Integer width,
            @QueryParam("h") @DefaultValue("300") Integer height) {

        return drawBarChart(format.toLowerCase(), title, categoryLabel, seriesLabel,
                categoryKeys, seriesKeys, data, width, height);
    }

    private Response drawBarChart(String format,String title,
    		String categoryLabel, String seriesLabel, String categoryKeys,
    		String seriesKeys, String data, int width, int height) {
    	
    	final JFreeChart chart;
    	CategoryDataset dataset = null;
    	
    	final double[][] chartData = ChartUtilities.getDataFromString(data);
    	
    	final String[] chartSeriesKeys = StringUtils.split("Affected Pathways", ',');
            	
    	//final String[] chartCategoryKeys = StringUtils.split(categoryKeys, ',');
    	//using group separator to split ck values
    	final String[] chartCategoryKeys = StringUtils.split(categoryKeys, (char)29);
        List<String> list = Arrays.asList(chartCategoryKeys);
        Collections.reverse(list);
        list.toArray(chartCategoryKeys);
        try{
        	dataset = DatasetUtilities.createCategoryDataset(chartSeriesKeys, chartCategoryKeys, chartData);
        }catch(IllegalArgumentException iae){
        	iae.printStackTrace();
        }
    	chart = ChartFactory.createStackedBarChart(
            title,
            seriesLabel,
            categoryLabel,
            dataset,
            PlotOrientation.HORIZONTAL,
            false,                        // include legend
            true,                        // tooltips
            false                        // urls
        );
    	ChartUtilities.styleStackedBarChart(chart);
    	
    	File file = getOrCreateFile(chart, format, width, height);
        String mt = new MimetypesFileTypeMap().getContentType(file);
    	
        return Response.ok(file, mt)
        .header("Cache-Control",  String.format("max-age=%s; must-revalidate", MAX_AGE))
        .header("Vary", "Cookie,Accept-Encoding,User-Agent")
        .build();
    }
    
    private Response drawStackedBarChart(String format, String title, 
            String categoryLabel, String seriesLabel, String categoryKeys, 
            String seriesKeys, String data, int width, int height) {

        final JFreeChart chart;
        CategoryDataset dataset = null;

        final double[][] chartData = ChartUtilities.getDataFromString(data);
        
        // Create dataset
        /*if(seriesKeys == null)
        {
        	seriesKeys = "Chart";
        }*/
        try{
	        if (!"".equals(seriesKeys)) {
	            final String[] chartSeriesKeys = StringUtils.split(seriesKeys, ',');
	            final String[] chartCategoryKeys = StringUtils.split(categoryKeys, (char)29);
	            dataset =  DatasetUtilities.createCategoryDataset(chartSeriesKeys, chartCategoryKeys, chartData);
	        } else {
	            dataset =  DatasetUtilities.createCategoryDataset("", "", chartData);
	        }
        }catch(IllegalArgumentException iae){
        	iae.printStackTrace();
        }
        // Plot chart
        chart = ChartFactory.createStackedBarChart(
            title,
            seriesLabel,
            categoryLabel,
            dataset,
            PlotOrientation.HORIZONTAL,
            true,                        // include legend
            true,                        // tooltips
            false                        // urls
        );
        ChartUtilities.styleStackedBarChart(chart);

        File file = getOrCreateFile(chart, format, width, height);
        String mt = new MimetypesFileTypeMap().getContentType(file);

        return Response.ok(file, mt)
                .header("Cache-Control",  String.format("max-age=%s; must-revalidate", MAX_AGE))
                .header("Vary", "Cookie,Accept-Encoding,User-Agent")
                .build();
    }

    private File getTempDir() {
        String tmpPath = servletContext.getRealPath("/tmp");
        File f;
        f = new File(tmpPath);
        if (!f.exists()) f.mkdir();
        return f;
    }

    private String getTempNameHash() {
        String reqStr = request.getRequestURI() + request.getQueryString();
        String name = null; 

        try {
            byte[] bytesOfMessage = reqStr.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytesOfMessage);
            BigInteger bigInt = new BigInteger(1, digest);
            name  = bigInt.toString(16);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return name;
    }

    private File getOrCreateFile(JFreeChart chart, String format, int width, int height) {
        File dir = getTempDir();
        String prefix = getTempNameHash();
        String suffix = "."+format;
        File file = new File(dir, prefix+suffix);

        try {
            if (file.createNewFile() || file.length() == 0) {
                Log.info("New chart file written to " + file.getCanonicalPath());
                OutputStream out;
                out = new BufferedOutputStream(new FileOutputStream(file));
                ChartUtilities.writeChart(format, out, chart, width, height);
                out.close();
            }
            file.deleteOnExit();
        } catch (NoSuchMethodException e) {
            throw new WebApplicationException(
                Response.status(400).entity("Format \"" + format + "\" is not supported").build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(
                Response.serverError().build()
            );
        }

        return file;
    }
}
