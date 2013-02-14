package org.doc4web.grails.jsf.facelets;

import grails.util.Environment;
import grails.util.Metadata;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.ServletContextResourceLoader;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ResourceResolver;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * org.doc4web.grails.jsf.facelets
 * <p/>
 * Date: 14 mars 2010
 * Time: 14:48:47
 *
 * @author pred
 */
public class GrailsResourceResolver extends ResourceResolver {

	private GroovyPageResourceLoader resourceLoader;
	private ServletContextResourceLoader servletContextLoader;


	public GrailsResourceResolver()
	{
		System.out.println("GrailsResourceResolver: constructor");
		// Bugfix: ResourceResolver only during Development, SB, 2011-11-24
		if (ApplicationHolder.getApplication().getMainContext().containsBean(GroovyPageResourceLoader.BEAN_ID))
		{
			System.out.println("GrailsResourceResolver: get groovyPageResourceLoader");

			this.resourceLoader = (GroovyPageResourceLoader) ApplicationHolder.getApplication().getMainContext().getBean(GroovyPageResourceLoader.BEAN_ID);

		}
		else if (ApplicationHolder.getApplication().getMainContext().containsBean("groovyPageResourceLoaderJSF"))
		{
			System.out.println("GrailsResourceResolver: get groovyPageResourceLoaderJSF");

			this.resourceLoader = (GroovyPageResourceLoader) ApplicationHolder.getApplication().getMainContext().getBean("groovyPageResourceLoaderJSF");
		}
	}

	public URL resolveUrl(String s) {
		if (servletContextLoader == null) {
			ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
			this.servletContextLoader = new ServletContextResourceLoader(sc);
		}
		try {
			return this.getResourceForUri(s).getURL();
            /*return new URL("file", "",
                    "PATH_TO_FACELETS_FILES_GOES_HERE" + s);*/
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			return null;
		}
	}

	private Resource getResourceForUri(String uri) {
		Resource r;
		r = getResourceWithinContext(uri);
		if (r == null || !r.exists()) {
			// try plugin
			String pluginUri = uri;
			r = getResourceWithinContext(pluginUri);
			if (r == null || !r.exists()) {
				uri = getUriWithinGrailsViews(uri);
				return getResourceWithinContext(uri);
			}
		}
		return r;
	}

	protected String getUriWithinGrailsViews(String relativeUri) {
		StringBuilder buf = new StringBuilder();
		String[] tokens;
		if (relativeUri.startsWith("/"))
			relativeUri = relativeUri.substring(1);


		if (relativeUri.indexOf('/') > -1)
			tokens = relativeUri.split("/");
		else
			tokens = new String[]{relativeUri};


		buf.append("/grails-app/views");
		for (String token : tokens) {
			buf.append('/').append(token);
		}

		return buf.toString();
	}

	/**
	 * Bugfix: Get Resource through resourceLoader OR servletContextLoader
	 *
	 * @param uri
	 * @return
	 */
	private Resource getResourceWithinContext(String uri)
	{
		System.out.println("GrailsResourceResolver: getResourceWithinContext: " + uri);

		if (resourceLoader != null) {
			System.out.println("GrailsResourceResolver: GroovyPageResourceLoader is active");

			if (Environment.getCurrent().isReloadEnabled() && Metadata.getCurrent().isWarDeployed()) {
				return resourceLoader.getResource(uri);
			}

			Resource r = servletContextLoader.getResource(uri);
			if (r.exists()) {
				return r;
			}

			return resourceLoader.getResource(uri);
		}
		else {
			System.out.println("GrailsResourceResolver: trying to get resource through context");
			Resource r = servletContextLoader.getResource(uri);

			if (r.exists()) {
				return r;
			}
		}

		System.out.println("GrailsResourceResolver: trying Spring MainContext");
		Resource r = ApplicationHolder.getApplication().getMainContext().getResource(uri);
		if (r.exists()) {
			return r;
		}

		System.out.println("GrailsResourceResolver: trying fixed base path with Spring MainContext");
		r = ApplicationHolder.getApplication().getMainContext().getResource("/WEB-INF/grails-app/views" + uri);
		if (r.exists()) {
			return r;
		}

		System.out.println("GrailsResourceResolver: couldn't get resource");

		throw new IllegalStateException("ResourceResolver not initialised correctly, no [resourceLoader] specified!");
	}
}
