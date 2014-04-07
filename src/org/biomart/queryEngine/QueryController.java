package org.biomart.queryEngine;

import org.biomart.common.exceptions.BioMartException;

import java.io.EOFException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Date;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.common.resources.Log;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.biomart.common.exceptions.TechnicalException;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.configurator.utils.McUtils;
import org.biomart.dino.DinoHandler;
import org.biomart.objects.objects.Attribute;
import org.biomart.processors.ProcessorInterface;
import org.biomart.processors.ProcessorRegistry;

import static org.apache.commons.lang.StringEscapeUtils.unescapeJava;

/**
 *
 * @author Syed Haider, jhsu
 *
 * This is the central component of QueryEngine that connects all query abstractions
 * together. The flow starts with an incoming XML query getting passed over to
 * QueryValidator. If validation is ok, query is sent to QuerySplitter where
 * its turned into a Query object with connecting links between SubQuery objects.
 * The Query object is then passed on to QueryPlanner for subsequent reordering for
 * optimal execution (TODO).
 * Lastly, the Qeury object is passed on to the ProcessorInterface along with a
 * QueryRunner and output handle object. The rationale in passing the output
 * handle to ProcessorInterface is to enable individual processors to hook the
 * results stream and manipulate it if they like. Basically, the QueryRunner object,
 * although created here (for keeping control over it), is used in the indivudal
 * processor by its runQuery method.
 *
 * Furthermore, The dynamic class loading of the specified processor is done here,
 * which essentially calls the processor object by its name. Hence,
 * all new processors can automagically be linked to the system without having
 * to modify anything in the Query Engine as long as the processor object lives in the
 * org.biomart.processors package.
 *
 * Final attribute ordering including Pseudo Attributes and their ordering is
 * generated here
 * 
 */
public final class QueryController {
	private final MartRegistry registryObj;
    private final String userGroup;
    private final ProcessorInterface processorObj;
	private final Document queryXMLobject;
	private final QueryValidator queryValidator;
	private final Query query;
	private boolean isCountQuery;
	
	private String[] mimes;
	private String user;

    public QueryController(String xml, final MartRegistry registryObj, String user, String[] mimes, boolean isCountQuery) {
		Log.info("Incoming XML query: " + xml);

		this.mimes = mimes;
		this.user = user;
		
		this.isCountQuery = isCountQuery;
		this.registryObj = registryObj;
        userGroup = McUtils.getUserGroup(registryObj, user, "").getName();
        
		InputStream in;

		try {
			in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
			SAXBuilder builder = new SAXBuilder();
			InputSource is = new InputSource(in);
			is.setEncoding("UTF-8");
			queryXMLobject = builder.build(is);
			
			boolean useMasterConfig = Boolean.parseBoolean(System.getProperty("biomart.query.masterconfig"));
			if(useMasterConfig){
				String configName = queryXMLobject.getRootElement().getChild("Dataset").getAttributeValue("config");
				configName = this.registryObj.getConfigByName(configName).getMart().getMasterConfig().getName();
				queryXMLobject.getRootElement().getChild("Dataset").setAttribute("config", configName);
			}

            queryValidator = new QueryValidator(queryXMLobject, registryObj, userGroup);
            queryValidator.validateProcessor();

            processorObj = initializeProcessor();
            processorObj.preprocess(queryXMLobject); // do any preprocessing on the XML before passing to validator
            processorObj.accepts(mimes); // figure out content type

			org.jdom.Attribute count = queryXMLobject.getRootElement().getAttribute("count");
			if ( count != null && ("1".equals(count.getValue()) || "true".equals(count.getValue())) ) {
				this.isCountQuery = true;
			}

            queryValidator.setQueryDocument(queryXMLobject);
            queryValidator.validateQuery();
            
            if (queryValidator.hasDino() && queryValidator.getUseDino()) {
                    query = null;
            } else {
                query = splitQuery();
                
                Log.debug("Unplanned: " + query);

                // This is commented because it does not work anymore
                // essentially there is no query planning happening anymore
                // to be replaced with an intelligent query planner at some stage
                // query.planQuery();

                Log.debug("Planned:" + query);

                generateAttributePositions();

                Log.debug("LIMIT: " + query.getLimit());
            }
		} catch (Exception e) {
            throw new ValidationException(e.getMessage(), e);
		}
	}

    public QueryController(String xml, final MartRegistry registryObj, String user, boolean isCountQuery) {
        this(xml, registryObj, user, ArrayUtils.EMPTY_STRING_ARRAY, isCountQuery);
    }

