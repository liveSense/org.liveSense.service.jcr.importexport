/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.liveSense.service.jcr.importexport.webconsole;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.ImportUUIDBehavior;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.jcr.api.SlingRepository;
import org.liveSense.service.jcr.importexport.JcrtImportExport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class JcrImportExportWebConsolePlugin extends
        HttpServlet {

	
	public enum ImportBehaviour {
		Remove_Existing(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING),
		Replace_Existing(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING),
		Throw(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW),
		Create_New(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

		private int num;
		ImportBehaviour(int num) {
			this.num = num;
		}
	}
	
    private static final long serialVersionUID = 0;

    private static final String ATTR_SUBMIT = "plugin.submit";

    private static final String PAR_WORKSPACE= "repository";
    private static final String PAR_DEFAULT_PATH = "backuppath";
    private static final String PAR_START_PATH = "startpath";
    private static final String PAR_EXPORT_TYPE = "exporttype";
    private static final String PAR_IMPORT_BEHAVIOUR = "importbehaviour";

    private static final String PAR_MSG = "msg";

    private transient ServiceRegistration service;
    SlingRepository repository;
    
    String defaultPath;
    String defaultWorkspace;
    String defaultStartPath;
    
    public JcrImportExportWebConsolePlugin(BundleContext context,
            SlingRepository repository, String defaultPath, String defaultWorkspace, String defaultStartPath) {
        this.repository = repository;
        this.defaultPath = defaultPath;
        this.defaultWorkspace = defaultWorkspace;
        this.defaultStartPath = defaultStartPath;
        
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "JCRResourceResolver Web Console Plugin");
        props.put(Constants.SERVICE_VENDOR, "liveSense.org");
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put("felix.webconsole.label", "jcrimportexport");
        props.put("felix.webconsole.title", "liveSense JCR Import/Export");
        props.put("felix.webconsole.configprinter.modes", "always");

        service = context.registerService(new String[] {
                "javax.servlet.Servlet" },
            this, props);
    }

    public void dispose() {
        if (service != null) {
            service.unregister();
            service = null;
        }
    }

    
    private String getParameterValue(HttpServletRequest req, String key, String defaultValue) {
    	if (req.getParameter(key) == null ||  "".equals(req.getParameter(key))) {
    		return defaultValue;
    	} else {
    		return req.getParameter(key);
    	}
    }
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
    throws ServletException, IOException {

        final String backupPath = getParameterValue(request, PAR_DEFAULT_PATH, defaultPath);
        final String workspace = getParameterValue(request, PAR_WORKSPACE, defaultWorkspace);
        final String startPath = getParameterValue(request, PAR_START_PATH, defaultStartPath);
        final String exportType = getParameterValue(request, PAR_EXPORT_TYPE, "system");
        ImportBehaviour behaviour = ImportBehaviour.Replace_Existing;
        try {
        		behaviour = ImportBehaviour.valueOf(ImportBehaviour.class, getParameterValue(request, PAR_IMPORT_BEHAVIOUR, ImportBehaviour.Replace_Existing.toString()));
        } catch (Exception e) {
		}

        final String msg = getParameterValue(request, PAR_MSG, null);
        
        final PrintWriter pw = response.getWriter();

        
        if (msg != null) {
            pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        	titleHtml(pw,
                    "Message",
                    "<p style='color: red'>" +msg + "</p>");
            pw.println("</table>");

        }
        
        pw.print("<form method='post'>");

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        titleHtml(
            pw,
            "Export Workspace",
            "To export a workspace you have specify Backup path, workspace and the start path in repository." +
            "The two main repository you can use \"default\" and \"security\"" +
            "The generated file name will be <workspacename>-<timestamp>.xml.gz");

        pw.println("<tr class='content'>");
        pw.println("<td class='content'>Default path</td><td class='content' colspan='2'>");
        pw.println("<input type='text' name='" + PAR_DEFAULT_PATH + "' value='" + (backupPath != null ? backupPath : "")
            + "' class='input' size='100'/>");

        pw.println("</td></tr><tr class='content'><td class='content'>Workspace</td><td class='content' colspan='2'>");
        
        pw.println("<input type='text' name='" + PAR_WORKSPACE + "' value='" + (workspace != null ? workspace : "")
                + "' class='input' size='50'/>");

        pw.println("</td></tr><tr class='content'><td class='content'>Export type</td><td class='content' colspan='2'>");

        pw.println("<input type='radio' name='"+PAR_EXPORT_TYPE+"' value='document "+("document".equals(exportType)?" checked" : "") +"'>Document<br>");
        pw.println("<input type='radio' name='"+PAR_EXPORT_TYPE+"' value='system' "+("system".equals(exportType)?" checked" : "") +">System<br>");
        
        pw.println("</td></tr><tr class='content'><td class='content'>Start path</td><td class='content' colspan='2'>");
        
        pw.println("<input type='text' name='" + PAR_START_PATH + "' value='" + (startPath != null ? startPath : "")
                + "' class='input' size='100'/>");
        pw.println("</td></tr>");
        
        separatorHtml(pw);
        
        pw.println("<tr class='content'><td class='content'></td><td class='content' colspan='2'>");
        pw.println("<input type='submit' name='" + ATTR_SUBMIT
            + "' value='Export' class='submit'/>");
        
        pw.print("</td>");
        pw.println("</tr>");
        pw.print("</form>");

        pw.println("</table>");

    
        pw.print("<form method='post'>");

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        titleHtml(
            pw,
            "Import Workspace",
            "To import a workspace you have specify backup file, a workspace import to and the start path in repository.");

        pw.println("<tr class='content'>");
        pw.println("<td class='content'>File name</td><td class='content' colspan='2'>");
        pw.println("<input type='text' name='" + PAR_DEFAULT_PATH + "' value='" + (backupPath != null ? backupPath : "")
            + "' class='input' size='100'/>");

        pw.println("</td></tr><tr class='content'><td class='content'>Workspace</td><td class='content' colspan='2'>");
        
        pw.println("<input type='text' name='" + PAR_WORKSPACE + "' value='" + (workspace != null ? workspace : "")
                + "' class='input' size='50'/>");

        pw.println("</td></tr><tr class='content'><td class='content'>Import collosion behaviour</td><td class='content' colspan='2'>");

        pw.println("<input type='radio' name='"+PAR_IMPORT_BEHAVIOUR+"' value='"+ImportBehaviour.Create_New.name()+"' "+(behaviour.equals(ImportBehaviour.Create_New) ? " checked" : "") +"'>Create new<br>");
        pw.println("<input type='radio' name='"+PAR_IMPORT_BEHAVIOUR+"' value='"+ImportBehaviour.Remove_Existing.name()+"' "+(behaviour.equals(ImportBehaviour.Remove_Existing) ? " checked" : "") +">Remove existing<br>");
        pw.println("<input type='radio' name='"+PAR_IMPORT_BEHAVIOUR+"' value='"+ImportBehaviour.Replace_Existing.name()+"' "+(behaviour.equals(ImportBehaviour.Replace_Existing) ? " checked" : "") +"'>Replace existing<br>");
        pw.println("<input type='radio' name='"+PAR_IMPORT_BEHAVIOUR+"' value='"+ImportBehaviour.Throw.name()+"' "+(behaviour.equals(ImportBehaviour.Throw) ? " checked" : "") +">Throw<br>");

        pw.println("</td></tr><tr class='content'><td class='content'>Start path</td><td class='content' colspan='2'>");
        
        pw.println("<input type='text' name='" + PAR_START_PATH + "' value='" + (startPath != null ? startPath : "")
                + "' class='input' size='100'/>");
        
        pw.println("</td></tr>");
        
        separatorHtml(pw);
        
        pw.println("<tr class='content'><td class='content'></td><td class='content' colspan='2'>");

        pw.println("<input type='submit' name='" + ATTR_SUBMIT
            + "' value='Import' class='submit'/>");
        
        pw.print("</td>");
        pw.println("</tr>");
        pw.print("</form>");


        pw.println("</table>");

    
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        final String backupPath = getParameterValue(request, PAR_DEFAULT_PATH, defaultPath);
        final String workspace = getParameterValue(request, PAR_WORKSPACE, defaultWorkspace);
        final String startPath = getParameterValue(request, PAR_START_PATH, defaultStartPath);
        final String exportType = getParameterValue(request, PAR_EXPORT_TYPE, "system");
        ImportBehaviour behaviour = ImportBehaviour.Replace_Existing;
        try {
        		behaviour = ImportBehaviour.valueOf(getParameterValue(request, PAR_IMPORT_BEHAVIOUR, ImportBehaviour.Replace_Existing.name()));
        } catch (Exception e) {
		}

        String msg = null;

        if ("Export".equals(request.getParameter(ATTR_SUBMIT))) {
        	try {
        		msg = "Export finsihed successfully: "+JcrtImportExport.exportRepository(repository, workspace, backupPath, startPath, "system".equals(exportType));
        	} catch (Exception e) {
        		msg = e.getMessage();
        	}
        } else if ("Import".equals(request.getParameter(ATTR_SUBMIT))) {
        	try {
        		msg = "Import finsihed successfully: "+JcrtImportExport.importRepository(repository, workspace, backupPath, startPath, behaviour.num);
        	} catch (Exception e) {
        		msg = e.getMessage();
        	}
        }
    	
        // finally redirect
        final String path = request.getContextPath() + request.getServletPath() + request.getPathInfo();
        final String redirectTo;
        if ( msg == null ) {
            redirectTo = path;
        } else {
            redirectTo = path + '?' + PAR_MSG + '=' + encodeParam(msg);
        }
        response.sendRedirect(redirectTo);
    }

    private String encodeParam(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen
            return value;
        }
    }

    private void titleHtml(PrintWriter pw, String title, String description) {
        pw.println("<tr class='content'>");
        pw.println("<th colspan='3'class='content container'>" + title
            + "</th>");
        pw.println("</tr>");

        if (description != null) {
            pw.println("<tr class='content'>");
            pw.println("<td colspan='3'class='content'>" + description
                + "</th>");
            pw.println("</tr>");
        }
    }

    private void separatorHtml(PrintWriter pw) {
        pw.println("<tr class='content'>");
        pw.println("<td class='content' colspan='3'>&nbsp;</td>");
        pw.println("</tr>");
    }

    private void separatorText(PrintWriter pw) {
        pw.println();
    }


}
