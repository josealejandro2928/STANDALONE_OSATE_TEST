package org.osate.standalone.emf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.impl.SystemImplementationImpl;
import org.osate.aadl2.instance.InstancePackage;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.util.Aadl2ResourceFactoryImpl;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;

import com.google.inject.Injector;

// Takes the name of a declarative file, and the name of a SystemImplementation
public final class CreateInstanceModel {
	private static final int URI_AADL_FILE = 0;
	private static final int URI_XMI_PATH = 1;
	private static final int NUM_ARGS = 2;
    private static int id = -1; // OPTIONAL ID

	public static void printModel(Resource[] resources) {
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

    public static String saveModelToXMI(SystemInstance systemInstance, XtextResourceSet rs,
            String path, String parentName)
			throws Exception {
        String instanceName = path;
        if (id > -1) {
            instanceName += id + "_";
        }
        if (parentName != null) {
            instanceName += parentName + "_";
        }
        instanceName += systemInstance.getName() + ".aaxl2";
		Resource xmiResource = rs.createResource(URI.createURI(instanceName));
		xmiResource.getContents().add(systemInstance);
		xmiResource.save(null);
		System.out.println("SUCCESS: Saved SystemInstance: " + instanceName);
        return instanceName;
	}

	public static List<Issue> validateModel(Resource[] resources, OutputSchema out) {
		List<Issue> issues = new ArrayList<Issue>();
		for (final Resource resource : resources) {
			// Validation
            IResourceValidator validator = ((XtextResource) resource).getResourceServiceProvider()
                    .getResourceValidator();
//            CustomValidation validator = new CustomValidation();
            try {
                issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
            } catch (Exception e) {
                System.err.println("****************************** " + e);
            }

            for (int i = 0; i < issues.size() && i < 10; i++) {
                Issue issue = issues.get(i);
                out.errors.add(issue.getMessage());
			}
		}
		return issues;
	}

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
		OutputSchema output = new OutputSchema();
        List<String> pathToModelsFiles = new ArrayList<>();
        String parentDirectoryName = "";
		try {
			if (args.length < NUM_ARGS) {
				throw new Exception("Need <URI AADL> and <URI XMI FILE>");
			}
            if (args.length > NUM_ARGS) {
                id = Integer.parseInt(args[2]);
            }
			System.out.println("\n*************************************************************");
			File fileAddl = new File(args[URI_AADL_FILE]);
			File fileXML = new File(args[URI_XMI_PATH]);

			if (!fileAddl.exists()) {
				throw new Exception("The addl file: " + args[URI_AADL_FILE] + " does not exits");
			}
			if (!fileXML.exists()) {
				throw new Exception("The file for storing the XMI files: " + args[URI_XMI_PATH] + " does not exits");
			}
			output.pathAADLFile = args[URI_AADL_FILE];
			output.pathXMLFile = args[URI_XMI_PATH];

            Map<String, Object> crossReferenceResolverOut = CrossReferenceResolver.resolve(output.pathAADLFile, null);
            pathToModelsFiles = (List<String>) crossReferenceResolverOut.get("foundFiles");
            parentDirectoryName = (String) crossReferenceResolverOut.get("parentName");

			// Init the AADL2 meta model injector
			final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("aaxl2", new Aadl2ResourceFactoryImpl());
			InstancePackage.eINSTANCE.eClass();
			final XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
            final Resource[] resources = new Resource[pathToModelsFiles.size()];
            for (int i = 0; i < pathToModelsFiles.size(); i++) {
                resources[i] = rs.getResource(URI.createURI(pathToModelsFiles.get(i)), true);
                resources[i].load(null);
            }
            ///// Loading the resources =, validating the model at index 0
            //// and instantiate it with all the possibles cross-references
			try {
                for (final Resource resource : resources) {
                    resource.load(null);
                }
                final Resource rsrc = resources[0];
                EcoreUtil.resolveAll(rsrc);
				//////////////// VALIDATE THE MODEL ///////////////////////////
                validateModel(new Resource[] { rsrc }, output);
				//////////////////////////////////////////////////////////////
                List<EObject> contents = rsrc.getContents();
                if (contents.size() == 0) {
                    throw new Exception("This model cannot be loaded, it must be corrupted");
                }
                final AadlPackage aadlPackage = (AadlPackage) contents.get(0);


				System.out.println("AADL_PACKAGE_NAME: " + aadlPackage);
				System.out.println("URI_AADL_FILE: " + args[URI_AADL_FILE]);
				System.out.println("URI_XMI_PATH: " + args[URI_XMI_PATH]);
				output.modelName = aadlPackage.getFullName();
				List<SystemImplementation> systemImplementations = new ArrayList<SystemImplementation>();
				System.out.println("Looking for only system implementation models...");
				for (final Classifier classifier : aadlPackage.getPublicSection().getOwnedClassifiers()) {
					if (classifier instanceof SystemImplementationImpl) {
						systemImplementations.add((SystemImplementation) classifier);
					}
				}

				try {
					for (SystemImplementation systemImpl : systemImplementations) {
						final SystemInstance systemInstance = InstantiateModel.instantiate(systemImpl);
                        output.pathXMLFile = saveModelToXMI(systemInstance, rs, args[URI_XMI_PATH],
                                parentDirectoryName);
					}
				} catch (final Exception e) {
					e.printStackTrace();
					throw new Exception("Error during instantiation " + e.getMessage());
				}
                System.out.println("OUTPUT:");
                System.out.print(output);

			} catch (final IOException e) {
				throw new Exception("ERROR LOADING DECLARATIVE FILE: " + e.getMessage());
			}

		}catch(Exception e) {
			System.err.println("************ERROR************: " + e.getMessage());
			output.errors.add(e.getMessage());
            output.isParsingSucceeded = false;
            System.out.println("OUTPUT:");
            System.out.print(output);
		}

	}
}

class OutputSchema {
	List<String> errors;
	String pathXMLFile;
	String pathAADLFile;
	String modelName;
    boolean isParsingSucceeded;

	OutputSchema() {
		this.errors = new ArrayList<String>();
		this.pathAADLFile = "";
		this.pathXMLFile = "";
		this.modelName = "";
        this.isParsingSucceeded = true;
	}

    public String filterCharacters(String src) {
        String result = src.replace("\"", "");
        result = result.replace("\'", "");
        return "\"" + result + "\"";
    }

	@Override
	public String toString() {
	    String src = "";

		try {
            this.errors = this.errors.stream().map(x -> filterCharacters(x)).collect(Collectors.toList());
            src = "modelName: " + this.modelName + "\n" +
                    "pathAADLFile: " + this.pathAADLFile + "\n" +
                    "pathXMLFile: " + this.pathXMLFile + "\n" +
                    "isParsingSucceeded: " + this.isParsingSucceeded + "\n" +
                    "errors: " + this.errors + "\n";
            return src;
		} catch (Exception e) {
			e.printStackTrace();
			return "Error";
		}
	}
}

class CancelValidation implements CancelIndicator {
    @Override
    public boolean isCanceled() {
        return true;
    }
}
