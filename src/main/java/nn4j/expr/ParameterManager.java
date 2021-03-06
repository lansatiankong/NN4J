package nn4j.expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.AdaDelta;
import org.nd4j.linalg.learning.AdaGrad;
import org.nd4j.linalg.learning.Adam;
import org.nd4j.linalg.learning.GradientUpdater;
import org.nd4j.linalg.learning.Nesterovs;
import org.nd4j.linalg.learning.NoOpUpdater;

import nn4j.expr.Parameter.RegType;
import nn4j.utils.NDArrayCache;


public class ParameterManager {
	
	private Updater updater;
	public ParameterManager(Updater updater){
		this.updater=updater;
	}

	private List<Parameter> parameters=new ArrayList<Parameter>();
	private Map<Parameter,GradientUpdater> updaters=new HashMap<Parameter, GradientUpdater>();
	public Parameter createParameter(INDArray value,RegType regType,float lambdaReg,boolean updatable){
		Parameter p=new Parameter(value, regType, lambdaReg,updatable);
		parameters.add(p);
		if(updatable)
		{
			updaters.put(p, updater(updater));
		}
		return p;
	}
	
	public void update(int iteration) throws Exception{
		if(iteration<=0)throw new Exception("must larger than 0");
		for (int i = 0; i < parameters.size(); i++) {
			Parameter param = parameters.get(i);
			if(!param.updatable())
				continue;
			GradientUpdater paramUpdater=updaters.get(param);

			if (iteration == 1)
			{
				paramUpdater.setStateViewArray(Nd4j.toFlattened('f', NDArrayCache.get(param.value().shape())), param.value().shape(), 'f', true);
			}

			for(INDArray gra : param.gradients())
			{
				if(param.regType().equals(RegType.L2)){
					gra.addi(param.value().mul(param.lambdaReg()));
				}
				
				gra = paramUpdater.getGradient(gra, iteration);
				param.value().subi(gra);
			}
		}
		reset();
	}
	
	public void reset(){
		for (int i = 0; i < parameters.size(); i++) {
			Parameter param = parameters.get(i);
			param.gradients().clear();
		}
	}
	
	public enum Updater {
	    SGD
	    ,ADAM
	    ,ADADELTA
	    ,NESTEROVS
	    ,ADAGRAD
	    ,RMSPROP
	    ,NONE
	    ,CUSTOM
	}
	
	private double momentum = 0.9;
	private double adamMeanDecay = 0.9;
	private double adamVarDecay = 0.999;
	private double rho = 0.95;
	private double epsilon = 1e-6;
	private double rmsDecay = 0.95;
	private double learningRate = 1e-3;
	
	public GradientUpdater updater(Updater u) {
		GradientUpdater updater;
		switch (u) {
		case SGD:
			updater = new org.nd4j.linalg.learning.Sgd(learningRate);
			break;
		case ADAM:
			updater = new Adam(learningRate, adamMeanDecay, adamVarDecay);
			break;
		case ADADELTA:
			updater = new AdaDelta(rho, epsilon);
			break;
		case NESTEROVS:
			updater = new Nesterovs(momentum, learningRate);
			break;
		case ADAGRAD:
			updater = new AdaGrad(learningRate, epsilon);
			break;
		case RMSPROP:
			updater = new org.nd4j.linalg.learning.RmsProp(learningRate, rmsDecay);
			break;
		case NONE:
			updater = new NoOpUpdater();
			break;
		case CUSTOM:
			throw new UnsupportedOperationException("Custom updaters: not yet implemented");
		default:
			throw new IllegalArgumentException("Unknown updater: " + u);
		}
		return updater;
	}
}
