import com.Model;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
    }
}
