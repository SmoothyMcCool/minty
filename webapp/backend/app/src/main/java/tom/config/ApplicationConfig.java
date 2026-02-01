package tom.config;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.filter.MarkdownFilter;
import de.neuland.pug4j.template.FileTemplateLoader;
import tom.cache.service.SingleFlightCacheManager;

@Configuration
@EnableWebMvc
@EnableJdbcHttpSession
@EnableTransactionManagement
@EnableScheduling
public class ApplicationConfig implements WebMvcConfigurer {

	private static final Logger logger = LogManager.getLogger(ApplicationConfig.class);

	MintyConfigurationImpl properties;

	public ApplicationConfig(MintyConfigurationImpl properties) {
		this.properties = properties;
	}

	@Bean
	static MintyConfiguration properties() throws StreamReadException, DatabindException, IOException {
		return new MintyConfigurationImpl();
	}

	@Bean
	public HttpSessionIdResolver httpSessionIdResolver() {
		return HeaderHttpSessionIdResolver.xAuthToken();
	}

	@Bean
	public CacheManager cacheManager() {
		return new SingleFlightCacheManager();
	}

	@Bean("streamingExecutor")
	public ThreadPoolTaskExecutor streamingExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.getConfig().threads().streamDefault());
		executor.setMaxPoolSize(properties.getConfig().threads().streamMax());
		executor.setQueueCapacity(properties.getConfig().threads().streamCapacity());
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setThreadNamePrefix("streaming-");
		executor.initialize();
		return executor;
	}

	@Bean
	@Qualifier("taskExecutor")
	ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(properties.getConfig().threads().taskDefault());
		taskExecutor.setMaxPoolSize(properties.getConfig().threads().taskMax());
		return taskExecutor;
	}

	@Bean
	@Qualifier("fileProcessingExecutor")
	ThreadPoolTaskExecutor fileProcessingExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(properties.getConfig().threads().documentDefault());
		taskExecutor.setMaxPoolSize(properties.getConfig().threads().documentMax());
		return taskExecutor;
	}

	@Bean
	@Qualifier("llmExecutor")
	ThreadPoolTaskExecutor llmExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(properties.getConfig().threads().llmDefault());
		taskExecutor.setMaxPoolSize(properties.getConfig().threads().llmMax());
		taskExecutor.setQueueCapacity(properties.getConfig().ollama().maxRequests());
		return taskExecutor;
	}

	@Bean
	@Qualifier("simpleExecutor")
	SimpleAsyncTaskExecutor simpleExecutor() {
		SimpleAsyncTaskExecutor simpleExecutor = new SimpleAsyncTaskExecutor();
		return simpleExecutor;
	}

	// For use by @Scheduled
	@Bean
	TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("spring-scheduler-");
		scheduler.initialize();
		return scheduler;
	}

	@Bean
	MultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}

	@Bean
	FileTemplateLoader pugFileLoader() {
		FileTemplateLoader templateLoader = new FileTemplateLoader();
		templateLoader.setBase("");
		return templateLoader;
	}

	@Bean
	PugConfiguration pugConfiguration(FileTemplateLoader pugFileLoader) {
		PugConfiguration config = new PugConfiguration();
		config.setCaching(true);
		config.setTemplateLoader(pugFileLoader);
		config.setFilter("markdown", new MarkdownFilter());
		return config;
	}

	@Bean
	JdbcTemplate vectorJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	OllamaApi ollamaApi() {
		URI ollamaUri = properties.getConfig().ollama().uri();
		logger.info("ollama URI is " + ollamaUri);
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

		Duration ollamaTimeout = properties.getConfig().ollama().apiTimeout();
		factory.setReadTimeout(ollamaTimeout);

		RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(factory);

		return OllamaApi.builder().baseUrl(ollamaUri.toString()).restClientBuilder(restClientBuilder).build();
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

	@Override
	public void configureAsyncSupport(@NonNull AsyncSupportConfigurer configurer) {
		Duration asyncResponseTimeout = properties.getConfig().ollama().asyncResponseTimeout();
		configurer.setDefaultTimeout(asyncResponseTimeout.toMillis());
	}
}
