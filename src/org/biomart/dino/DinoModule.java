package org.biomart.dino;

import org.apache.commons.lang.StringUtils;
import org.biomart.api.Portal;
import org.biomart.api.Query;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.dino.annotations.BaseConfigDir;
import org.biomart.dino.annotations.EnrichmentConfig;
import org.biomart.dino.command.HypgCommand;
import org.biomart.dino.command.HypgRunner;
import org.biomart.dino.command.ShellRunner;
import org.biomart.dino.dinos.enrichment.GuiResponseCompiler;
import org.biomart.dino.querybuilder.JavaQueryBuilder;
import org.biomart.dino.querybuilder.QueryBuilder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;

public class DinoModule extends AbstractModule {

    @Override
    protected void configure() {
        String s = System.getProperty("file.separator"),
                baseDir = StringUtils.join(new String[] {
                        System.getProperty("biomart.basedir"),
                        System.getProperty("file.separator"),
                        "conf", "dinos"
                }, s),
                p = StringUtils.join(new String[] {
                        baseDir, "enrichment", "EnrichmentDino.json"
                }, s);
        
        
        bind(String.class)
            .annotatedWith(BaseConfigDir.class)
            .toInstance(baseDir);
        
        
        bind(QueryBuilder.class)
            .annotatedWith(Names.named("java_api"))
            .to(JavaQueryBuilder.class);

        bind(HypgCommand.class);
        bind(ShellRunner.class)
            .annotatedWith(EnrichmentConfig.class)
            .to(HypgRunner.class);

        bind(String.class)
            .annotatedWith(EnrichmentConfig.class)
            .toInstance(p);
        
        bind(GuiResponseCompiler.class);

    }

    @Provides
    Portal providePortal(MartRegistryFactory factory) {
        return new Portal(factory);
    }
    
    @Provides
    Query provideQuery(MartRegistryFactory factory) {
        return new Query(new Portal(factory));
    }
    
}
