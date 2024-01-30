# 概述

本文主要介绍普通spring项目(非springboot)怎么进行配置加密。

出于安全考虑，生产配置不能明文出现在配置文件中。对于SpringBoot可以使用jasypt-spring-boot这个组件来为配置属性提供加密。

普通的spring项目暂时就没有找到合适的加密工具。这时候那就只能自己造轮子了。

在造轮子前，可以看下jasypt-spring-boot是怎么实现的。

## 1.Spring boot 配置加密

spring boot配置加密比较简单，使用jasypt-spring-boot可以非常简单就能实现，具体参考 https://github.com/ulisesbocchio/jasypt-spring-boot 。这方面资料很多就不多赘述了。接下来我们主要了解下jasypt-spring-boot是怎么实现配置加密的，方便我们对普通的spring项目进行使用。如果只是使用可以直接看  2.普通spring 项目配置加密

### 1.1 spring.factories 配置

了解了spring boot都知道，springboot有个相当好用的特性，叫做自动装配。就是springboot会自动加载引入的starter库，无需像传统的spring项目那些加大量的配置和引入代码。这个机制是会扫描引入库META-INF中的spring.factories文件，加载配置的的class name进行实例化。所以我们先找jasypt-spring-boot-starter的spring.factories文件。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/6c3c75b3802848afa7a0f1eedaedea27.png#pic_center)
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.ulisesbocchio.jasyptspringboot.JasyptSpringBootAutoConfiguration

org.springframework.cloud.bootstrap.BootstrapConfiguration=com.ulisesbocchio.jasyptspringboot.JasyptSpringCloudBootstrapConfiguration
```

由于我们使用的是spring boot所以 加载的类为 com.ulisesbocchio.jasyptspringboot.JasyptSpringBootAutoConfiguration。

### 1.2 JasyptSpringBootAutoConfiguration的加载

```java
@Configuration
@Import(EnableEncryptablePropertiesConfiguration.class)
public class JasyptSpringBootAutoConfiguration {
}
```

JasyptSpringBootAutoConfiguration 没做啥，主要就 Import了 EnableEncryptablePropertiesConfiguration类，我们继续看 EnableEncryptablePropertiesConfiguration。

```java
@Configuration
@Import({EncryptablePropertyResolverConfiguration.class, CachingConfiguration.class})
@Slf4j
public class EnableEncryptablePropertiesConfiguration {

    @Bean
    public static EnableEncryptablePropertiesBeanFactoryPostProcessor enableEncryptablePropertySourcesPostProcessor(final ConfigurableEnvironment environment, EncryptablePropertySourceConverter converter) {
        return new EnableEncryptablePropertiesBeanFactoryPostProcessor(environment, converter);
    }
}
```

EnableEncryptablePropertiesConfiguration 做了3件事

1. imoirt EncryptablePropertyResolverConfiguration
2. imprt CachingConfiguration
3. 创建了一个 EnableEncryptablePropertiesBeanFactoryPostProcessor bean对象

下面分别进行说明下：

EncryptablePropertyResolverConfiguration类负责创建各种加密解密需要用到的bean对象。

CachingConfiguration 用于刷新 properties信息，主要在cloud 场景下使用，如nacos配置刷新。通过实现ApplicationListener 监听配置变化。

EnableEncryptablePropertiesBeanFactoryPostProcessor 实现BeanFactoryPostProcessor，Ordered。BeanFactoryPostProcessor的主要作用是在Spring容器实例化bean之前，对bean的定义进行修改。这意味着我们可以在bean实例化之前，对bean的定义进行一些自定义的修改。

接下来我们重点关注 EnableEncryptablePropertiesBeanFactoryPostProcessor#postProcessBeanFactory。

### 1.3 postProcessBeanFactory 修改PropertySource 

```java
@Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        LOG.info("Post-processing PropertySource instances");
        //获取PropertySources
        MutablePropertySources propSources = environment.getPropertySources();
        //转换PropertySources
        converter.convertPropertySources(propSources);
    }
