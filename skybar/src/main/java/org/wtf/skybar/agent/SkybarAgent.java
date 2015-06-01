package org.wtf.skybar.agent;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wtf.skybar.registry.SkybarRegistry;
import org.wtf.skybar.transform.SkybarTransformer;
import org.wtf.skybar.web.WebServer;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class SkybarAgent {

    private static final Logger logger = LoggerFactory.getLogger(SkybarAgent.class);

    public static void premain(String options, Instrumentation instrumentation) throws Exception {

        SkybarConfig config = getSkybarConfig();

        Pattern classNameRegex = config.getClassNameRegex();
        if (classNameRegex == null) {
            System.err.println("skybar.instrumentation.classRegex property not defined.");
            System.exit(-1);
        }

        instrumentation.addTransformer(new SkybarTransformer(classNameRegex), false);

        final String sourceGitUrl = config.getSourceGitUrl();
        if (sourceGitUrl != null) cloneOrPullSource(sourceGitUrl);

        int port = config.getWebUiPort();
        new WebServer(SkybarRegistry.registry, port, getSourcePathString(config)).start();
        logger.info("Skybar started on port " + port + " against classes matching " + classNameRegex);
    }

    private static SkybarConfig getSkybarConfig() throws IOException {
        String configFile = System.getProperty("skybar.config");
        if (configFile == null) {
            configFile = System.getenv("SKYBAR_CONFIG");
        }
        Properties fileProps = new Properties();
        if (configFile != null) {
            try (InputStreamReader reader =
                     new InputStreamReader(new FileInputStream(new File(configFile)), StandardCharsets.UTF_8)) {
                fileProps.load(reader);
            }
        } else {
            final File defaultConfigFile = new File("skybar.properties");
            if (defaultConfigFile.exists()) {
                try (InputStreamReader reader =
                         new InputStreamReader(new FileInputStream(defaultConfigFile), StandardCharsets.UTF_8)) {
                    fileProps.load(reader);
                }
            }
        }

        return new SkybarConfig(toMap(fileProps), toMap(System.getProperties()), System.getenv());
    }

    private static String getSourcePathString(SkybarConfig config) throws IOException {
        String sourceLookupPath = config.getSourceLookupPath();
        if (sourceLookupPath == null) {
            return new File("src/main/java").getCanonicalPath();
        }
        return sourceLookupPath;
    }

    private static void cloneOrPullSource(final String gitUrl) throws Exception {
        final File localRepo = new File("skybar/sources");
        final Repository repo = FileRepositoryBuilder.create(new File("skybar/sources", ".git"));

        try {
            System.out.println("Attempting Git pull.");
            new Git(repo).pull().call();
            // TODO what if the repository config has changed since it was cloned?
            System.out.println("Git pull succeeded.");
        } catch (NoHeadException e) {
            System.out.println("Git pull failed, attempting Git clone.");
            final Git cloneResult = Git.cloneRepository().
                setURI(gitUrl).
                setDirectory(localRepo).
                setCloneAllBranches(true).
                call();
            cloneResult.close();
            System.out.println("Git clone succeeded.");
        }

        final File[] versionFiles = new File(".").
            listFiles(
                new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().endsWith(".version");
                    }
                }
            );
        if (versionFiles.length > 0) {
            final String versionString =
                new String(
                    IO.readFully(versionFiles[0])
                ).
                trim();
            final String gitHash = versionString.substring(versionString.lastIndexOf('-') + 1);
            System.out.println("Attempting Git reset hard " + gitHash);
            new Git(repo).reset().setMode(ResetCommand.ResetType.HARD).setRef(gitHash).call();
        }
    }

    private static Map<String, String> toMap(Properties props) {
        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }

        return map;
    }
}
