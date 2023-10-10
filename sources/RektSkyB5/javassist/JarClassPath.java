/*
 * Decompiled with CFR 0.152.
 */
package javassist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;

final class JarClassPath
implements ClassPath {
    List<String> jarfileEntries;
    String jarfileURL;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    JarClassPath(String pathname) throws NotFoundException {
        JarFile jarfile = null;
        try {
            jarfile = new JarFile(pathname);
            this.jarfileEntries = new ArrayList<String>();
            for (JarEntry je : Collections.list(jarfile.entries())) {
                if (!je.getName().endsWith(".class")) continue;
                this.jarfileEntries.add(je.getName());
            }
            this.jarfileURL = new File(pathname).getCanonicalFile().toURI().toURL().toString();
            return;
        }
        catch (IOException iOException) {
        }
        finally {
            if (null != jarfile) {
                try {
                    jarfile.close();
                }
                catch (IOException iOException) {}
            }
        }
        throw new NotFoundException(pathname);
    }

    @Override
    public InputStream openClassfile(String classname) throws NotFoundException {
        URL jarURL = this.find(classname);
        if (null != jarURL) {
            try {
                if (ClassPool.cacheOpenedJarFile) {
                    return jarURL.openConnection().getInputStream();
                }
                URLConnection con = jarURL.openConnection();
                con.setUseCaches(false);
                return con.getInputStream();
            }
            catch (IOException e2) {
                throw new NotFoundException("broken jar file?: " + classname);
            }
        }
        return null;
    }

    @Override
    public URL find(String classname) {
        String jarname = classname.replace('.', '/') + ".class";
        if (this.jarfileEntries.contains(jarname)) {
            try {
                return new URL(String.format("jar:%s!/%s", this.jarfileURL, jarname));
            }
            catch (MalformedURLException malformedURLException) {
                // empty catch block
            }
        }
        return null;
    }

    public String toString() {
        return this.jarfileURL == null ? "<null>" : this.jarfileURL.toString();
    }
}