```

继续进入 converter.convertPropertySources(propSources)，这个方法将propSources 进行了steam操作，这个sream操作主要做了俩件事，将PropertySource 进行了转换，然后将转换后的PropertySource 替换原有的PropertySource；就是将原有的属性配置信息进行了处理，替换了原有的PropertySource 对象。

```java
public void convertPropertySources(MutablePropertySources propSources) {
        StreamSupport.stream(propSources.spliterator(), false)
                .filter(ps -> !(ps instanceof EncryptablePropertySource))
                //进行转换
                .map(this::makeEncryptable)
                .collect(toList())
            	//替换原有的资源对象
                .forEach(ps -> propSources.replace(ps.getName(), ps));
    }
```

继续看 makeEncryptable，返回一个新的PropertySource。

```java
public <T> PropertySource<T> makeEncryptable(PropertySource<T> propertySource) {
        if (propertySource instanceof EncryptablePropertySource || skipPropertySourceClasses.stream().anyMatch(skipClass -> skipClass.equals(propertySource.getClass()))) {
            log.info("Skipping PropertySource {} [{}", propertySource.getName(), propertySource.getClass());
            return propertySource;
        }
    	// 转换一个新的 PropertySource
        PropertySource<T> encryptablePropertySource = convertPropertySource(propertySource);
        log.info("Converting PropertySource {} [{}] to {}", propertySource.getName(), propertySource.getClass().getName(),
                AopUtils.isAopProxy(encryptablePropertySource) ? "AOP Proxy" : encryptablePropertySource.getClass().getSimpleName());
        return encryptablePropertySource;
    }
```

具有实现看convertPropertySource 方法

```java
private <T> PropertySource<T> convertPropertySource(PropertySource<T> propertySource) {
        return interceptionMode == InterceptionMode.PROXY
                ? proxyPropertySource(propertySource) : instantiatePropertySource(propertySource);
    }
```

默认使用WRAPPER 模式，我们主要看instantiatePropertySource

```java
private <T> PropertySource<T> instantiatePropertySource(PropertySource<T> propertySource) {
        PropertySource<T> encryptablePropertySource;
        if (needsProxyAnyway(propertySource)) {
            encryptablePropertySource = proxyPropertySource(propertySource);
        } else if (propertySource instanceof  SystemEnvironmentPropertySource) {
            encryptablePropertySource = (PropertySource<T>) new EncryptableSystemEnvironmentPropertySourceWrapper((SystemEnvironmentPropertySource) propertySource, propertyResolver, propertyFilter);
        } else if (propertySource instanceof MapPropertySource) {
            encryptablePropertySource = (PropertySource<T>) new EncryptableMapPropertySourceWrapper((MapPropertySource) propertySource, propertyResolver, propertyFilter);
        } else if (propertySource instanceof EnumerablePropertySource) {
            encryptablePropertySource = new EncryptableEnumerablePropertySourceWrapper<>((EnumerablePropertySource) propertySource, propertyResolver, propertyFilter);
        } else {
            encryptablePropertySource = new EncryptablePropertySourceWrapper<>(propertySource, propertyResolver, propertyFilter);
        }
        return encryptablePropertySource;
    }
```

这里将propertySource包装为各种PropertySource wrapper.通过包装后的PropertySource 就能够进行解密了。

### 1.4 配置解密

spring框架读取配置信息时是通过PropertySource 的getProperty获取。

我们进入其中一个EncryptablePropertySourceWrapper 查看

```java
@Override
    public Object getProperty(String name) {
        return encryptableDelegate.getProperty(name);
    }
```

继续进入EncryptablePropertySource#getProperty，

进行了两个动作 1.获取配置内容2.进行解密

```java
default Object getProperty(EncryptablePropertyResolver resolver, EncryptablePropertyFilter filter, PropertySource<T> source, String name) {
    	// 获取配置内容
        Object value = source.getProperty(name);
        if (filter.shouldInclude(source, name) && value instanceof String) {
            String stringValue = String.valueOf(value);
            // 这里就是进行解密
            return resolver.resolvePropertyValue(stringValue);
        }
        return value;
    }
