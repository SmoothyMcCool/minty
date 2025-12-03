package tom.config;

import org.springframework.lang.NonNull;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import tom.config.security.SecurityConfig;

public class ApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return null;
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class[] { SecurityConfig.class, ApplicationConfig.class };
	}

	@Override
	protected @NonNull String[] getServletMappings() {
		return new String[] { "/" };
	}

	@Override
	protected void customizeRegistration(@NonNull ServletRegistration.Dynamic registration) {
		registration.setMultipartConfig(new MultipartConfigElement(""));
	}

	@Override
	protected Filter[] getServletFilters() {
		return new Filter[] { new SpaRedirectFilter() };
	}

	@Override
	public void onStartup(@SuppressWarnings("null") ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		// e.g. servletContext.addListener(new SomeListener());
	}
}
