package org.biomart.web;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import java.lang.annotation.Annotation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import org.biomart.api.rest.annotations.Cache;

/**
 *
 * @author jhsu
 */
public class CacheControlFilter implements ContainerResponseFilter {
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        Annotation[] annotations = response.getAnnotations();
        for (Annotation atn : annotations) {
            if (atn instanceof Cache) {
                Cache cache = (Cache)atn;
                CacheControl cc = new CacheControl();
                cc.setMaxAge(cache.maxAge());
                cc.setMustRevalidate(cache.mustRevalidate());
                cc.setNoCache(cache.noCache());
                cc.setNoStore(cache.noStore());
                cc.setNoTransform(cache.noTransform());
                cc.setProxyRevalidate(cache.proxyRevalidate());
                response.getHttpHeaders().add(HttpHeaders.CACHE_CONTROL, cc.toString());
                break;
            }
        }
        return response;
    }
}
