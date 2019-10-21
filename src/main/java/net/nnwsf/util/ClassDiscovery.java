package net.nnwsf.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClassDiscovery {

    private static Logger log = Logger.getLogger(ClassDiscovery.class.getName());

    private static ClassDiscovery instance = new ClassDiscovery();

    public static ClassDiscovery getInstance() {
        return instance;
    }

	public Collection<Class<?>> getClassesForPackage(Package pkg, ClassLoader classLoader) {
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		
		//Get name of package and turn it to a relative path
		String pkgname = pkg.getName();
		String relPath = pkgname.replace('.', '/');
	
		// Get a File object for the package
		URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);
		
		//If we can't find the resource we throw an exception
		if (resource == null) {
			throw new RuntimeException("Unexpected problem: No resource for " + relPath);
		}

		//If the resource is a jar get all classes from jar
		if(resource.toString().startsWith("jar:")) {
			classes.addAll(processJarfile(resource, pkgname, classLoader));
		} else {
			classes.addAll(processDirectory(new File(resource.getPath()), pkgname, classLoader));
		}

		return classes;
    }
    
	private Collection<Class<?>> processJarfile(URL resource, String pkgname, ClassLoader classLoader) {
		Collection<Class<?>> classes = new ArrayList<Class<?>>();
		
		//Turn package name to relative path to jar file
		String relPath = pkgname.replace('.', '/');
		String resPath = resource.getPath();
		String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
		log.log(Level.INFO, "Reading JAR file: '" + jarPath + "'");
		
		try(JarFile jarFile = new JarFile(jarPath)) {
			
			classes = jarFile.stream()
				.map(entry -> entry.getName())
				.filter(entryName -> entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length()))
				.map(entryName -> {
					String className = null;
					className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
					log.log(Level.INFO, "JarEntry '" + entryName + "'  =>  class '" + className + "'");
					return loadClass(classLoader, className);

				})
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
		}
		return classes;
    }
    
    private Class<?> loadClass(ClassLoader classLoader, String className) {
		try {
			return classLoader.loadClass(className);
		} 
		catch (ClassNotFoundException e) {
			throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
		}
    }
    
	private Collection<Class<?>> processDirectory(File directory, String pkgname, ClassLoader classLoader) {
		
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		 
		String[] files = directory.list();
		for (int i = 0; i < files.length; i++) {
			String fileName = files[i];
			String className = null;
			
			if (fileName.endsWith(".class")) {
				className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
			}
			
			if (className != null) {
				log.log(Level.INFO, "FileName '" + fileName + "'  =>  class '" + className + "'");
				classes.add(loadClass(classLoader, className));
			}
			
			//If the file is a directory recursively class this method.
			File subdir = new File(directory, fileName);
			if (subdir.isDirectory()) {
				classes.addAll(processDirectory(subdir, pkgname + '.' + fileName, classLoader));
			}
		}
		return classes;
	}
}