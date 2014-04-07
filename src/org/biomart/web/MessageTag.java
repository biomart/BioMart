package org.biomart.web;

import java.io.IOException;
import java.util.ResourceBundle;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jhsu
 */
public class MessageTag  implements Tag {
	private PageContext pageContext = null;
	private Tag parent = null;
    private String code = null;
    private String empty = null;
    private boolean capitalize = false;
    private boolean plural = false;

    public void setCode(String code) {
        this.code = code;
    }

    public void setEmpty(String empty) {
        this.empty = empty;
    }

    public void setCapitalize(String capitalize) {
        this.capitalize = Boolean.parseBoolean(capitalize);
    }

    public void setPlural(String plural) {
        this.plural = Boolean.parseBoolean(plural);
    }

   @Override
	public int doStartTag() throws JspException {
        try {
            ResourceBundle messages = GuiceServletConfig._messages;
            String value;
            String key =  code.replaceAll("\\s+","_").toLowerCase();

            if (messages.containsKey(key)) {
                value = messages.getString(key);
            } else if (empty != null) {
                // If empty text is set then just print it and return
                this.pageContext.getOut().write(empty);
                return SKIP_BODY;

            } else {
                value = code.replaceAll("_", " ");
            }

            if (plural) {
                if (messages.containsKey(key+"_plural")) {
                    value = messages.getString(key+"_plural");
                } else {
                    value = value + "s";
                }
            }

            if (capitalize) {
                value = StringUtils.capitalize(value);
            }

            this.pageContext.getOut().write(value);
        } catch (IOException ioe) {
            throw new JspException("Error: IOException while writing to client");
        }
		return SKIP_BODY;
	}

   @Override
   public int doEndTag() throws JspException {
      return EVAL_PAGE;
   }

   @Override
   public void release() {
   }

   @Override
   public void setPageContext(PageContext pageContext) {
      this.pageContext = pageContext;
   }

   @Override
   public void setParent(Tag parent) {
      this.parent = parent;
   }

   @Override
   public Tag getParent() {
      return parent;
   }
}
