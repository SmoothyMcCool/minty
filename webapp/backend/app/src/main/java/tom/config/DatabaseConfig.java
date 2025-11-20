package tom.config;

import java.util.Properties;

import javax.sql.DataSource;

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

	private final ExternalProperties props;

	public DatabaseConfig(ExternalProperties properties) {
		props = properties;
	}

	@Bean
	@SpringSessionDataSource
	public DataSource applicationDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(props.get("applicationDbUrl"));
		config.setUsername(props.get("applicationDbUser"));
		config.setPassword(props.get("applicationDbPassword"));
		config.setDriverClassName("org.mariadb.jdbc.Driver");
		config.addDataSourceProperty("maxAllowedPacket", props.get("applicationDbMaxAllowedPacket"));
		config.addDataSourceProperty("compress", props.get("applicationDbCompress"));
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
