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

    public static void init(ClassLoader applicationClassLoader, String rootPackage) {
		instance = new ClassDiscovery(applicationClassLoader, rootPackage);
	}

	private final ClassLoader applicationClassLoader;

	private final Collection<Class<?>> discoveredClasses;
	private final Map<Class<?>, Class<?>> implementations;
	private final Map<Class<?>, Collection<Class<?>>> superClassesAndInferfaces;
	private final Map<Class<?>, Collection<Class<?>>> subClasses;
	private final Collection<Package> packagesToScan;

	private ClassDiscovery(ClassLoader applicationClassLoader, String rootPackage) {
		this.applicationClassLoader = applicationClassLoader;
		this.packagesToScan = Arrays.stream(Package.getPackages()).filter(p -> p.getName().startsWith(rootPackage)).collect(Collectors.toList());
		this.discoveredClasses = Collections.synchronizedCollection(getClassesForPackages(packagesToScan));
		this.implementations = Collections.synchronizedMap(new HashMap<>());
		this.superClassesAndInferfaces = Collections.synchronizedMap(new HashMap<>());
		subClasses = Collections.synchronizedMap(new HashMap<>());
		catalogAllSubAnSuperClasses();
	}

	private synchronized Collection<Class<?>> getClassesForPackages(Collection<Package> packagesToScan) {
		Collection<Class<?>> classes = new HashSet<>();
		for(Package pkg : packagesToScan) {
			// Get name of package and turn it to a relative path
			String pkgname = pkg.getName();
			String relPath = pkgname.replace('.', '/');

			// Get a File object for the package
			URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);

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

	@SuppressWarnings("unchecked")
	public static <T> Map<Annotation, Class<T>> discoverAnnotatedClasses(Class<?>... annotationClasses) throws Exception {
		Map<Annotation, Class<T>> allAnnotatedClasses = new HashMap<>();
		for (Class<?> aClass : instance.discoveredClasses) {
			for (Class<?> annotationClass : annotationClasses) {
				Annotation[] classAnnotations = aClass.getAnnotations();
				for (Annotation aClassAnnotation : classAnnotations) {
					if(aClassAnnotation.annotationType().isAssignableFrom(annotationClass)) {
						allAnnotatedClasses.put(aClassAnnotation, (Class<T>)aClass);
					}
				}
			}
		}
		return allAnnotatedClasses;
	}

	@SuppressWarnings("unchecked")
	public static <T, A> Map<A, Class<T>> discoverAnnotatedClasses(Class<T> type, Class<A> annotationClass) throws Exception {
		Map<A, Class<T>> allAnnotatedClasses = new IdentityHashMap<>();
		for (Class<?> aClass : instance.discoveredClasses) {
			Annotation[] classAnnotations = aClass.getAnnotations();
			for (Annotation aClassAnnotation : classAnnotations) {
					if (aClassAnnotation.annotationType().isAssignableFrom(annotationClass)) {
					allAnnotatedClasses.put(annotationClass.cast(aClassAnnotation), (Class<T>)aClass);
				}
			}
		}
		return allAnnotatedClasses;
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getImplementation(Class<T> aClass) {
		if (aClass.isInterface()) {
			Class<T> implementation = (Class<T>) instance.implementations.get(aClass);
			if (implementation == null) {
				for(Class<?> aDiscoveredClass : instance.discoveredClasses) {
					if (!aDiscoveredClass.isInterface() && aClass.isAssignableFrom(aDiscoveredClass)) {
						implementation = (Class<T>) aDiscoveredClass;
						instance.implementations.put(aClass, implementation);
					}
				}
			}
			return implementation;
		} else {
			return aClass;
		}
	}

	private void catalogAllSubAnSuperClasses() {
		for(Class<?> aClass : discoveredClasses) {
			getAllSuperClassesAndInterfaces(aClass);
		}
	}


	private synchronized Collection<Class<?>> getAllSuperClassesAndInterfaces(Class<?> aClass) {
		Collection<Class<?>> allSuperClassesAndInterfaces = superClassesAndInferfaces.get(aClass);
		if(allSuperClassesAndInterfaces == null) {
			allSuperClassesAndInterfaces = new HashSet<>();
			if(aClass.getSuperclass() != null && !Object.class.equals(aClass.getSuperclass())) {
				allSuperClassesAndInterfaces.add(aClass.getSuperclass());
			}
			allSuperClassesAndInterfaces.addAll(Arrays.asList(aClass.getInterfaces()));
			for (Class<?> superClasseOrInterface : allSuperClassesAndInterfaces) {
				allSuperClassesAndInterfaces.addAll(getAllSuperClassesAndInterfaces(superClasseOrInterface));
			}
			superClassesAndInferfaces.put(aClass, allSuperClassesAndInterfaces);
			if(!aClass.isInterface()) {
				for (Class<?> subClassOrInterface : allSuperClassesAndInterfaces) {
					Collection<Class<?>> allSubClasses = subClasses.get(subClassOrInterface);
					if(allSubClasses == null) {
						allSubClasses = new ArrayList<>();
						subClasses.put(subClassOrInterface, allSubClasses);
					}
					if(!allSubClasses.contains(aClass)) {
						allSubClasses.add(aClass);
					}
				}
			}
		}
		return  allSuperClassesAndInterfaces;
	}

	public synchronized Collection<Class<?>> findAllImplementations(Class<?> aClass) {
		return subClasses.get(aClass);
	}


	private Collection<Class<?>> processJarfile(URL resource, String pkgname) {
		log.log(Level.INFO, "Discovering classes for package {0} in resource {1}", new Object[] {pkgname, resource});
		Collection<Class<?>> classes = new ArrayList<Class<?>>();

		// Turn package name to relative path to jar file
		String relPath = pkgname.replace('.', '/');
		String resPath = resource.getPath();
		String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
		log.log(Level.INFO, "Reading JAR file: '" + jarPath + "'");

		try (JarFile jarFile = new JarFile(jarPath)) {

			classes = jarFile.stream()
				.map(entry -> entry.getName())
				.filter(entryName -> entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length()))
					.map(entryName -> {
						String className = null;
						className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
						log.log(Level.INFO, "JarEntry '" + entryName + "'  =>  class '" + className + "'");
						return loadClass(applicationClassLoader, className);

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
    
	private Collection<Class<?>> processDirectory(File directory, String pkgname) {
		log.log(Level.INFO, "Discovering classes for package {0} in directory {1}", new Object[] {pkgname, directory});
		ArrayList<Class<?>> classes = new ArrayList<>();

		ArrayList<File> subDirectories = new ArrayList<>();

		String[] files = directory.list();
		for (int i = 0; i < files.length; i++) {
			String fileName = files[i];
			String className = null;

			if (fileName.endsWith(".class")) {
				className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
			}

			if (className != null) {
				log.log(Level.INFO, "FileName '" + fileName + "'  =>  class '" + className + "'");
				classes.add(loadClass(applicationClassLoader, className));
			}

			// If the file is a directory recursively class this method.
			File subdir = new File(directory, fileName);
			if (subdir.isDirectory()) {
				subDirectories.add(subdir);
			}
		}
		for (File subdir : subDirectories) {
			classes.addAll(processDirectory(subdir, pkgname + '.' + subdir.getName()));
		}
		return classes;
	}

	public static Collection<Package> getPackagesToScan() {
		return instance.packagesToScan;
	}
}