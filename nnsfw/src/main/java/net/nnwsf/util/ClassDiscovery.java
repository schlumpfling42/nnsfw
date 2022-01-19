package net.nnwsf.util;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassGraphException;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class ClassDiscovery {

    private static ClassDiscovery instance;

    public static void init(String rootPackage) {
		instance = new ClassDiscovery(rootPackage);
	}

	private final ScanResult scanResult;
	private final Map<Class<?>, Class<?>> implementations;

	private ClassDiscovery(String rootPackage) {
		try {
			scanResult =
				new ClassGraph()
					.enableAllInfo()
					.acceptPackages("net.nnwsf", rootPackage)
					.scan();
			this.implementations = Collections.synchronizedMap(new HashMap<>());
		} catch(ClassGraphException e) {
			throw new RuntimeException("Unable to scan class files", e);
		}
	}

	public static Map<Annotation, Class<?>> discoverAnnotatedClasses(Class<? extends Annotation>[] annotationClasses) throws Exception {
		Map<Annotation, Class<?>> allAnnotatedClasses = new HashMap<>();
		Arrays.stream(annotationClasses).forEach(annotationClass -> {
			instance.scanResult.getClassesWithAnnotation(annotationClass).stream().forEach(classInfo -> {
				allAnnotatedClasses.put(classInfo.getAnnotationInfo(annotationClass).loadClassAndInstantiate(), classInfo.loadClass());
			});
		});
		return allAnnotatedClasses;
	}

	public static Collection<Class<?>> discoverFieldAnnotatedClasses(Class<? extends Annotation>[] annotationClasses) throws Exception {
		Collection<Class<?>> allAnnotatedClasses = new HashSet<>();
		Arrays.stream(annotationClasses).forEach(annotationClass -> {
			instance.scanResult.getClassesWithFieldAnnotation(annotationClass).stream().forEach(classInfo -> {
				allAnnotatedClasses.add(classInfo.loadClass());
			});
		});
		return allAnnotatedClasses;
	}


	@SuppressWarnings("unchecked")
	public static <T, A extends Annotation> Map<A, Class<T>> discoverAnnotatedClasses(Class<T> type, Class<A> annotationClass) throws Exception {
		Map<A, Class<T>> allAnnotatedClasses = new IdentityHashMap<>();
		for (ClassInfo classInfo : instance.scanResult.getClassesWithAnnotation(annotationClass)) {
			if(classInfo.extendsSuperclass(type) || (type.isInterface() && classInfo.implementsInterface(type))) {
				AnnotationInfo annotationInfo = classInfo.getAnnotationInfo(annotationClass);
				allAnnotatedClasses.put((A)annotationInfo.loadClassAndInstantiate(), (Class<T>)classInfo.loadClass());
			}
		}
		return allAnnotatedClasses;
	}

	@SuppressWarnings("unchecked")
	public static <T, A extends Annotation> Map<A, Class<T>> discoverAnnotatedClasses(Class<A> annotationClass) throws Exception {
		Map<A, Class<T>> allAnnotatedClasses = new IdentityHashMap<>();
		for (ClassInfo classInfo : instance.scanResult.getClassesWithAnnotation(annotationClass)) {
			AnnotationInfo annotationInfo = classInfo.getAnnotationInfo(annotationClass);
			allAnnotatedClasses.put((A)annotationInfo.loadClassAndInstantiate(), (Class<T>)classInfo.loadClass());
		}
		return allAnnotatedClasses;
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getImplementation(Class<T> aClass, Class<? extends Annotation> annotationClass, String value) {
		if (aClass.isInterface()) {
			Class<T> implementation = (Class<T>) instance.implementations.get(aClass);
			if (implementation == null) {
				Optional<ClassInfo> firstImplementation = null;
				if(value != null && !"".equals(value)) {
					firstImplementation = instance.scanResult.getClassesImplementing(aClass).stream()
					.filter(classInfo -> {
						if(!classInfo.isInterface()) {
							AnnotationInfo annotationInfo = classInfo.getAnnotationInfo(annotationClass);
							if(annotationInfo != null 
								&& annotationInfo.getParameterValues().get("value") != null
								&& value.equals(annotationInfo.getParameterValues().get("value").getValue())) {
								return true;
							}
						}
						return false;
					}).findFirst();
				} else {
					firstImplementation = instance.scanResult.getClassesImplementing(aClass).stream()
					.filter(classInfo -> !classInfo.isInterface() && classInfo.getAnnotationInfo(annotationClass) == null).findFirst();
				}
				implementation = firstImplementation.map(classInfo -> {
					Class<T> foundImplemention = (Class<T>)classInfo.loadClass();
						instance.implementations.put(aClass, foundImplemention);
						return foundImplemention;
				}).orElse(null);
			}
			return implementation;
		} else {
			return aClass;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getImplementation(Class<T> aClass) {
		if (aClass.isInterface()) {
			Class<T> implementation = (Class<T>) instance.implementations.get(aClass);
			if (implementation == null) {
				Optional<ClassInfo> firstImplementation = instance.scanResult.getClassesImplementing(aClass).stream()
					.filter(classInfo -> !classInfo.isInterface()).findFirst();
				implementation = firstImplementation.map(classInfo -> {
					Class<T> foundImplemention = (Class<T>)classInfo.loadClass();
						instance.implementations.put(aClass, foundImplemention);
						return foundImplemention;
				}).orElse(null);
			}
			return implementation;
		} else {
			return aClass;
		}
	}

}