    public void runQuery(OutputStream outputHandle) throws TechnicalException, IOException {
        long start = new Date().getTime();

        // 1. Initialize QueryRunner
        // 2. Set query on Processor
        // 3. Set outputstream on Processor
        // 4. Run query
        // 5. Call done (cleanup) on Processor
        try {
        		if (queryValidator.hasDino() && queryValidator.getUseDino()) {
        			Query q = new Query(queryValidator, false);
        			DinoHandler.runDino(q, user, mimes, outputHandle);
        		} else {
                    QueryRunner queryRunnerObj = new QueryRunner(query,
                            processorObj.getCallback(), processorObj.getErrorHandler(), isCountQuery);
    
                    processorObj.setQuery(queryRunnerObj.query);
                    processorObj.setOutputStream(outputHandle);
    
                    queryRunnerObj.runQuery();
    
                    processorObj.done();
        		}
        } catch (BioMartException e) {
            if (!(e.getCause() instanceof EOFException)) {
                throw e;
            }
        } catch (SQLException e) {
            throw new TechnicalException(e);
        } catch (InterruptedException e) {
            throw new TechnicalException(e);
		} finally {
            long end = new Date().getTime();
            Log.info(String.format("Total query time is %s ms", end-start));
        }
	}

    public String getContentType() {
        return processorObj.getContentType();
    }

    private Query splitQuery() throws FunctionalException {
		QuerySplitter querySplitterObj = new QuerySplitter(queryValidator, registryObj, isCountQuery);
		querySplitterObj.disectQuery();
		querySplitterObj.prepareLinks();
		Query q = querySplitterObj.getQuery();
		q.queryXMLobject = queryXMLobject;
		q.setPseudoAttributes(queryValidator.getPseudoAttributes());
		Log.info("Query = " + q.toString());
        return q;
	}

    private void generateAttributePositions(){
		Iterator it = query.queryPlanMap.entrySet().iterator();
		Map.Entry pair = (Map.Entry) it.next();

		Log.debug("original atts size (including pseudo atts): " + query.getOriginalAttributeOrder().size());

		List<SubQuery> plannedQuery = query.queryPlanMap.get(pair.getKey().toString());
		List<QueryElement> originalOrder = query.getOriginalAttributeOrder();
		int output_atts_index = 0;
		int pseudo_att_position = 0;

		Map<String, Integer> pseudoSeen = new HashMap<String, Integer>();

		for(QueryElement original_att : originalOrder){
			Log.debug("Original att: "+ original_att.toString() + " : "+ original_att.getDataset().getName());
			Attribute attObj = (Attribute) original_att.getElement();
			// its a Pseudo att
			if (attObj.isPseudoAttribute()){
				// assign the number -ve so it can be traced in queryRunner
				if(!pseudoSeen.containsKey(attObj.getName())){
					query.outputOrder[output_atts_index] = pseudo_att_position - 1000;
					pseudoSeen.put(attObj.getName(), pseudo_att_position-1000);
					query.outputDisplayNames[output_atts_index++] = attObj.getDisplayName(original_att.getDataset());
					pseudo_att_position++;
				} else {
					query.outputOrder[output_atts_index] = pseudoSeen.get(attObj.getName());
					query.outputDisplayNames[output_atts_index++] = attObj.getDisplayName(original_att.getDataset());
				}
			} else {
				int position = 0;
				for(SubQuery sq : plannedQuery){
					position += sq.getImportablesSize();
					position += sq.getExportablesSize();
					for(QueryElement attribute : sq.getQueryAttributeList()){
						if(attribute.getType() == QueryElementType.ATTRIBUTE){
							if(original_att.getElement().isPointer()) {
								if (((Attribute)original_att.getElement()).getPointedAttribute().getInternalName().equals(attribute.toString()) 
                                    // && ((Attribute)original_att.getElement()).getPointedDatasetList().get(0).equals(attribute.getDataset().getName())
                                    ) {
									query.outputOrder[output_atts_index] = position;
									query.outputDisplayNames[output_atts_index++] = attObj.getDisplayName(original_att.getDataset());
									break; // next attribute                       			
								}
							} else 
								if (original_att.toString().equals(attribute.toString())
										&& original_att.getDataset().getName().equals(attribute.getDataset().getName())) {
									query.outputOrder[output_atts_index] = position;
									query.outputDisplayNames[output_atts_index++] = attObj.getDisplayName(original_att.getDataset());
									break; // next attribute
								}
							position++;
						}

					}
				}
			}
		}

		for (int i : query.outputOrder) {
			Log.debug("output Order vals: "+ i);
		}
	}

    private ProcessorInterface initializeProcessor() {
        try {
            Class clazz = ProcessorRegistry.get(queryValidator.getProcessor());
            Constructor ctor = clazz.getConstructor(new Class[] {});
            ProcessorInterface p = (ProcessorInterface)ctor.newInstance(new Object[]{});

            for (String field : p.getFieldNames()) {
                String value;

                if (p.isClientDefined(field)) {
                    value = unescapeJava(queryValidator.getProcessorParams().get(field));
                } else {
                    value = unescapeJava(queryValidator.getProcessorConfig().get(field));
                }

                if (value == null) {
                    value = p.getDefaultValueForField(field);
                }

                if (value == null && p.isRequired(field)) {
                    throw new ValidationException("Processor field required but not found: " + field);

                } else if (value != null) {
                    p.setFieldValue(field, value);
                }
            }

            return p;

		} catch (Exception e) {
            throw new ValidationException(e.getMessage(), e);
		}
    }
}
