package tom.config;

import java.util.List;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.template.FileTemplateLoader;

@Configuration
@EnableWebMvc
@EnableWebSecurity
@EnableJdbcHttpSession
@PropertySource("classpath:application.properties")
@EnableTransactionManagement
public class ApplicationConfig implements WebMvcConfigurer {

	private static final Logger logger = LogManager.getLogger(ApplicationConfig.class);

	@Value("${ollamaUri}")
	private String ollamaUri;

	@Bean
	public HttpSessionIdResolver httpSessionIdResolver() {
		return HeaderHttpSessionIdResolver.xAuthToken();
	}

	@Bean
	@Qualifier("taskExecutor")
	ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(10);
		return taskExecutor;
	}

	@Bean
	@Qualifier("fileProcessingExecutor")
	ThreadPoolTaskExecutor fileProcessingExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(10);
		return taskExecutor;
	}

	@Bean
	@Qualifier("simpleExecutor")
	SimpleAsyncTaskExecutor simpleExecutor() {
		SimpleAsyncTaskExecutor simpleExecutor = new SimpleAsyncTaskExecutor();
		return simpleExecutor;
	}

	@Bean
	public MultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}

	@Bean
	FileTemplateLoader pugFileLoader() {
		FileTemplateLoader templateLoader = new FileTemplateLoader();
		templateLoader.setBase("");
		return templateLoader;
	}

	@Bean
	public PugConfiguration pugConfiguration(FileTemplateLoader pugFileLoader) {
		PugConfiguration config = new PugConfiguration();
		config.setCaching(true);
		config.setTemplateLoader(pugFileLoader);
		return config;
	}

	@Bean
	JdbcTemplate vectorJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	OllamaApi ollamaApi() {
		logger.info("ollama URI is " + ollamaUri);
		return OllamaApi.builder().baseUrl(ollamaUri).build();
	}

	@Override
	public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		builder.indentOutput(true);// .dateFormat(new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy"));
		// .serializationInclusion(Include.NON_EMPTY);

		// builder.featuresToDisable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

		ObjectMapper mapper = builder.build();
		// mapper.configure(DeserializationFeature.date, state);

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
		// converter.setJsonPrefix(")]}',\n");
		converters.add(converter);

		AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(null, false);
		AnnotationIntrospector secondary = new JacksonXmlAnnotationIntrospector();
		builder.annotationIntrospector(AnnotationIntrospector.pair(introspector, secondary));

		converters.add(new MappingJackson2XmlHttpMessageConverter(builder.createXmlMapper(true).build()));
	}

	@Override
	public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**").addResourceLocations("/static/");
	}

	@Override
	public void addViewControllers(@NonNull ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("forward:/index.html");
	}
}