```

resolvePropertyValue 就是的解密方法。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/bd7f51de9704494bb20f2727d289f748.png#pic_center)
它有两个默认实现，最后一个是我自定义实现的解密。

>  这个方法就是判断是否是已经加密的value，如果是，则进行解密。如果不是，那就返回原值。

```java
 @Override
    public String resolvePropertyValue(String value) {
        return Optional.ofNullable(value)
                .filter(detector::isEncrypted)		// 如果经过属性探测器确认的，才继续
                .map(resolvedValue -> {
                    try {
                        String unwrappedProperty = detector.unwrapEncryptedValue(resolvedValue.trim());	// 过滤加密规则后的字符串
                        return encryptor.decrypt(unwrappedProperty);	// 解密
                    } catch (EncryptionOperationNotPossibleException e) {
                        throw new DecryptionException("Unable to decrypt: " + value + ". Decryption of Properties failed,  make sure encryption/decryption " +
                                "passwords match", e);
                    }
                })
                .orElse(value);
    }
```

### 1.5流程说明

通过上面我们发现 jasypt-spring-boot 的大致实现流程如下：

1.自动装配初始化JasyptSpringBootAutoConfiguration，生成解密相关的bean

2.通过EnableEncryptablePropertiesBeanFactoryPostProcessor#postProcessBeanFactory  覆盖原有PropertySource(重写了getProperty方法)，使新的PropertySource具有解密能力

3.spring加载配置信息时调用新PropertySource getProperty，由于新PropertySource 重写了getProperty，所以可以进行解密。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/72ca6cb9da1c471eafde61a6d378f4ac.jpeg#pic_center)
解密实际上就是调用应用中的PropertySource#getProperty方法。

## 2.解密详细分析

说明解密前我们得先了解下 @Value 和 配置${}是如何实现的。

### 在spring 3.1以前 

是通过 PropertyPlaceholderConfigurer 实现的。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f3c36a414a6d4352990357b7b2daf9b4.png#pic_center)
上面是以@value注解为例，xml中使用也是同样的逻辑。只是注入的入口不一样。

最后获取配置内容时调用的PropertyPlaceholderConfigurer.PropertyPlaceholderConfigurerResolver#resolvePlaceholder 方法

```java
private class PropertyPlaceholderConfigurerResolver implements PlaceholderResolver {
        private final Properties props;

        private PropertyPlaceholderConfigurerResolver(Properties props) {
            this.props = props;
        }

        public String resolvePlaceholder(String placeholderName) {
            return PropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName, this.props, PropertyPlaceholderConfigurer.this.systemPropertiesMode);
        }
    }
```

实际最后取得是props 中得值。那这个props是什么时候写入的呢？

答案是：PropertyPlaceholderConfigurer中得PropertyPlaceholderConfigurerResolver对象是在PropertyPlaceholderConfigurer#processProperties方法进行初始化的。这点很关键，后面我们方案1就是利用到这个机制。

### 在spring 3.1及以后

是通过 PropertySourcesPlaceholderConfigurer 实现的。为什么这么说呢？大家看这个栈信息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/0d9ea3d1c54443f2862589e29bdab4f5.png#pic_center)
可以看到获取值最终是在PropertySourcesPropertyResolver的this.propertySources中获取，而propertySources来自哪里呢？

答案是：PropertySourcesPlaceholderConfigurer中得postProcessBeanFactory方法中通过

```java
processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
```

传入的。我们执行查看下postProcessBeanFactory方法

```java
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.propertySources == null) {
			this.propertySources = new MutablePropertySources();
			if (this.environment != null) {
                // 1.加载原有 Environment 属性信息
				this.propertySources.addLast(
					new PropertySource<Environment>(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, this.environment) {
						@Override
						@Nullable
						public String getProperty(String key) {
							return this.source.getProperty(key);
						}
					}
				);
			}
			try {
                // 2.通过 mergeProperties 方法载入 resources目录内配置信息
				PropertySource<?> localPropertySource =
						new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, mergeProperties());
				if (this.localOverride) {
					this.propertySources.addFirst(localPropertySource);
				}
				else {
					this.propertySources.addLast(localPropertySource);
				}
			}
			catch (IOException ex) {
				throw new BeanInitializationException("Could not load properties", ex);
			}
		}
		//3.创建 PropertySourcesPropertyResolver 传入propertySources
		processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
		this.appliedPropertySources = this.propertySources;
	}
