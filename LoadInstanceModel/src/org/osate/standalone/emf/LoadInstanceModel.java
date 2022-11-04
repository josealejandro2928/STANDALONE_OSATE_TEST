package org.osate.standalone.emf;

import java.io.IOException;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.osate.aadl2.instance.InstancePackage;
import org.osate.aadl2.util.Aadl2ResourceFactoryImpl;

public final class LoadInstanceModel {
	public static void main(String[] args) {
		// This doesn't care about annexes, so we don't have to init the extension registry

		// Init the instance model -- need both these lines
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("aaxl2", new Aadl2ResourceFactoryImpl());
		InstancePackage.eINSTANCE.eClass();

		final ResourceSet rs = new ResourceSetImpl();
		final Resource resource = rs.getResource(URI.createURI(args[0]), true);

		try {
			resource.load(null);
			final TreeIterator<EObject> treeIter = resource.getAllContents();
			while (treeIter.hasNext()) {
				System.out.println(treeIter.next());
			}
		} catch (final IOException e) {
			System.err.println("ERROR LOADING RESOURCE: " + e.getMessage());
		}
	}
}
