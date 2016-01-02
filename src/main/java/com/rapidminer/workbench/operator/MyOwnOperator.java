package com.rapidminer.workbench.operator;

import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


public class MyOwnOperator extends Operator{

	public MyOwnOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(
			    new ExampleSetPassThroughRule( exampleSetInput, exampleSetOutput, SetRelation.EQUAL){
			        @Override
			        public ExampleSetMetaData modifyExampleSet( 
			            ExampleSetMetaData metaData ) throws UndefinedParameterError {
			                metaData.addAttribute(
			                    new AttributeMetaData("newAttribute", Ontology.REAL));
			                return metaData;
			        }
			});
	}
	
	@Override
	public void doWork() throws OperatorException{
		String text = getParameterAsString(PARAMETER_TEXT);
		LogService.getRoot().log(Level.INFO, text);
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		
	
		// get attributes from example set
		Attributes attributes = exampleSet.getAttributes();
		// create a new attribute
		String newName = "newAttribute";
		// define the name and the type of the new attribute
		// valid types are 
		// - nominal (sub types: binominal, polynominal, string, file_path)
		// - date_time (sub types: date, time)
		// - numerical (sub types: integer, real)
		Attribute targetAttribute = AttributeFactory.createAttribute(newName, Ontology.REAL);
		targetAttribute.setTableIndex(attributes.size());
		attributes.addRegular(targetAttribute);
		for(Example example:exampleSet){
		    example.setValue(targetAttribute, Math.round(Math.random()*10+0.5));
		}
		exampleSetOutput.deliver(exampleSet);
	}
	
	@Override
	public List<ParameterType> getParameterTypes(){
	    List<ParameterType> types = super.getParameterTypes();

	    types.add(new ParameterTypeString(
	        PARAMETER_TEXT,
	        "This parameter defines which text is logged to the console when this operator is executed.",
	        "This is a default text",
	        false));
	    return types;
	}
	
	public final static String PARAMETER_TEXT = "log text";
	
	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

}
