package domain.controllers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Observable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import domain.MethodTest;
import domain.TestSuite;

public class TestSuiteController extends Observable {

	private TestSuite testSuite;
	private String filename;
	private MethodTest methodUnderTest;

	public TestSuiteController() {
		//TODO: fmartins: get the right path and filename
		filename = "/Users/fmartins/Documents/eclipse-workspaces/projects/runtime-EclipseApplication/testePESTT/default.xml";
		loadTestSuite(filename);
	}
	
	public void setMethodUnderTest(String packageName, String className, String methodSignature) {
		methodUnderTest = testSuite.getMethodTest(packageName, className, methodSignature);
	}
	
	public MethodTest getMethodUnderTest() {
		return methodUnderTest;
	}

	private void loadTestSuite(String filename) {
		try {
			JAXBContext context = JAXBContext.newInstance(TestSuite.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			testSuite = (TestSuite) unmarshaller.unmarshal(new FileInputStream(filename));
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// in case file is new, just create an empty testSuite
			testSuite = new TestSuite();
		}				
	}
	
	public void flush () {
		try {
			JAXBContext context = JAXBContext.newInstance(TestSuite.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			Writer writer = new FileWriter(filename);
			marshaller.marshal(testSuite, writer);
			writer.close();
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
