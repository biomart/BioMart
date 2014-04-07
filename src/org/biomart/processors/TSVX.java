package org.biomart.processors;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import java.util.List;
import org.biomart.common.constants.OutputConstants;
import org.biomart.objects.objects.Attribute;
import org.biomart.processors.annotations.ContentType;
import org.biomart.queryEngine.Query;
import org.biomart.queryEngine.QueryElement;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author jhsu
 *
 * HTML processor. It also takes care of dynamic URL creation.
 * the logic for adding attributes to the URL is that all (numColumns) placeholders in the URL
 * for a given attribute x are present as subsequent numColumns columns of the TSV resultset in
 * exactly the same order as they appear in the URL.
 */
@ContentType("text/html")
public class TSVX extends ProcessorImpl {
    private final HtmlCallback fn; // callback function for query engine

    public TSVX() {
        fn = new HtmlCallback();
    }

    @Override
    public Function getCallback() {
        return fn;
    }

    @Override
    public void setQuery(Query query) {
        super.setQuery(query);
        fn.initLinkVariables(query);
    }

    private class HtmlCallback implements Function<String[],Boolean>, OutputConstants {
        private final String DYNAMIC_ATTR_PATTERN = "%[a-zA-Z0-9_]+%";
        private final String PRIMARY_ATTR_PATTERN = "%s%";

        private int numColumns; // number of total columns (including pseudo attributes)
        private boolean isProcessingHeader; // flag for isProcessingHeader processing
        private boolean hasLinks = false; // whether we expect links in results or not
        private boolean[] checks; // check for link URL on column
        private String[] links; // keeps track of URLs (same size as checks array)

        public void initLinkVariables(Query query) {
            List<QueryElement> attributes = query.getOriginalAttributeOrder();
            numColumns = attributes.size();

            // Track a list of checks and their corresponding links
            this.checks = new boolean[numColumns];
            this.links = new String[numColumns];

            // If isProcessingHeader==1, then we should treat first row as isProcessingHeader and bypass URL injection
            isProcessingHeader = query.hasHeader();

            // Figure out which column needs URL injection
            for (int i=0; i< attributes.size(); i++) {
                QueryElement element = attributes.get(i);
                String url = ((Attribute)element.getElement()).getLinkOutUrl(element.getDataset().getName());
                if (url != null && !"".equals(url)) {
                    this.hasLinks = true;
                    this.links[i] = url;
                    this.checks[i] = true;
                }
            }
        }

        @Override
        public Boolean apply(String[] row) {
            // The actual row we print out
            String[] processedRow;

            if (hasLinks) {
                // Will have nulls, which we will skip over during join
                processedRow = new String[numColumns]; 

                for (int i=0; i<numColumns; i++) {
                    // This column has a link
                    if (checks[i]) {
                        String url = links[i];
                        String linkText = row[i];

                        // Replace first occurrence with primary attribute (link text)
                        url = url.replaceAll(PRIMARY_ATTR_PATTERN, linkText);

                        // While there are still strings to replace, keep consuming columns
                        while (url.matches(".*?" + DYNAMIC_ATTR_PATTERN + ".*?")) {
                            url = url.replaceFirst(DYNAMIC_ATTR_PATTERN, row[++i]);
                        }

                        // Ignore link if we're on isProcessingHeader or linkText is empty (it's meaningless)
                        // Otherwise we print out the URL along with link text
                        if (!isProcessingHeader & !isNullOrEmpty(linkText)) {
                            processedRow[i] = "<a href=\"" + url + "\">" + linkText + "</a>";
                        } else {
                            processedRow[i] = linkText;
                        }
                    } else {
                        // this column doesn't have link URL
                        processedRow[i] = row[i];
                    }
                }

            } else {
                // We don't expect links in results so just set processedRow to original row
                processedRow = row;
            }

            // Not writing isProcessingHeader anymore
            isProcessingHeader = false;

            // Skip over nulls (might have been consumed during building link URL)
            writeSilently(out, (
                Joiner
                    .on('\t')
                    .skipNulls()
                    .join(processedRow)
                + "\n").getBytes());

            return false;
        }
    }

}
