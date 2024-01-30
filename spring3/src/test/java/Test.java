import com.EncryptUtil;

/**
 * @author songxulin
 * @date 2024/1/27
 */
public class Test {
    public static void main(String[] args) throws Exception {
        String encrypt = EncryptUtil.encrypt("127.0.0.1", "12345678");//"@#$%^6a7"
        System.out.println(encrypt);
    }
}
