import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.example.model.BeanDefinitionReader;
import org.example.model.BeanDefinitionsWrapper;
import org.example.config.JsonBeanDefinitionReader;
import org.junit.Test;

public class JsonBeanDefinitionReaderReaderTest {

    @Test
    public void testReadBeanDefinitions() throws Exception {
        BeanDefinitionsWrapper beanDefinitions = new JsonBeanDefinitionReader().readBeanDefinitions("beans.json");

        var allBeans = beanDefinitions.getBeans();
        assertNotNull("Bean definitions should not be null", beanDefinitions);
        assertEquals("Incorrect number of bean definitions", 2, allBeans.size());

        // Проверка первого бина
        BeanDefinitionReader firstBean = allBeans.get(0);
        assertEquals("com.example.services.MyService", firstBean.getClassName());
        assertEquals("myService", firstBean.getName());
        assertEquals("singleton", firstBean.getScope());
        assertEquals("value1", firstBean.getInitParams().get("setSomeProperty"));
        assertEquals(5, firstBean.getInitParams().get("setAnotherProperty"));

        // Проверка второго бина
        BeanDefinitionReader secondBean = allBeans.get(1);
        assertEquals("com.example.repository.MyRepository", secondBean.getClassName());
        assertEquals("myRepositoryImpl", secondBean.getName());
        assertEquals("prototype", secondBean.getScope());
        assertEquals("myDataSource", secondBean.getInitParams().get("setDataSource"));
    }
}