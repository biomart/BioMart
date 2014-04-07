package org.biomart.dino;

import java.util.Iterator;
import java.util.List;

import org.biomart.common.resources.Log;
import org.biomart.configurator.model.object.PartitionColumn;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.queryEngine.Query;
import org.biomart.queryEngine.QueryElement;
import org.biomart.queryEngine.QueryElementType;

public class Utils {

    /**
     * Given a query element that wraps an attribute list e, it returns the
     * attribute within the attribute list useful for any id to Ensembl gene id
     * translation.
     * 
     * NOTE: this method returns null if the operation failed because we cannot
     * create and return an empty element, nor the wrapped element as symbol of
     * fail.
     * 
     * @param qe
     *            query element that wraps an attribute list.
     * @return an attribute to use for Ensembl gene id translation or null if
     *         the operation failed.
     */
    public static Attribute 
    getAttributeForEnsemblGeneIdTranslation(QueryElement qe) {
        List<Attribute> eList = getAttributeList(qe);
        return eList.isEmpty() ? null : eList.get(0);
    }
    
    public static Attribute 
    getAttributeForEnsemblGeneIdTranslation(Attribute a) {
        Log.debug("Utils::getAttributeForEnsemblGeneIdTranslation("
                + a.getName() + ")");
        List<Attribute> eList = a.getAttributeList();
        return eList.isEmpty() ? null : eList.get(0);
    }
    
    public static Attribute 
    getAttributeForEnsemblGeneIdTranslation(Query q) {
        QueryElement qe;
        List<QueryElement> l = q.getQueryElementList();
        Iterator<QueryElement> it = l.iterator();
        while(it.hasNext()) {
            if ((qe = it.next()).getType() != QueryElementType.ATTRIBUTE) {
                return (Attribute) qe.getElement();
            }
        }
        
        return null;
    }

    /**
     * Same as getAttributeForEnsemblGeneIdTranslation except that this is for
     * species translation.
     * 
     * @param qe
     * @return
     */
    public static Attribute getAttributeForAnnotationRetrieval(QueryElement qe) {
        List<Attribute> eList = getAttributeList(qe);
        return eList.size() > 1 ? eList.get(1) : null;
    }
    
    public static Attribute getAttributeForAnnotationRetrieval(Attribute a) {
        List<Attribute> eList = a.getAttributeList();
        return eList.size() > 1 ? eList.get(1) : null;
    }
    
    public static Attribute 
    getAttributeForAnnotationRetrieval(Query q) {
        QueryElement qe;
        List<QueryElement> l = q.getQueryElementList();
        Iterator<QueryElement> it = l.iterator();
        while(it.hasNext()) {
            if ((qe = it.next()).getType() != QueryElementType.ATTRIBUTE) {
                return (Attribute) qe.getElement();
            }
        }
        
        return null;
    }

    

    private static List<Attribute> getAttributeList(QueryElement qe) {
        Element e = qe.getElement();
        Attribute a;

        if (!(e instanceof Attribute)) {
            return null;
        } else {
            a = (Attribute) e;
        }

        return a.getAttributeList();
    }

    
    public static String getDatasetName(Attribute attributeList) {
        Attribute a = getAttributeForEnsemblGeneIdTranslation(attributeList);
        if (a == null)
            return "";
        
        String name = a.getName(), namespaces[] = name.split("_"),
               ns = namespaces[0];
        
        Config cfg = a.getParentConfig();
        Mart mart = cfg.getMart();
        PartitionTable t = mart.getSchemaPartitionTable();
        PartitionColumn c = t.getColumnObject(5);
        
        return matchPrefix(c.getColumnList(), ns);
    }
    
    /**
     * 
     * It match prefix with an underscore appended against lines
     * 
     * @param rows
     * @param prefix Is the prefix to match against lines. It MUST NOT contain underscores.
     * @return
     */
    private static String matchPrefix(List<String> lines, String prefix) {
        System.out.println("matchPrefix(..., "+ prefix + ")");
        String match = "", pre = prefix + "_";
        
        if (lines != null && prefix != null) {
            for (String row : lines) {
                if (row.startsWith(pre))
                    return row;
            }
        }
        
        return match;
    }

    public static String 
    getConfigName(Attribute attributeList) {
        Attribute a = getAttributeForAnnotationRetrieval(attributeList);
        if (a == null)
            return "";
        
        Config cfg = a.getParentConfig();
        
        return cfg.getName();
    }

    public static String getDatasetName(QueryElement e) {
        String s = e.getDataset().getName();
        return s == null ? "" : s;
    }

}




































