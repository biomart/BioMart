package org.biomart.configurator.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

public class PartitionMatchModel {
	private String sourceSourceString;
	private String sourceTargetString;
	private List<ArrayList<String>> sourceRangeList;
	private List<String> targetRangeList;
	private Map<String,PartitionTableModel> sourcePTMap;
	private Map<String,PartitionTableModel> targetPTMap;
	
	public PartitionMatchModel(Map<String,PartitionTableModel> source, Map<String,PartitionTableModel> target,
			String sourceString, String targetString) {
		this.sourcePTMap = source;
		this.targetPTMap = target;
		this.sourceSourceString = sourceString;
		this.sourceTargetString = targetString;
	}
	
	public void setSourceString(String sourceString) {
		this.sourceSourceString = sourceString;
	}
	
	public String getSourceString() {
		return sourceSourceString;
	}
	
	public void setTargetString(String targetString) {
		this.sourceTargetString = targetString;
	}
	
	public String getTargetString() {
		return sourceTargetString;
	}
	
	//the targetPTMap should not be empty
	public List<PartitionMatchBean> getCombinationWithBoolean() {
		List<PartitionMatchBean> pmbList = new ArrayList<PartitionMatchBean>();
		sourceRangeList = this.parseRangeString(sourceTargetString);
		if(sourceRangeList == null || sourceRangeList.isEmpty()) {
			Set<Entry<String,PartitionTableModel>> enties = targetPTMap.entrySet();
			for(Map.Entry<String,PartitionTableModel> entry:enties) {
				String key = entry.getKey();
				PartitionTableModel ptm = entry.getValue();
				for(int i=0; i<ptm.getRowsList().length;i++) {
					PartitionMatchBean pmb = new PartitionMatchBean();
					pmb.setCheck(true); //true by default;
					pmb.setSource(null);
					Integer row = (Integer)ptm.getRowsList()[i];
					pmb.setTarget(new PTCellObject(ptm.getCell(row, 1),key,""+row,"1"));
					pmbList.add(pmb);					
				}
			}
		} else {
			//FIXME so far, only consider one level partition, will expand it later
			String sourcePTName,sourceCellName;
			for(ArrayList<String> sal:sourceRangeList) {
				Set<Entry<String,PartitionTableModel>> targetEnties = targetPTMap.entrySet();
				sourcePTName = this.getRealPartitionName(sal.get(0), false);
				int sRow = this.getPTRowInt(sal.get(0));
				sourceCellName = sourcePTMap.get(sourcePTName).getCell(sRow, 1); 
				for(Map.Entry<String,PartitionTableModel> entry:targetEnties) {
					String key = entry.getKey();
					PartitionTableModel ptm = entry.getValue();
					for(int i=0; i<ptm.getRowsList().length;i++) {
						PartitionMatchBean pmb = new PartitionMatchBean();
						pmb.setCheck(true); //true by default;
						pmb.setSource(new PTCellObject(sourceCellName,sourcePTName,""+sRow,"1"));
						Integer row = (Integer)ptm.getRowsList()[i];
						pmb.setTarget(new PTCellObject(ptm.getCell(row, 1),key,""+row,"1"));
						pmbList.add(pmb);					
					}
				}
			}
		}
		return pmbList;
	}
	
	private String getRealPartitionName(String pName,boolean rowBool) {
		int index = pName.lastIndexOf("R");
		if(rowBool)
			return pName.substring(index);
		else 
			return pName.substring(0,index);
	}
	
	private int getPTRowInt(String pName) {
		int index = pName.lastIndexOf("R");
		int iRow = -1;
		try {
			String row = pName.substring(index+1);
			iRow = Integer.parseInt(row);
		}catch(Exception e) {
			//TODO
		}
		return iRow;
	}
	
	
	private List<ArrayList<String>> parseRangeString(String range) {
		if(range.equals(""))
			return null;
		List<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
		StringTokenizer st = new StringTokenizer(range,"[]");
		

		while(st.hasMoreTokens()) {
			String[] sArray = st.nextToken().split(":");
			ArrayList<String> al = new ArrayList<String>(Arrays.asList(sArray));
			list.add(al);
		}
		return list;
	}
	
}