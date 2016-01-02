package com.rapidminer.workbench.operator;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;

import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;

/**
 * A Poisson Learner using poisson distribution. For numerical values only.
 * 
 * @author Martin Schmitz
 */

public class PoissonNaiveBayes extends AbstractLearner {

	public PoissonNaiveBayes(OperatorDescription description) {
		super(description);
	}
	
	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		return new PoissonDistributionModel(exampleSet);
	}
	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		switch (lc) {

			case NUMERICAL_ATTRIBUTES:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
				return true;
			default:
				return false;
		}
	}

}
