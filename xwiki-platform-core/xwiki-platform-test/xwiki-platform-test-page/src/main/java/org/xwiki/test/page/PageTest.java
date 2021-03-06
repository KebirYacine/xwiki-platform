/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.page;

import java.io.InputStream;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.cache.rendering.RenderingCache;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiServletURLFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that wishes to unit test wiki page should extend this class and call {@link #renderPage(DocumentReference)}
 * to load and render a page located in the classpath.
 *
 * @version $Id$
 * @since 7.3M1
 */
@PageComponentList
@ReferenceComponentList
public class PageTest
{
    private static final String XWIKI = "xwiki";

    /**
     * Page tests use the Oldcore rule to configure some base mocks (such as XWiki).
     */
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    /**
     * The document being tested.
     */
    protected XWikiDocument document;

    /**
     * The stubbed request used to simulate a real Servlet Request.
     */
    protected XWikiServletRequestStub request;

    /**
     * The stubbed response used to simulate a real Servlet Response.
     */
    protected XWikiServletResponseStub response;

    /**
     * The mocked XWiki instance, provided for ease of use (can also be retrieved through {@link #oldcore}).
     */
    protected XWiki xwiki;

    /**
     * The configured XWiki Context, provided for ease of use (can also be retrieved through {@link #oldcore}).
     */
    protected XWikiContext context;

    /**
     * The Component Manager to use for getting Component instances or registering Mock Components in the test,
     * provided for ease of use (can also be retrieved through {@link #oldcore}).
     */
    protected MockitoComponentManagerRule mocker = oldcore.getMocker();

    /**
     * Set up components before Components declared in {@link org.xwiki.test.annotation.ComponentList} are handled.
     *
     * @throws Exception in case of errors
     */
    @BeforeComponent
    public void setUpComponentsForPageTest() throws Exception
    {
        mocker.registerMockComponent(JMXBeanRegistration.class);
        mocker.registerMockComponent(Environment.class);
        mocker.registerMockComponent(JobProgressManager.class);
        mocker.registerMockComponent(RenderingCache.class);

        CacheManager cacheManager = mocker.registerMockComponent(CacheManager.class);
        when(cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(mock(Cache.class));
    }

    /**
     * Set up of Components after the Components declared in {@link org.xwiki.test.annotation.ComponentList} have been
     * handled but before {@link MockitoOldcoreRule#before(Class)} has been called (i.e. before it has created Mocks
     * and configured Components).
     *
     * @throws Exception in case of errors
     */
    @AfterComponent
    public void configureComponentsBeforeOldcoreRuleForPageTest() throws Exception
    {
        // Configure the Execution Context
        ExecutionContext ec = new ExecutionContext();
        mocker.<Execution>getInstance(Execution.class).setContext(ec);
    }

    /**
     * @param reference the reference of the Document to test (and thus load from the Classloader)
     * @throws Exception in case of errors
     */
    protected void loadPage(DocumentReference reference) throws Exception
    {
        document = new XWikiDocument(reference);
        // TODO: Add support for NS
        InputStream is = getClass().getClassLoader().getResourceAsStream(
            reference.getLastSpaceReference().getName() + "/" + reference.getName() + ".xml");
        document.fromXML(is);
    }

    /**
     * @param reference the reference of the Document to load and render (and thus load from the Classloader)
     * @return the result of rendering the Document corresponding to the passed reference
     * @throws Exception in case of errors
     */
    protected String renderPage(DocumentReference reference) throws Exception
    {
        loadPage(reference);
        return document.getRenderedContent(context);
    }

    /**
     * Sets the Syntax with which the Document to test will be rendered into. If not called, the Document will be
     * rendered as XHTML.
     *
     * @param syntax the Syntax to render the Document into
     * @throws Exception in case of errors
     */
    protected void setOutputSyntax(Syntax syntax) throws Exception
    {
        MutableRenderingContext renderingContext = mocker.getInstance(RenderingContext.class);
        renderingContext.push(renderingContext.getTransformation(), renderingContext.getXDOM(),
            renderingContext.getDefaultSyntax(), "test", renderingContext.isRestricted(), syntax);
    }

    /**
     * Configures the various Components and their mocks with default values for page tests.
     *
     * @throws Exception in case of errors
     */
    @Before
    public void setUpForPageTest() throws Exception
    {
        // Configure mocks from OldcoreRule
        context = oldcore.getXWikiContext();
        xwiki = oldcore.getMockXWiki();

        // We need this one because some component in its init creates a query...
        when(oldcore.getQueryManager().createQuery(any(String.class), any(String.class))).thenReturn(mock(Query.class));

        // Set up a fake Request
        // Configure request so that $!request.outputSyntax" == 'plain
        // Need to be executed before ecm.initialize() so that XWikiScriptContextInitializer will initialize the
        // script context properly
        request = new XWikiServletRequestStub();
        request.setScheme("http");
        context.setRequest(request);

        response = new XWikiServletResponseStub();
        context.setResponse(response);

        ExecutionContextManager ecm = mocker.getInstance(ExecutionContextManager.class);
        ecm.initialize(oldcore.getExecutionContext());

        // Let the user have view access to all pages
        when(oldcore.getMockRightService().hasAccessLevel(eq("view"), eq("XWiki.XWikiGuest"), anyString(),
            eq(context))).thenReturn(true);

        // Set up URL Factory
        when(xwiki.getWebAppPath(context)).thenReturn(XWIKI);
        context.setURL(new URL("http://localhost:8080/xwiki/bin/Main/WebHome"));
        when(xwiki.showViewAction(context)).thenReturn(true);
        context.setURLFactory(new XWikiServletURLFactory(context));
        when(xwiki.getServletPath(XWIKI, context)).thenReturn("/bin/");

        // All translation return their key as translation values
        LocalizationScriptService lss = mock(LocalizationScriptService.class);
        mocker.registerComponent(ScriptService.class, "localization", lss);
        when(lss.render(anyString())).thenAnswer(
            new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                {
                    // Return the translation key as the value
                    return invocationOnMock.getArgumentAt(0, String.class);
                }
            }
        );
    }

    /**
     * Clean up after the test.
     *
     * @throws Exception in case of errors
     */
    @After
    public void tearDown() throws Exception
    {
        MutableRenderingContext renderingContext = mocker.getInstance(RenderingContext.class);
        renderingContext.pop();
    }
}
