package tom.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableJpaRepositories("tom")
public class DatabaseConfig {

	// Properties for JPA
	@Value("${applicationDbUrl}")
	private String applicationDbUrl;
	@Value("${applicationDbUser}")
	private String applicationDbUser;
	@Value("${applicationDbPassword}")
	private String applicationDbPassword;

	@Bean
	@SpringSessionDataSource
	public DataSource applicationDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(applicationDbUrl);
		config.setUsername(applicationDbUser);
		config.setPassword(applicationDbPassword);
		config.setDriverClassName("org.mariadb.jdbc.Driver");
		config.setMaximumPoolSize(20);

		return new HikariDataSource(config);
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(applicationDataSource());
		em.setPackagesToScan("tom");
		// em.set.setHibernateProperties(getHibernateProperties());

		JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);

		Properties properties = new Properties();
		properties.setProperty("ssl", "false");
		// properties.setProperty("show_sql", "true");
		em.setJpaProperties(properties);

		return em;
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
		return transactionManager;
	}

}
