import com.Model;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * @author songxulin
 * @date 2024/1/27
 */
public class App {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");


        Model bean = context.getBean(Model.class);
        String test = bean.getTest();
        System.out.println(test);
        ConfigurableEnvironment environment = context.getBean(ConfigurableEnvironment.class);
        MutablePropertySources propertySources = environment.getPropertySources();
        for (PropertySource<?> source : propertySources) {
            System.out.println(source.getProperty("redis.host}"));
        }
        //        加密
//        resolvePropertyValue encryptor = new resolvePropertyValue();
//        String decrypt = encryptor.decrypt("123456");
//        System.out.println(decrypt);
    }
//
}
