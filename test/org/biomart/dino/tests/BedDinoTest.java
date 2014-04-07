package org.biomart.dino.tests;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.biomart.dino.Binding;
import org.biomart.dino.dinos.BedDino;
import org.biomart.dino.dinos.Dino;
import org.biomart.queryEngine.Query;
import org.biomart.queryEngine.QueryElement;
import org.junit.Before;
import org.junit.Test;

public class BedDinoTest {

    
    BedDino dino;
    Dino dinoMock;
    
    final String b1 = "chrChr-_R\t123\t456", e1 = "Chr-_R:123:456", 
            b2 = "chrX\t1\t2", e2 = "X:1:2",
            b3 = "10\t13\t25", e3 = "10:13:25",
            b4 = "chr3\t123\t456\tname\t34\t-", e4 = "3:123:456:-1",
            b5 = "chr3\t123\t456\tname\t34\t+", e5 = "3:123:456:+1",
            b6 = "chr3\t123\t456\t\t\t+", e6 = "3:123:456:+1",
            b7 = "chr3\t123\t456\t2\t\t+", e7 = "3:123:456:+1",
            b8 = "chr3\t123\t456\t2\t\t+asd\t34-d. h+3", e8 = "3:123:456:+1",
            b9 = "chr1\t20969149\t20978686\tNR_046507\t0\t-\t20978686\t20978686\t0\t3\t462,102,3879,\t0,2928,5658,",
            e9 = "1:20969149:20978686:-1",
            mle = e1+","+e2+","+e3+","+e4+","+e5+","+e6+","+e7,
            mlb = b1+"\n"+b2+"\n"+b3+"\n"+b4+"\n"+b5+"\n"+b6+"\n"+b7;
    
    
    @Before
    public void setUp() throws Exception {
        dino = new BedDino();
        dino.setOutputFilterName("foo");
    }

    @Test
    public void singleLineTest() {
        assertEquals(e1, dino.getEnsemblFormat(b1));
        assertEquals(e2, dino.getEnsemblFormat(b2));
        assertEquals(e3, dino.getEnsemblFormat(b3));
        assertEquals(e4, dino.getEnsemblFormat(b4));
        assertEquals(e5, dino.getEnsemblFormat(b5));
        assertEquals(e6, dino.getEnsemblFormat(b6));
        assertEquals(e7, dino.getEnsemblFormat(b7));
        assertEquals(e8, dino.getEnsemblFormat(b8));
        assertEquals(e9, dino.getEnsemblFormat(b9));
    }
    
    
    @Test
    public void multiLineTest() {
        assertEquals(mle, dino.getEnsemblFormat(mlb));
    }
    
    
    @Test
    public void singleLineFailTest() {
        assertEquals("", dino.getEnsemblFormat("\t123\t456"));
        assertEquals("", dino.getEnsemblFormat("chrChr-_R\t\t456"));
        assertEquals("", dino.getEnsemblFormat("chrChr-_R\t123\t"));
        assertEquals("", dino.getEnsemblFormat("chrChr-_R123\t456"));
        assertEquals("", dino.getEnsemblFormat("chrChr-_R\t123456"));
        assertEquals("", dino.getEnsemblFormat("chrChr-_R123456"));
        assertEquals("", dino.getEnsemblFormat(""));
        assertEquals("", dino.getEnsemblFormat("chr3\t123\t456\t\t34\t"));
        assertEquals("", dino.getEnsemblFormat("chr3\t123\t456\tname\t\t"));
        assertEquals("", dino.getEnsemblFormat("chr3\t123\t456\tname\t34"));
        assertEquals("", dino.getEnsemblFormat("chr3\t123\t456\tname\t34\t"));
    }
    
    
//    @Test
//    public void decoratorTest() throws Exception {
//        String[] mime = new String[0];
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        
//        Query q = mock(Query.class);
//        QueryElement qe = mock(QueryElement.class);
//        Binding b = mock(Binding.class);
//        
//        
//        Map<String, QueryElement> md = new HashMap<String, QueryElement>();
//        md.put("bedfile", qe);
//        
//        when(b.getQueryBindings()).thenReturn(md);
//        when(dinoMock.setMetaData((Binding) any())).thenReturn(dinoMock);
//        when(dinoMock.setMimes(mime)).thenReturn(dinoMock);
//        when(dinoMock.setQuery(q)).thenReturn(dinoMock);
//        
//        Field field = BedDino.class.getDeclaredField("bedFile");
//        field.setAccessible(true);
//        field.set(dino, mlb);
//        
//        dino.setMetaData(b)
//            .setMimes(mime)
//            .setQuery(q);
//        
//        Method method = dino.getClass().getDeclaredMethod("doFormatAndDelegate", OutputStream.class);
//        method.setAccessible(true);
//        method.invoke(dino, out);
//        
//        verify(qe).setFilterValues(mle);
//        verify(dinoMock).setQuery(q);
//        
//    }

}
