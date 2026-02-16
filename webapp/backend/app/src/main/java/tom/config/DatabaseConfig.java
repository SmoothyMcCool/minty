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

	private final MintyConfiguration props;

	public DatabaseConfig(MintyConfiguration properties) {
		props = properties;
	}

	@Bean(destroyMethod = "close")
	@SpringSessionDataSource
	DataSource applicationDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(props.getConfig().db().url());
		config.setUsername(props.getConfig().db().user());
		config.setPassword(props.getConfig().db().password());
		config.setDriverClassName("org.mariadb.jdbc.Driver");
		config.addDataSourceProperty("maxAllowedPacket", props.getConfig().db().maxPacketSize());
		config.addDataSourceProperty("compress", props.getConfig().db().useCompression());
		config.setMaximumPoolSize(20);

		return new HikariDataSource(config);
	}

	@Bean
	LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource applicationDataSource) {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(applicationDataSource);
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
	PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
		return transactionManager;
	}

}
