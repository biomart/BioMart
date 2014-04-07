package org.biomart.dino.converters;

import java.io.OutputStream;

import org.biomart.objects.objects.Attribute;

/**
 * 
 * @author luca
 *
 * This class can be used for to Ensembl Id translation but also to retrieve
 * annotation terms.
 * 
 */
public class AttributeListEnsemblIdConverter implements Converter {

    @Override
    public Converter run() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Converter setAttributes(Attribute attributeList) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Using this method is optional since for retrieving all the annotation
     * terms must not be set any filter.
     * It can be invoked multiple times to set different filters for the 
     * same query.
     * 
     * @param name The name of the filter.
     * @param value The value of the filter.
     */
    @Override
    public Converter setFilter(String name, String value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Converter setOutput(OutputStream out) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * This method allows to get the name of the dataset used for the query.
     * 
     */
    @Override
    public String getDatasetName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * This method allows to get the name of the configuration used for the query.
     * 
     */
    @Override
    public String getConfigName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * It returns a different Attribute based on the translation required:
     * Annotation, human ids, mouse ids etc.
     * 
     * @return The Attribute used for translation.
     */
    @Override
    public Attribute getAttributeForTranslation() {
        // TODO Auto-generated method stub
        return null;
    }

}
