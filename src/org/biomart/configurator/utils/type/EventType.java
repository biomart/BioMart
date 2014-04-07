package org.biomart.configurator.utils.type;

public enum EventType {
	Update_DataSet,
	Update_Schema,
	Update_TreeLocation,
	Update_PartitionTable,
	Update_McViewType,
	Update_McGuiType,
	Update_SchemaGUI,
	Update_DSColumnMasked,
	Update_DmTableMasked,
	
	Request_DimensionPartition,
	Request_MartRunner,
	Request_NewMartStart,
	Request_NewMartEnd,
	Request_NewLocation,
	
	Remove_PartitionTable,
	
	Synchronize_Schema,
	Synchronize_Dataset;
}

