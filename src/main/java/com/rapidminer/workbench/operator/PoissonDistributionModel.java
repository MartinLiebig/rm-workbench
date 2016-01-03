package com.rapidminer.workbench.operator;

import java.util.logging.Level;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.example.set.ExampleSetUtilities.SetsCompareOption;
import com.rapidminer.example.set.ExampleSetUtilities.TypesCompareOption;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.LogService;

/**
 * This is the model class for poisson naive bayes.
 * 
 * @author Martin Schmitz
 */

public class PoissonDistributionModel extends PredictionModel {
	
	/* Number of classes for the label */
	private int numberOfClasses = 0;
	/* Number of attributes (all numerical) */ 
	private int numberOfAttributes = 0;
	/* Properties for the distribution.
	 * 1. attribute-index
	 * 2. class-index
	 * 3. mean or std (std is not yet used, but might be used later
	 */
	private double distributionProperties[][][]; // attribute, classes, mean/std
	/* Sum of weights. This feature is not yet supported */
	private double sumOfWeights[][]; 
	/* Store the mapping of label-classes */
	private String classValues[];
	/* Priors of each class. #Classes/#Total */
	private double classPriors[];
	/* Number of Training examples */
	private double trainExamples;
	/* PDs from apache. used to calc. the propability for a given x */
	private PoissonDistribution PD[][];
	
	private NormalDistribution gaus[][];
	
	public PoissonDistributionModel(ExampleSet trainingExampleSet, SetsCompareOption sizeCompareOperator,
			TypesCompareOption typeCompareOperator) {
		super(trainingExampleSet, sizeCompareOperator, typeCompareOperator);
		// TODO Auto-generated constructor stub
	}
	
	public PoissonDistributionModel(ExampleSet trainExampleSet) {
		super(trainExampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		// Set constants correctly
		
		Attribute labelAttribute = trainExampleSet.getAttributes().getLabel();
		this.numberOfClasses = labelAttribute.getMapping().size();
		this.numberOfAttributes = trainExampleSet.getAttributes().size();
		this.distributionProperties = new double[this.numberOfAttributes][this.numberOfClasses][2];
		this.sumOfWeights = new double[this.numberOfAttributes][this.numberOfClasses];
		this.classPriors = new double[this.numberOfClasses];
		this.trainExamples = (double) trainExampleSet.size();
		
		this.classValues = new String[numberOfClasses];
		for (int i = 0; i < numberOfClasses; i++) {
			classValues[i] = labelAttribute.getMapping().mapIndex(i);
			classPriors[i] = 0;
		}
		

		
		int attributeIndex = 0;
		for (Example example : trainExampleSet) {
			int classIndex = (int) example.getLabel();
			classPriors[classIndex]+=1; // will be normalized later
			// count sum of values and squared sum of values
			for (Attribute attribute : trainExampleSet.getAttributes()) {
				double attributeValue = example.getValue(attribute);
				distributionProperties[attributeIndex][classIndex][0] += attributeValue; 
				distributionProperties[attributeIndex][classIndex][1] += Math.pow(attributeValue, 2);
				sumOfWeights[attributeIndex][classIndex] += 1;
				attributeIndex++;
			}
			attributeIndex = 0;
		}
		
		for (Attribute attribute : trainExampleSet.getAttributes()){
			// Convert sum of values and sq sum of values to mean and std
			for(int i = 0; i < numberOfClasses; i++){
				// mean
				distributionProperties[attributeIndex][i][0] = 
							distributionProperties[attributeIndex][i][0]/sumOfWeights[attributeIndex][i];
				// std
				distributionProperties[attributeIndex][i][1] = Math.sqrt(
						distributionProperties[attributeIndex][i][1]/sumOfWeights[attributeIndex][i] 
						- Math.pow(distributionProperties[attributeIndex][i][0], 2)
						);
			}
		}
		// Calculate classPriors as #count of class / total size
		for (int i = 0; i < numberOfClasses; i++) {
			classPriors[i] = classPriors[i]/trainExampleSet.size();;
		}

	}
	
	
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		double currentValue = 0;
		double[] likelihood = new double[this.numberOfClasses];
		PD = new PoissonDistribution[numberOfAttributes][numberOfClasses];
		this.gaus = new NormalDistribution[numberOfAttributes][numberOfClasses];
		
		for(int att = 0; att < numberOfAttributes; att++){
			for(int c = 0; c < numberOfClasses; c++){
				double mean = distributionProperties[att][c][0];
				PD[att][c] = new PoissonDistribution(mean);
				this.gaus[att][c] = 
						new NormalDistribution(mean, Math.sqrt(mean));
			}
		}
		
		for(Example example : exampleSet){
			for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
				int attributeIndex = 0;
				likelihood[classIndex] = 0;
				for(Attribute attribute : exampleSet.getAttributes()){
					currentValue = example.getValue(attribute);
					double prop;
					if(currentValue < 500){
						prop = Math.log(PD[attributeIndex][classIndex].probability((int) currentValue));
					}
					else{
						prop = Math.log(gaus[attributeIndex][classIndex].density(currentValue));
					}
					if(Double.isFinite(prop)){
						likelihood[classIndex] += prop;
					}
					attributeIndex++;
				}
			}
			
			double[] confidences = new double[this.numberOfClasses];
			double sumOfConfidences = 0;
			int mostPropableClass = -1;
			double maxConfidence = -1;
			
			for(int i = 0; i < numberOfClasses;i++){
				
				confidences[i] = this.classPriors[i]*Math.exp(likelihood[i]);
				if(confidences[i] > maxConfidence){
					mostPropableClass = i;
					maxConfidence = confidences[i];
				}
				//confidence *= this.trainExamples;
				if(!Double.isNaN(confidences[i]) || !Double.isInfinite(confidences[i]))
					sumOfConfidences+=confidences[i];
			}
			example.setPredictedLabel(mostPropableClass);
			for(int i = 0; i < numberOfClasses;i++){		
				example.setConfidence(classValues[i],confidences[i]/sumOfConfidences);
			}
			
		
		}
	return exampleSet;
	}

	private double getPoissonPropability(long x, int attributeIndex, int classIndex){
		double lambda = distributionProperties[attributeIndex][classIndex][0];
		double prop = Math.pow(lambda,x)*Math.exp(-1*lambda);
		for(int i = 1; i <= x; i++){
			prop/=i;
		}
		if(!Double.isFinite(prop))
			LogService.getRoot().log(Level.INFO, Double.toString(x)+" "+Double.toString(lambda));
		return prop;
	}
	
	public static int factorial(int n) {
		
		int fact = 1; // this  will be the result
	    for (int i = 1; i <= n; i++) {
	        fact*=i;
	    }
	    return fact;
	}
	
	private double getGaussianPropability(long x, int attributeIndex, int classIndex){
		double mu = distributionProperties[attributeIndex][classIndex][0];
		double sigma = distributionProperties[attributeIndex][classIndex][1]; // this is most likely cheated..
		
		double factor = Math.sqrt(2*Math.PI*sigma*sigma);
		double exppart = Math.exp(-1*Math.pow(x-mu,2)/(2*Math.pow(sigma,2)));
		
		return 1/factor * exppart;
	
	}
}
