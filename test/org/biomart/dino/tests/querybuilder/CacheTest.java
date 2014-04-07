package org.biomart.dino.tests.querybuilder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;

import org.biomart.dino.cache.Cache;
import org.biomart.dino.querybuilder.QueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CacheTest {
    Cache c;
    QueryBuilder qb;
    
    final String data = "key_column\th1\th2\nmy_gene_id\td1\td2", h = "h1\th2",
            cachedData = "d1\td2";
    
    @SuppressWarnings("rawtypes")
    @Before
    public void setUp() {
        qb = mock(QueryBuilder.class);
        
        ArgumentCaptor<OutputStream> argument = ArgumentCaptor.forClass(OutputStream.class);
        
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                OutputStream o = (OutputStream) args[0];
                o.write(data.getBytes());
//                Mock mock = (Mock) invocation.getMock();
//                return mock;
                return null;
            }
            
        }).when(qb).getResults(argument.capture());
    }
    
    @Test
    public void defaultDelims() throws IOException {
        c = new Cache(qb);
        assertEquals("\t", c.getColDelim());
        assertEquals("\n", c.getLineDelim());
    }
    
    @Test
    public void getHeaderTest() throws IOException {
        c = new Cache(qb);
        assertEquals(h, c.getHeader());
    }
    
    @Test
    public void getTest() throws IOException {
        c = new Cache(qb);
        assertEquals(cachedData, c.get("my_gene_id"));
    }
}
