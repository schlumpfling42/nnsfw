package net.nnwsf.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClassDiscovery {

    private static Logger log = Logger.getLogger(ClassDiscovery.class.getName());

    private static ClassDiscovery instance;

    public static void init(final ClassLoader applicationClassLoader, final String rootPackage) {
		instance = new ClassDiscovery(applicationClassLoader, rootPackage);
	}

	private final ClassLoader applicationClassLoader;

	private final Collection<Class<?>> discoveredClasses;
	private final Map<Class<?>, Class<?>> implementations;

	private ClassDiscovery(final ClassLoader applicationClassLoader, final String rootPackage) {
		this.applicationClassLoader = applicationClassLoader;
		final Collection<Package> packagesToScan = Arrays.stream(Package.getPackages())
				.filter(p -> p.getName().startsWith(rootPackage)).collect(Collectors.toList());
		this.discoveredClasses = Collections.synchronizedCollection(getClassesForPackages(packagesToScan));
		this.implementations = Collections.synchronizedMap(new HashMap<>());
	}

	public static ClassDiscovery getInstance() {
		return instance;
	}

	private synchronized Collection<Class<?>> getClassesForPackages(final Collection<Package> packagesToScan) {
		final Collection<Class<?>> classes = new HashSet<>();
		for (final Package pkg : packagesToScan) {
			// Get name of package and turn it to a relative path
			final String pkgname = pkg.getName();
			final String relPath = pkgname.replace('.', '/');

			// Get a File object for the package
			final URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);

			// If we can't find the resource we throw an exception
			if (resource == null) {
				throw new RuntimeException("Unexpected problem: No resource for " + relPath);
			}

			// If the resource is a jar get all classes from jar
			if (resource.toString().startsWith("jar:")) {
				classes.addAll(processJarfile(resource, pkgname));
			} else {
				classes.addAll(processDirectory(new File(resource.getPath()), pkgname));
			}
		}
		return classes;
	}

	public <T> Collection<Class<T>> discoverAnnotatedClasses(final Class<T> type, final Class<?>... annotationClasses)
			throws Exception {
		final Collection<Class<T>> allAnnotatedClasses = new HashSet<>();
		for (final Class<?> aClass : discoveredClasses) {
			for (final Class<?> annotationClass : annotationClasses) {
				final Annotation[] classAnnotations = aClass.getAnnotations();
				for (final Annotation aClassAnnotation : classAnnotations) {
					if (aClassAnnotation.annotationType().isAssignableFrom(annotationClass)) {
						allAnnotatedClasses.add((Class<T>) aClass);
					}
				}
			}
		}
		return allAnnotatedClasses;
	}

	public <T> Class<T> getImplementation(final Class<T> aClass) {
		if (aClass.isInterface()) {
			Class<T> implementation = (Class<T>) implementations.get(aClass);
			if (implementation == null) {
				for (final Class<?> aDiscoveredClass : discoveredClasses) {
					if (!aDiscoveredClass.isInterface() && aClass.isAssignableFrom(aDiscoveredClass)) {
						implementation = (Class<T>) aDiscoveredClass;
						implementations.put(aClass, implementation);
					}
				}
			}
			return implementation;
		} else {
			return aClass;
		}
	}

	private Collection<Class<?>> processJarfile(final URL resource, final String pkgname) {
		Collection<Class<?>> classes = new ArrayList<Class<?>>();

		// Turn package name to relative path to jar file
		final String relPath = pkgname.replace('.', '/');
		final String resPath = resource.getPath();
		final String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
		log.log(Level.INFO, "Reading JAR file: '" + jarPath + "'");

		try (JarFile jarFile = new JarFile(jarPath)) {

			classes = jarFile
					.stream().map(entry -> entry.getName()).filter(entryName -> entryName.endsWith(".class")
							&& entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length()))
					.map(entryName -> {
						String className = null;
						className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
						log.log(Level.INFO, "JarEntry '" + entryName + "'  =>  class '" + className + "'");
						return loadClass(applicationClassLoader, className);

					}).collect(Collectors.toList());
		} catch (final IOException e) {
			throw new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
		}
		return classes;
	}

	private Class<?> loadClass(final ClassLoader classLoader, final String className) {
		try {
			return classLoader.loadClass(className);
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
		}
	}

	private Collection<Class<?>> processDirectory(final File directory, final String pkgname) {

		final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

		final String[] files = directory.list();
		for (int i = 0; i < files.length; i++) {
			final String fileName = files[i];
			String className = null;

			if (fileName.endsWith(".class")) {
				className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
			}

			if (className != null) {
				log.log(Level.INFO, "FileName '" + fileName + "'  =>  class '" + className + "'");
				classes.add(loadClass(applicationClassLoader, className));
			}

			// If the file is a directory recursively class this method.
			final File subdir = new File(directory, fileName);
			if (subdir.isDirectory()) {
				classes.addAll(processDirectory(subdir, pkgname + '.' + fileName));
			}
		}
		return classes;
	}
}