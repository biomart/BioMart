package org.biomart.dino.dinos.enrichment;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class GuiResponseCompiler {

    public static String ROOT_KEY = "data";

    // The mustache compiler clashes with angular expression format...
    // I introduced angularjs after have chose to use mustache.
    // TODO: use another template engine?
    public static void
    compile(File tpl, OutputStream out, Map<String, Object> binding) throws IOException {
        FileReader in = null;
        BufferedWriter writer = null;
        try {
//            in = new FileReader(tpl);
            writer = new BufferedWriter(new OutputStreamWriter(out));
//            DefaultMustacheFactory mf = new DefaultMustacheFactory();
//            Mustache mustache = mf.compile(in, "enrichment.html", "[", "]");
//            mustache.execute(writer, binding);
            String content = FileUtils.readFileToString(tpl);
            content = content.replaceFirst("@@.*@@", binding.get(ROOT_KEY).toString());
            writer.append(content);
            writer.flush();
        } finally {
            if (in != null) {
                in.close();
            }

            if (writer != null) {
                writer.close();
            }
        }
    }
}
