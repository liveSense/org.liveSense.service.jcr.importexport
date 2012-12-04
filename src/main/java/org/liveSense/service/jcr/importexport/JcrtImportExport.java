package org.liveSense.service.jcr.importexport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.liveSense.service.jcr.importexport.webconsole.JcrImportExportWebConsolePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(immediate=true, metatype=true)
@Properties(value={
		@Property(name=JcrtImportExport.PROP_BACKUP_DIR),
		@Property(name=JcrtImportExport.PROP_WORKSPACE),
		@Property(name=JcrtImportExport.PROP_START_PATH)
})
public class JcrtImportExport {
	
	public static final String PROP_BACKUP_DIR = "backuppath";
	public static final String DEFAULT_BACKUP_DIR = "/backup";

	public static final String PROP_WORKSPACE = "workspace";
	public static final String DEFAULT_WORKSPACE = "default";

	public static final String PROP_START_PATH = "startpath";
	public static final String DEFAULT_START_PATH = "/";

	
	private static final Logger log = LoggerFactory.getLogger(JcrtImportExport.class);

	@Reference(cardinality=ReferenceCardinality.MANDATORY_UNARY, policy=ReferencePolicy.DYNAMIC)
	ConfigurationAdmin configAdmin;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY_UNARY, policy=ReferencePolicy.DYNAMIC)
	SlingRepository repository;
	
	private String backupPath = DEFAULT_BACKUP_DIR;
	private String workspace = DEFAULT_WORKSPACE;
	private String startParth = DEFAULT_START_PATH;
	

	public static String exportRepository(SlingRepository repository, String workspace, String path, String startPath, boolean system) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:SS");
		
		if (workspace == null || "".equals(workspace))
			workspace = repository.getDefaultWorkspace();
		
		log.info("Backuping repository:"+workspace+" ...");
		Session session = null;
		try {
			session = repository.loginAdministrative(workspace);
		} catch (RepositoryException e) {
			log.error("Could not get session",e);
			throw new Exception("Could not get JCR Session",e);
		}
		if (startPath == null || "".equals(startPath))
			startPath = session.getRootNode().getPath();

		
		String d = sdf.format(new Date());
		String filepath = path+"/"+workspace+"-"+d+".xml.gz";
		File f = new File(filepath);
        if (f.exists()) {
            throw new IllegalArgumentException("Export file "+filepath+" is existing, can not export");
        }
        try {
            FileOutputStream os = new FileOutputStream(f);
            GZIPOutputStream out = new 
          		  GZIPOutputStream(new BufferedOutputStream(os));
            
            //export all including binary, recursive
            if (system) {
            	session.exportSystemView(startPath, out, false, false);
            } else {
            	session.exportDocumentView(startPath, out, false, false);            	
            }
            out.flush();
            out.close();
            os.close();
    		log.info("Backuping repository: "+workspace+"... OK");

        } catch(Throwable t) {
        	log.error("Failed to export repository at " +
                "/" + " to file "+filepath+"\n", t);
			throw new Exception("Failed to export repository at " +
                "/" + " to file "+filepath+"\n",t);
        }
		session.logout();
		return filepath;
	}

	
	public static String importRepository(SlingRepository repository, String workspace, String path, String startPath, int behaviour) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:SS");
		
		if (workspace == null || "".equals(workspace))
			workspace = repository.getDefaultWorkspace();
		
		log.info("Restoring repository:"+workspace+" ...");
		Session session = null;
		try {
			session = repository.loginAdministrative(workspace);
		} catch (RepositoryException e) {
			log.error("Could not get session",e);
			throw new Exception("Could not get JCR Session",e);
		}
		if (startPath == null || "".equals(startPath))
			startPath = session.getRootNode().getPath();
		
		String d = sdf.format(new Date());
		String filepath = path;
		File f = new File(filepath);
        if (!f.exists()) {
            throw new IllegalArgumentException("Import file "+filepath+" is existing, can not import");
        }
        try {
        	FileInputStream fis = new FileInputStream(f);
            GZIPInputStream gs = new GZIPInputStream(fis);
        	
            /*
            //import all
            if ("security".equals(workspace)) {
            	final UserManager userManager = AccessControlUtil.getUserManager(session);
            	final PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
            	UserImporter importer = new UserImporter();
            	JackrabbitImporterSession jsession = new JackrabbitImporterSession(session, userManager, principalManager);
            	importer.init(jsession, 
            			new DefaultNamePathResolver(session), 
            			false, 
            			ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING, 
            			new ReferenceChangeTracker());
            	//ImportHandler ih = new ImportHandler(importer, );
                //new ParsingContentHandler(ih).parse(in);
            } else {
                session.importXML(startPath, gs, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING );
            }
			*/

            // For security workspace we create UserImporter
            /*
            if ("security".equals(workspace)) {
	            final UserManager userManager = AccessControlUtil.getUserManager(session);
	        	final PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
	        	UserImporter importer = new UserImporter();
	        	JackrabbitImporterSession jsession = new JackrabbitImporterSession(session, userManager, principalManager);
	        	importer.init(jsession, 
	        			new DefaultNamePathResolver(session), 
	        			false, 
	        			ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING, 
	        			new ReferenceChangeTracker());
            }
			*/
            session.importXML(startPath, gs, behaviour);
           
            session.save();
            gs.close();
            fis.close();
    		log.info("Restoring repository: "+workspace+"... OK");

        } catch(Throwable t) {
        	log.error("Failed to import repository at " +
                "/" + " from file "+filepath+"\n", t);
			throw new Exception("Failed to import repository at " +
                "/" + " from file "+filepath+"\n",t);
        }
		session.logout();
		return filepath;
	}

	
	JcrImportExportWebConsolePlugin plugin = null;
	
	@Activate
	protected void activate(BundleContext context) {

		backupPath = PropertiesUtil.toString(context.getProperty(PROP_BACKUP_DIR), context.getProperty("sling.home"));
		workspace = PropertiesUtil.toString(context.getProperty(PROP_WORKSPACE), context.getProperty(JcrtImportExport.DEFAULT_WORKSPACE));
		startParth = PropertiesUtil.toString(context.getProperty(PROP_START_PATH), context.getProperty(JcrtImportExport.DEFAULT_START_PATH));

		plugin = new JcrImportExportWebConsolePlugin(context, repository, backupPath, workspace, startParth);
	}

	@Deactivate
	protected void deactivate(BundleContext context) {

	    if (plugin != null) {
	        plugin.dispose();
	        plugin = null;
	    }
	}

}