```

processProperties这个方法需要我们重点关注下，它有两个参数beanFactory 是一个ConfigurableListableBeanFactory（实现类DefaultListableBeanFactory）（bean工厂，在这个场景中负责初始化bean实例）对象，PropertySourcesPropertyResolver 是获取properties属性的类。

看到beanFactory 这里其实 我们可以知道processProperties 方法是将PropertySourcesPropertyResolver 提供给beanFactory用来在bean初始化时给bean对象的属性进行赋值。

最终会调用 PlaceholderConfigurerSupport 的 doProcessProperties方法。

```java
protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
			StringValueResolver valueResolver) {

		BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);

		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		for (String curName : beanNames) {
			// Check that we're not parsing our own bean definition,
			// to avoid failing on unresolvable placeholders in properties file locations.
			if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
				BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
				try {
					visitor.visitBeanDefinition(bd);
				}
				catch (Exception ex) {
					throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage(), ex);
				}
			}
		}
		// New in Spring 2.5: resolve placeholders in alias target names and aliases as well.
		beanFactoryToProcess.resolveAliases(valueResolver);
		// New in Spring 3.0: resolve placeholders in embedded values such as annotation attributes.
		beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
	}
```

PlaceholderConfigurerSupport  这个其实PropertyPlaceholderConfigurer也有继承。也就是说PropertyPlaceholderConfigurer 和 PropertySourcesPlaceholderConfigurer  都是基于 PlaceholderConfigurerSupport 实现的。



最后我们简单总结一下。

调用过程大致有几个关键节点点如下：
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/046873203ca345f589d4375548d414e7.png#pic_center)

1. DefaultListableBeanFactory 开始进行初始化bean

2. AutowiredAnnotationBeanPostProcessor 寻找@value注解，准备加载属性值

3. DefaultListableBeanFactory 进行属性值加载
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f66a17bc452d4d0a9e4980eb1268df59.png#pic_center)

   可以看到他是通过Resolver来获取值的

4. PropertySourcesPropertyResolver 最终加载propertySources 的属性值

其中 PropertySourcesPropertyResolver #getProperty获取的propertySources 信息来自于PropertySourcesPlaceholderConfigurer中的postProcessBeanFactory传入。

而postProcessBeanFactory 有做了三件事

1.加载原有 Environment 属性信息

2.通过 PropertiesLoaderSupport的mergeProperties 方法载入 resources目录内配置信息

3.创建 PropertySourcesPropertyResolver 传入propertySources，通过 PropertiesLoaderSupport的doProcessProperties方法进行加载

看到这里我们不难分析出我们想要获取到解密后的属性信息，我们可以从这几个方向着手。

1.改变 Environment  中的propertySources信息

2.改变mergeProperties 加载的propertes

3.创建自定义的 ConfigurablePropertyResolver，目的就是改变对象里面的 props

下面我们根据这几个方向分别进行实现。

## 3.普通spring 项目配置加密

spring 配置加密有几种方案

### 方案1. 通过 environment 实现

官方建议的方式，通过我们对jasypt-spring-boot分析， 这个其实就是jasypt-spring-boot的实现方式。我们简单仿照来一波就可以了。

通过实现BeanFactoryPostProcessor我们实现 postProcessBeanFactory实现改变 Environment  中的propertySources信息。

```java
public class EnableEncryptablePropertiesBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

    private final ConfigurableEnvironment environment;

    public EnableEncryptablePropertiesBeanFactoryPostProcessor(StandardEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        MutablePropertySources propSources = this.environment.getPropertySources();
        StreamSupport.stream(propSources.spliterator(), false)
                .filter(ps -> !(ps instanceof EncryptablePropertySource))
                .map(this::makeEncryptable)
                .collect(toList())
                .forEach(ps -> propSources.replace(ps.getName(), ps));
    }

    @Override
    public int getOrder() {
        return 2147483547;
    }
	/**
     * 包装自定义的PropertySource指定解析器
     * @param propertySource
     * @param <T>
     * @return
     */
    public <T> PropertySource<T> makeEncryptable(PropertySource<T> propertySource) {
        if (propertySource instanceof EncryptablePropertySource ) {
            return propertySource;
        }
        PropertySource<T> encryptablePropertySource = new EncryptablePropertySourceWrapper(propertySource,new EncryptablePropertyResolver());
        return encryptablePropertySource;
    }
}

