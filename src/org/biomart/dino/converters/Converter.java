package org.biomart.dino.converters;

import java.io.IOException;
import java.io.OutputStream;

import org.biomart.objects.objects.Attribute;

public interface Converter {

    public Converter run() throws IOException;
    
    public Converter setAttributes(Attribute attributeList);
    
    public Converter setFilter(String name, String value);
    
    public Converter setOutput(OutputStream out);
    
    /**
     * 
     * @return The dataset name used within the query for translation.
     */
    public String getDatasetName();
    
    /**
     * 
     * @return The configuration name used within the query for translation.
     */
    public String getConfigName();
    
    /**
     * 
     * It is used a different Attribute based on the translation type:
     * for annotations, human, mouse etc.
     * 
     * @return The Attribute used to understand what translation to do and do it. 
     */
    public Attribute getAttributeForTranslation();
    
    
}
