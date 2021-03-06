package org.wtf.skybar.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Use Jetty ResourceHandler to search a path-style set of directories for a resource.
 */
public class SourceLister extends ResourceHandler {
    private static final Logger LOG = Log.getLogger(SourceLister.class);
    private final List<Resource> searchPaths;

    /**
     * String with "path.separator" delimiters to search for source files.
     *
     * @param searchPaths delimited string.
     * @throws java.io.IOException if given an invalid path/directory.
     */
    public SourceLister(String searchPaths) throws IOException {
        String[] split = searchPaths.split(System.getProperty("path.separator"));
        this.searchPaths = new ArrayList<>(split.length);
        for (String str: split) {
            File dir = new File(str).getCanonicalFile();
            if (!dir.isDirectory()) {
                throw new IOException("Invalid search path, not a directory: "+
                        dir.getAbsolutePath());
            }
            this.searchPaths.add(Resource.newResource(dir));
            LOG.info("Skybar source path added: "+dir.getAbsolutePath());
        }
        assert(this.searchPaths.size() > 0);
    }

    /**
     * Iterate through all our search paths to try to find the requested resource. 
     * @param path URI path starting with "/".
     * @return found Resource or null if not found.
     * @throws MalformedURLException if invalid URL passed in.
     */
    @Override
    public Resource getResource(String path) throws MalformedURLException {
        Resource result = null;
        for (Resource base: this.searchPaths) {
            this.setBaseResource(base);
            result = super.getResource(path);
            if (result != null && result.exists()) {
                LOG.debug("Skybar source "+path+" found: "+result);
                break;
            }
        }
        if (result == null || !result.exists()) {
            LOG.warn("Skybar source NOT found: "+path);
        }
        return result;
    }
}