```

具体代码可以查看 

源码：https://github.com/AndsGo/spring-conf-encryption/tree/main/spring5

### 方案2.改变PropertySourcesPlaceholderConfigurer 加载的properties

我们知道spring  <context:property-placeholder/> 标签是用来加载配置文件的。

我们可以查看 property-placeholder xsd定义 发现：
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d8281c8ab2d249b48165b801d2cd6421.png#pic_center)

他本质上是加载 PropertySourcesPlaceholderConfigurer（spring 3.1 之后，3.1之前是加载 PropertyPlaceholderConfigurer ），

它有那些属性可以配置内，我们继续看propertyLoading。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/c4096a4e9e3a4078af05b2bcee02a378.png#pic_center)
property-placeholder 可以配置如下属性：

（1）location：表示属性文件位置，多个之间通过如逗号/分号等分隔； 
（2）file-encoding：文件编码； 
（3）ignore-resource-not-found：如果属性文件找不到，是否忽略，默认false，即不忽略，找不到将抛出异常 
（4）ignore-unresolvable：是否忽略解析不到的属性，如果不忽略，找不到将抛出异常 
（5）properties-ref：本地java.util.Properties配置 
（6）local-override：是否本地覆盖模式，即如果true，那么properties-ref的属性将覆盖location加载的属性 
（7）system-properties-mode：系统属性模式，ENVIRONMENT（默认），NEVER，OVERRIDE 
（8）ENVIRONMENT：将使用Spring 3.1提供的PropertySourcesPlaceholderConfigurer，其他情况使用Spring 3.1之前的PropertyPlaceholderConfigurer 
（9）OVERRIDE:PropertyPlaceholderConfigurer使用，因为在spring 3.1之前版本是没有Enviroment的，所以OVERRIDE是spring 3.1之前版本的Environment 
（10）NEVER：只查找properties-ref、location； 
（11）order：当配置多个`<context:property-placeholder/>`时的查找顺序

我们在这里重点关注properties-ref，这里可以配置我们自定义的 Properties，我们可以在这里面进行解密操作。

自定义：DataSourceProperties

```java
public class DataSourceProperties extends Properties {
    private String location;
    /**
     * 构造方法
     *
     * @param location 需要解密的属性名称
     */
    public DataSourceProperties(String location) throws IOException {
        String[] split = location.split(",");
        for (String loc : split) {
            Properties properties = new Properties();
            properties.load(DataSourceProperties.class.getClassLoader()
                    .getResourceAsStream(loc));
            Enumeration<?> propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String key = propertyNames.nextElement().toString();
                String value = properties.getProperty(key);
                this.setProperty(key, EncryptablePropertyResolver.resolvePropertyValue(value));
            }
        }
    }
}

