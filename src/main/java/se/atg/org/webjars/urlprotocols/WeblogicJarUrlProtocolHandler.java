package se.atg.org.webjars.urlprotocols;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.webjars.CloseQuietly;
import org.webjars.urlprotocols.UrlProtocolHandler;

public class WeblogicJarUrlProtocolHandler implements UrlProtocolHandler {

    @Override
    public boolean accepts(String protocol) {
        return "zip".equals(protocol);
    }

    @Override
    public Set<String> getAssetPaths(URL url, Pattern filterExpr, ClassLoader... classLoaders) {
        HashSet<String> assetPaths = new HashSet<String>();
        String[] segments = getSegments("file:" + url.getPath());
        JarFile jarFile = null;
        JarInputStream jarInputStream = null;

        try {
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                if (jarFile == null) {
                    File file = new File(URI.create(segment));
                    jarFile = new JarFile(file);
                    if (i == segments.length - 2) {
                        jarInputStream = new JarInputStream(new FileInputStream(file));
                    }
                } else {
                    jarInputStream = new JarInputStream(jarFile.getInputStream(jarFile.getEntry(segment)));
                }
            }

            JarEntry jarEntry = jarInputStream.getNextJarEntry();
            while (jarEntry !=null) {
                String assetPathCandidate = jarEntry.getName();
                if (!jarEntry.isDirectory() && filterExpr.matcher(assetPathCandidate).matches()) {
                    assetPaths.add(assetPathCandidate);
                }
                jarEntry = jarInputStream.getNextJarEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            CloseQuietly.closeQuietly(jarFile);
            CloseQuietly.closeQuietly(jarInputStream);
        }

        return assetPaths;
    }

    private String[] getSegments(String path) {
        String [] parts = path.split("!/");
        ArrayList<String> segments = new ArrayList<>(parts.length);
        StringBuilder outer = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; ++i) {
            if (segments.isEmpty()) {
                if (isArchive(outer.toString())) {
                    segments.add(outer.toString());
                    segments.add(parts[i]);
                } else {
                    outer.append("!/").append(parts[i]);
                }
            } else {
                segments.add(parts[i]);
            }
        }
        return segments.toArray(new String[segments.size()]);
    }

    private boolean isArchive(String path) {
        Path candidate = Paths.get(URI.create(path));
        return Files.isReadable(candidate) && Files.isRegularFile(candidate);
    }
}
