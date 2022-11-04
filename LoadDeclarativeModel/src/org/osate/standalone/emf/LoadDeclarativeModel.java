package org.osate.standalone.emf;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.osate.aadl2.AadlPackage;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;

import com.google.inject.Injector;

public final class LoadDeclarativeModel {
	public static void main(String[] args) {
		// This doesn't care about annexes, so we don't have to init the extension registry

		// Init the XText/EMF meta model
		final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();

		// Create a resource set and populate from the command line
		final XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
		final Resource[] resources = new Resource[args.length];
		for (int i = 0; i < args.length; i++) {
			resources[i] = rs.getResource(URI.createURI(args[i]), true);
		}

		// Load the resources
		System.out.println("Loading...");
		for (final Resource resource : resources) {
			try {
				resource.load(null);
                AadlPackage aadlPackage = (AadlPackage) resource.getContents().get(0);
                System.out.println("1-Loaded Model: " + aadlPackage);
			} catch (final IOException e) {
				System.err.println("ERROR LOADING RESOURCE: " + e.getMessage());
			}
		}

		// Validate the model objects
		System.out.println();
		System.out.println("Validating...");
		for (final Resource resource : resources) {
			// Validation
            AadlPackage aadlPackage = (AadlPackage) resource.getContents().get(0);
            System.out.println("2-Validating Model: " + aadlPackage);
			IResourceValidator validator = ((XtextResource) resource).getResourceServiceProvider()
					.getResourceValidator();
			List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
			for (Issue issue : issues) {
				System.out.println(issue.getMessage());
			}
		}

		// Print the model objects
		System.out.println();
		System.out.println("Traversing...");
		for (final Resource resource : resources) {
			System.out.println("*** " + resource.getURI().toString() + " ***");
			final TreeIterator<EObject> treeIter = resource.getAllContents();
			while (treeIter.hasNext()) {
				System.out.println(treeIter.next());
			}
			System.out.println("-- -- -- -- -- -- -- --");
		}
	}
}