```

配置applicationContext.xml

```xml
	<bean id="dataSourceProperties" class="com.DataSourceProperties">
		<constructor-arg name="location" value="constant.properties,constant1.properties"/>
	</bean>
	<context:property-placeholder properties-ref="dataSourceProperties"/>
```

我们将自定义的dataSourceProperties 初始化给到 property-placeholder。PropertySourcesPlaceholderConfigurer 会在 mergeProperties 方法中将我们自定义的 Properties 加载进入 PropertySources 中。

https://github.com/AndsGo/spring-conf-encryption/tree/main/spring5-1

### 方案3.重写PropertyPlaceholderConfigurer

PropertyPlaceholderConfigurer 继承自**PlaceholderConfigurerSupport**,它可以用于解析 bean 定义属性值中的占位符。实现*将*值从属性文件或其他[属性源](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/env/PropertySource.html)提取到 bean 定义中。

因此我们可以通过重写PropertyPlaceholderConfigurer，来进行的配置的解密。

特别注意 **super.processProperties(beanFactory, props);**它就是注册ConfigurablePropertyResolver，我们重写覆盖了props的属性值，存入了已经解密的属性值。

```java
public class EncryptPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
    private String prefix = "ABC(";
    private String suffix = ")";
    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory,
                                     Properties props) throws BeansException {
        try {
            // 实际使用中我们会将密钥放入环境变量中
			//String encryption_key = System.getenv("XXX_KEY");
            String encryption_key = "12345678";
            for (Object key : props.keySet()) {
                if(isEncrypted(props.getProperty(key.toString()))){
                    //配置解密
                    props.setProperty(key.toString(), EncryptUtil.decrypt(unwrapEncryptedValue(props.getProperty(key.toString())),encryption_key));
                }
            }
            //注册 ConfigurablePropertyResolver
            super.processProperties(beanFactory, props);
        } catch (Exception e) {
            throw new BeanInitializationException(e.getMessage());
        }
    }
    private boolean isEncrypted(String property) {
        if (property == null) {
            return false;
        }
        final String trimmedValue = property.trim();
        return (trimmedValue.startsWith(prefix) &&
                trimmedValue.endsWith(suffix));
    }
    private String unwrapEncryptedValue(String property) {
        return property.substring(
                prefix.length(),
                (property.length() - suffix.length()));
    }
}
```

重写了后我们需要在spring配置文件中将其初始化，覆盖默认的 bean,propertyPlaceholderConfigurer

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:context="http://www.springframework.org/schema/context"
	   xmlns:aop="http://www.springframework.org/schema/aop"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans
                        http://www.springframework.org/schema/beans/spring-beans-4.1.xsd
						http://www.springframework.org/schema/context
						http://www.springframework.org/schema/context/spring-context.xsd">
	<bean id="propertyPlaceholderConfigurer" class="com.EncryptPropertyPlaceholderConfigurer">
		<property name="locations">
			<list>
				<value>classpath:constant.properties</value>
			</list>
		</property>
	</bean>
	<!-- 扫描对应包下所有使用注解的类型 -->
	<context:component-scan base-package="com"/>
</beans>
```

这样就是可以实现解密了。

需要注意的是PropertyPlaceholderConfigurer 在sppring 5.2后面过时，官方不建议使用。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f2fe29e39cc848768aa614c069cf6080.png#pic_center)

源码： https://github.com/AndsGo/spring-conf-encryption/tree/main/spring3

最后我们总结下这三种方式：

1.通过 environment 实现 这种方式代码复杂，但是相当灵活，可以实现多种业场景jasypt-spring-boot就是用这种方式，也是官方推荐的方式。

2.改变mergeProperties 加载的propertes 和 3.创建自定义的 ConfigurablePropertyResolver，目的就是改变对象里面的 props

这两种反式代码比较简单，但是可操作性就很弱了，基本只能进行Properties值得调整。但是简单，业务简单场景用这两种反式还是比较方便。
