package org.biomart.configurator.linkIndices;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Proxy;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.resources.Log;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.queryEngine.OperatorType;
import org.biomart.queryEngine.QueryElement;
import org.biomart.queryEngine.SubQuery;

public class LinkIndices {
	private Map<String,Set<String>> indexMap;
    private final Proxy proxy;
	private final boolean isExplainQuery;

	public LinkIndices(){
		this(false);
	}

	public LinkIndices(boolean isExplainQuery){
		this.isExplainQuery = isExplainQuery;
        this.proxy = Proxy.NO_PROXY;
		this.indexMap = new HashMap<String,Set<String>>();
	}

	public Set<String> getIndex(Dataset dataset, QueryElement importable){
		
        String uniqueKey = dataset.getParentMart().getName() + "_" + dataset.getName()
            + "__" + importable.getLinkDataset().getParentMart().getName() + "_" + importable.getLinkDataset().getName() + ".txt";

        Log.info("using linkindices");

        if (this.indexMap.get(uniqueKey) == null) {
            Log.info("looking for index file in fileSystem (only first time trip): " + uniqueKey);
            Log.info("GETWD: "+ System.getProperty("user.dir"));
            try {
                Log.info("reading indices from fileSystem: " + uniqueKey);
            	FileReader fr = new FileReader(McGuiUtils.INSTANCE.getDistIndicesDirectory().getCanonicalPath()  + "/" + uniqueKey);
                        
            	HashSet<String> linkIndex = new HashSet<String>();
            	BufferedReader br = new BufferedReader(fr);
            	String row;
            	while((row = br.readLine()) != null) {
            		linkIndex.add(Arrays.asList(row.split("\t",0)).get(0));
            	}
            	fr.close();
            	this.indexMap.put(uniqueKey, linkIndex);
            } catch (FileNotFoundException e){
            	Log.error("index file not found under: " + "/registry/linkindices/");
            } catch (IOException e) {
            	// TODO Auto-generated catch block
            	e.printStackTrace();
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
        return this.indexMap.get(uniqueKey);
	}

	public void addIndex(Link link, List<String> datasets){
		Link otherLink = McUtils.getOtherLink(link);
		if(otherLink == null) {
			JOptionPane.showMessageDialog(null, "link error", "error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		for(String dataset: datasets){
			SubQuery subquery = new SubQuery(isExplainQuery);
			subquery.setLimit(-1);
			for(Filter filter: otherLink.getFilterList()){
				if(filter.getQualifier() == OperatorType.E){
					subquery.addAttribute(new QueryElement(filter.getAttribute(), link.getParentConfig().getMart().getDatasetByName(dataset)));
					break;
				}
			}
			Log.debug("LinkIndices query: " + subquery.getQuery());
			try {
				Log.debug("LinkIndices: retrieving results");
				subquery.executeQuery();
				int batchsize = 5000;
				int start = 1;
				int resultsize = batchsize;

				Set<List<String>> linkIndexResults = new HashSet<List<String>>();
				Set<String> linkIndex = new HashSet<String>();
				while(resultsize >= batchsize){
					//Log.error("start: " + start);
					//Log.error("end: " + (start+batchsize));
					List<List<String>> resultBatch = subquery.getResults(start, start+batchsize-1);
					linkIndexResults.addAll(resultBatch);
					resultsize = resultBatch.size();
					//Log.error("size: " + resultsize);
					//Log.error(test.toString());
					start+=batchsize;
				}
				Log.debug("LinkIndices: results retrieved");
				File registryIndiceFile = McGuiUtils.INSTANCE.getIndicesDirectory();
				FileWriter registryWriter = null;
				String pointeddsName = link.getPointedDataset();
				String tmpName = pointeddsName;
				if(McUtils.hasPartitionBinding(pointeddsName)) {
					PartitionTable pt = link.getParentConfig().getMart().getSchemaPartitionTable();
					int row = pt.getRowNumberByDatasetName(dataset);
					tmpName = McUtils.getRealName(pt, row, pointeddsName);
				}
				String fileName = link.getParentConfig().getMart().getName() + "_" + dataset + "__"
					+ link.getPointedMart().getName() + "_" + tmpName + ".txt";
				if(registryIndiceFile != null) {
					System.out.println(registryIndiceFile.getName());
					registryWriter = new FileWriter(registryIndiceFile+"/"+fileName);
				}
                FileWriter writer = new FileWriter(McGuiUtils.INSTANCE.getDistIndicesDirectory().getCanonicalPath() +"/"+
                    fileName);

				for(List<String> row : linkIndexResults){
                    Log.debug("> "+ row.toString());
                    if(row.contains(null) != true) {
                        for(String entry : row){
                			writer.write(entry);
                			writer.write('\t');
                			if(registryWriter!=null) {
                				registryWriter.write(entry);
                				registryWriter.write('\t');
                			}
                         linkIndex.add(row.get(0));
                		}
                		writer.write('\n');
                		if(registryWriter!=null) {
                			registryWriter.write('\n');
                		}
                    }
				}
				writer.close();
				if(registryWriter!=null) {
					registryWriter.close();
				}

                this.indexMap.put(dataset + "_" + link.getName(), linkIndex);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "sql error", "error", JOptionPane.ERROR_MESSAGE);
			} catch (TechnicalException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
